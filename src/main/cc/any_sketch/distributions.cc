// Copyright 2020 The AnySketch Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "src/main/cc/any_sketch/distributions.h"

#include <cmath>
#include <cstdint>
#include <limits>
#include <memory>

#include "absl/base/macros.h"
#include "absl/container/flat_hash_map.h"
#include "absl/memory/memory.h"
#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/strings/str_cat.h"
#include "absl/strings/string_view.h"
#include "src/main/cc/any_sketch/hash_function.h"
#include "src/main/cc/any_sketch/util/macros.h"

namespace wfa::any_sketch {
namespace {
class BaseDistribution : public Distribution {
 public:
  BaseDistribution(int64_t min_value, int64_t max_value)
      : min_value_(min_value), max_value_(max_value) {}

  absl::StatusOr<int64_t> Apply(
      absl::string_view item, const ItemMetadata& item_metadata) const override;

  int64_t min_value() const override { return min_value_; }

  // The largest value (inclusive) that the Distribution can return.
  int64_t max_value() const override { return max_value_; }

 private:
  int64_t min_value_;
  int64_t max_value_;

  virtual absl::StatusOr<int64_t> ApplyInternal(
      absl::string_view item, const ItemMetadata& item_metadata) const = 0;
};

absl::StatusOr<int64_t> BaseDistribution::Apply(
    absl::string_view item, const ItemMetadata& item_metadata) const {
  ASSIGN_OR_RETURN(int64_t value, ApplyInternal(item, item_metadata));

  if (value < min_value()) {
    return absl::InvalidArgumentError(absl::StrCat(
        "Returned value ", value, " is less than minimum value ", min_value()));
  }
  if (value > max_value()) {
    return absl::InvalidArgumentError(
        absl::StrCat("Returned value ", value,
                     " is greater than maximum value ", max_value()));
  }
  return value;
}

class OracleDistribution : public BaseDistribution {
 public:
  OracleDistribution(int64_t min_value, int64_t max_value,
                     absl::string_view feature_name)
      : BaseDistribution(min_value, max_value), feature_name_(feature_name) {}

 private:
  absl::StatusOr<int64_t> ApplyInternal(
      absl::string_view item,
      const ItemMetadata& item_metadata) const override {
    if (auto itr = item_metadata.find(feature_name_);
        itr != item_metadata.end()) {
      return itr->second;
    }
    return absl::InvalidArgumentError(absl::StrCat(
        "Could not find key ", feature_name_, " in item_metadata"));
  }

  std::string feature_name_;
};

class FingerprintingDistribution : public BaseDistribution {
 public:
  FingerprintingDistribution(int64_t min_value, int64_t max_value,
                             const HashFunction* fingerprinter)
      : BaseDistribution(min_value, max_value), fingerprinter_(fingerprinter) {}

 private:
  absl::StatusOr<int64_t> ApplyInternal(
      absl::string_view item,
      const ItemMetadata& item_metadata) const override {
    return ApplyToFingerprint(fingerprinter_->Fingerprint(item), item_metadata);
  }

  virtual absl::StatusOr<int64_t> ApplyToFingerprint(
      uint64_t fingerprint, const ItemMetadata& item_metadata) const = 0;

  const HashFunction* fingerprinter_;
};

class UniformDistribution : public FingerprintingDistribution {
 public:
  UniformDistribution(int64_t min_value, int64_t max_value,
                      const HashFunction* fingerprinter)
      : FingerprintingDistribution(min_value, max_value, fingerprinter) {}

 private:
  absl::StatusOr<int64_t> ApplyToFingerprint(
      uint64_t fingerprint, const ItemMetadata& item_metadata) const override {
    return fingerprint % size() + min_value();
  }
};

class ExponentialDistribution : public FingerprintingDistribution {
 public:
  ExponentialDistribution(double rate, int64_t size,
                          const HashFunction* fingerprinter)
      : FingerprintingDistribution(0, size - 1, fingerprinter),
        rate_(rate),
        exp_rate_(std::exp(rate)) {}

 private:
  double rate_;
  double exp_rate_;

  absl::StatusOr<int64_t> ApplyToFingerprint(
      uint64_t fingerprint, const ItemMetadata& item_metadata) const override {
    double u = static_cast<double>(fingerprint) /
               static_cast<double>(std::numeric_limits<uint64_t>::max());
    double x = 1 - std::log(exp_rate_ + u * (1 - exp_rate_)) / rate_;
    return static_cast<int64_t>(std::floor(x * size()));
  }
};

int CountTrailingZeros(uint64_t n) {
  if (n == 0) {
    return 64;
  }
  int c = 63;
  n &= ~n + 1;
  if (n & 0x00000000FFFFFFFF) c -= 32;
  if (n & 0x0000FFFF0000FFFF) c -= 16;
  if (n & 0x00FF00FF00FF00FF) c -= 8;
  if (n & 0x0F0F0F0F0F0F0F0F) c -= 4;
  if (n & 0x3333333333333333) c -= 2;
  if (n & 0x5555555555555555) c -= 1;
  return c;
}

class GeometricDistribution : public FingerprintingDistribution {
 public:
  GeometricDistribution(int64_t min_value, int64_t max_value,
                        const HashFunction* fingerprinter)
      : FingerprintingDistribution(min_value, max_value, fingerprinter) {}

 private:
  absl::StatusOr<int64_t> ApplyToFingerprint(
      uint64_t fingerprint, const ItemMetadata& item_metadata) const override {
    int trailing_zeroes = CountTrailingZeros(fingerprint);
    return std::min(max_value(), min_value() + trailing_zeroes);
  }
};
}  // namespace

std::unique_ptr<Distribution> GetOracleDistribution(
    absl::string_view feature_name, int64_t min_value, int64_t max_value) {
  return absl::make_unique<OracleDistribution>(min_value, max_value,
                                               feature_name);
}
std::unique_ptr<Distribution> GetUniformDistribution(
    const HashFunction* fingerprinter, int64_t min_value, int64_t max_value) {
  return absl::make_unique<UniformDistribution>(min_value, max_value,
                                                fingerprinter);
}
std::unique_ptr<Distribution> GetExponentialDistribution(
    const HashFunction* fingerprinter, double rate, int64_t size) {
  ABSL_ASSERT(rate > 0.0);
  ABSL_ASSERT(size > 0);
  return absl::make_unique<ExponentialDistribution>(rate, size, fingerprinter);
}
std::unique_ptr<Distribution> GetGeometricDistribution(
    const HashFunction* fingerprinter, int64_t min_value, int64_t max_value) {
  return absl::make_unique<GeometricDistribution>(min_value, max_value,
                                                  fingerprinter);
}
}  // namespace wfa::any_sketch
