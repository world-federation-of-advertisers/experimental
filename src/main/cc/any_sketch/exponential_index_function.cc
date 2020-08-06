// Copyright 2020 The Any Sketch Authors
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

#include <cmath>
#include <cstdint>
#include <limits>


#include "absl/base/macros.h"
#include "exponential_index_function.h"

namespace wfa::any_sketch {

ExponentialIndexFunction::ExponentialIndexFunction(
    double rate,
    uint64_t size)
    : rate_(rate),
      size_(size),
      rate_exponential_(std::exp(rate)){
        ABSL_ASSERT(rate > 0);
        ABSL_ASSERT(size > 0);
      }

uint64_t ExponentialIndexFunction::GetIndex(uint64_t fingerprint) const {
  double u = static_cast<double>(fingerprint) / static_cast<double>(hash_max_value());
  double x = 1 - std::log(rate_exponential_ + u * (1 - rate_exponential_)) / rate_;
  return static_cast<uint64_t>(std::floor(x * size_));
}

uint64_t ExponentialIndexFunction::hash_max_value() const {
  return std::numeric_limits<int64_t>::max();
}

uint64_t ExponentialIndexFunction::max_value() const {
  return size_ - 1;
}

}  // namespace wfa::any_sketch
