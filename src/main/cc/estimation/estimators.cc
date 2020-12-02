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

#include "src/main/cc/estimation/estimators.h"

#include <cmath>
#include <cstdint>
#include <functional>

#include "absl/base/macros.h"
#include "absl/functional/bind_front.h"

namespace wfa::estimation {

namespace {
// Get the expected number of legionaries activated for given cardinality
uint64_t GetExpectedActiveRegisterCount(double decay_rate,
                                        uint64_t num_of_total_registers,
                                        uint64_t cardinality) {
  ABSL_ASSERT(decay_rate > 1.0);
  ABSL_ASSERT(num_of_total_registers > 0);
  if (cardinality == 0) return 0;
  double exponential_of_decay = std::exp(decay_rate);
  double t = cardinality / static_cast<double>(num_of_total_registers);
  double negative_term =
      -std::expint((-decay_rate * t) / (exponential_of_decay - 1));
  double positive_term = std::expint((-decay_rate * exponential_of_decay * t) /
                                     (exponential_of_decay - 1));
  return (1 - (negative_term + positive_term) / decay_rate) *
         num_of_total_registers;
}

// Calculate the invert of a monotonic increasing function using binary search
uint64_t InvertMonotonic(const std::function<uint64_t(uint64_t)>& function,
                         uint64_t target) {
  uint64_t f0 = function(0);
  ABSL_ASSERT(f0 <= target);
  uint64_t left = 0;
  uint64_t right = 1;
  // Find a region that contains the solution
  while (function(right) < target) {
    left = right;
    right *= 2;
  }
  uint64_t mid = (right + left) / 2;
  while (right > left) {
    uint64_t f_mid = function(mid);
    if (f_mid > target) {
      right = mid - 1;
    } else {
      left = mid + 1;
    }
    mid = (right + left) / 2;
  }
  return mid;
}

double GetCardinality(
    std::function<uint64_t(uint64_t)> inverse_cardinality_estimator,
    uint64_t active_register_count) {
  return InvertMonotonic(inverse_cardinality_estimator, active_register_count);
}

}  // namespace

int64_t EstimateCardinalityLiquidLegions(double decay_rate,
                                         uint64_t num_of_total_registers,
                                         uint64_t active_register_count) {
  std::function<uint64_t(uint64_t)> get_expected_active_register_count =
      absl::bind_front(GetExpectedActiveRegisterCount, decay_rate,
                       num_of_total_registers);
  ABSL_ASSERT(active_register_count < num_of_total_registers);
  return GetCardinality(get_expected_active_register_count,
                        active_register_count);
}

}  // namespace wfa::estimation
