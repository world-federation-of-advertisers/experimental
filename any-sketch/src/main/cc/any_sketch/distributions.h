// Copyright 2020 The Cross-Media Measurement Authors
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

#ifndef SRC_MAIN_CC_ANY_SKETCH_DISTRIBUTIONS_H_
#define SRC_MAIN_CC_ANY_SKETCH_DISTRIBUTIONS_H_

#include <cstdint>
#include <memory>
#include <string>

#include "absl/container/flat_hash_map.h"
#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"
#include "src/main/cc/any_sketch/fingerprinters.h"

namespace wfa::any_sketch {

using ItemMetadata = absl::flat_hash_map<std::string, int64_t>;

// Base for representing distributions -- a way of deterministically mapping an
// item and associated metadata to a number.
class Distribution {
 public:
  Distribution(const Distribution&) = delete;
  Distribution& operator=(const Distribution&) = delete;

  virtual ~Distribution() = default;

  // The smallest value (inclusive) that the Distribution can return.
  virtual int64_t min_value() const = 0;

  // The largest value (inclusive) that the Distribution can return.
  virtual int64_t max_value() const = 0;

  // The size of the range of values the Distribution can return.
  int64_t size() const { return max_value() - min_value() + 1; }

  // Calculates the value of the distribution for an item and associated
  // metadata.
  virtual absl::StatusOr<int64_t> Apply(
      absl::string_view item, const ItemMetadata& item_metadata) const = 0;

 protected:
  Distribution() = default;
};

std::unique_ptr<Distribution> GetOracleDistribution(
    absl::string_view feature_name, int64_t min_value, int64_t max_value);
std::unique_ptr<Distribution> GetUniformDistribution(
    const Fingerprinter* fingerprinter, int64_t min_value, int64_t max_value);
std::unique_ptr<Distribution> GetExponentialDistribution(
    const Fingerprinter* fingerprinter, double rate, int64_t size);
std::unique_ptr<Distribution> GetGeometricDistribution(
    const Fingerprinter* fingerprinter, int64_t min_value, int64_t max_value);

}  // namespace wfa::any_sketch

#endif  // SRC_MAIN_CC_ANY_SKETCH_DISTRIBUTIONS_H_
