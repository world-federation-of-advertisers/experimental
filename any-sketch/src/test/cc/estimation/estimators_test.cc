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

#include "src/main/cc/estimation/estimators.h"

#include <cstdint>
#include <iostream>
#include <type_traits>

#include "absl/container/fixed_array.h"
#include "absl/container/flat_hash_set.h"
#include "absl/strings/str_cat.h"
#include "absl/types/span.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "src/main/cc/any_sketch/distributions.h"
#include "src/main/cc/any_sketch/fingerprinters.h"

namespace wfa::estimation {
namespace {
using ::wfa::any_sketch::Distribution;
using ::wfa::any_sketch::Fingerprinter;

MATCHER_P2(EqWithError, value, error, "") {
  return ExplainMatchResult(testing::Le(error), std::llabs(arg - value),
                            result_listener);
}

uint64_t GetExpectedActiveRegisterCount(double decay_rate,
                                        uint64_t num_of_total_registers,
                                        uint64_t cardinality) {
  ABSL_ASSERT(decay_rate > 1.0);
  ABSL_ASSERT(num_of_total_registers > 0);
  if (cardinality == 0) return 0;
  double exp_rate = std::exp(decay_rate);
  double t = cardinality / static_cast<double>(num_of_total_registers);
  double negative_term = -std::expint((-decay_rate * t) / (exp_rate - 1));
  double positive_term =
      std::expint((-decay_rate * exp_rate * t) / (exp_rate - 1));
  return (1 - (negative_term + positive_term) / decay_rate) *
         num_of_total_registers;
}

uint64_t GenerateRandomSketchAndGetSize(double decay_rate,
                                        uint64_t num_of_total_registers,
                                        uint64_t num_of_fingerprints) {
  absl::flat_hash_set<int64_t> indexes;

  const Fingerprinter& fingerprinter =
      wfa::any_sketch::GetSha256Fingerprinter();
  std::unique_ptr<Distribution> exponential_distribution =
      GetExponentialDistribution(&fingerprinter, decay_rate,
                                 num_of_total_registers);

  for (uint64_t i = 0; i < num_of_fingerprints; ++i) {
    absl::StatusOr<int64_t> value =
        exponential_distribution->Apply(absl::StrCat(i), {});
    ABSL_ASSERT(value.ok());
    indexes.insert(value.value());
  }
  return indexes.size();
}

TEST(UtilsTest, TestEmptyReturnsZero) {
  uint64_t result = EstimateCardinalityLiquidLegions(10, 100000, 0);
  EXPECT_EQ(result, 1);
}

TEST(UtilsTest, TestEstimationCorrectWithExpectation) {
  double rate = 10;
  uint64_t sketch_size = 100000;
  uint64_t max_cardinality = 1000000;

  for (uint64_t actual_cardinality = 1; actual_cardinality <= max_cardinality;
       actual_cardinality += 1000) {
    uint64_t num_expected_active_registers =
        GetExpectedActiveRegisterCount(rate, sketch_size, actual_cardinality);
    EXPECT_THAT(EstimateCardinalityLiquidLegions(rate, sketch_size,
                                                 num_expected_active_registers),
                EqWithError(actual_cardinality, actual_cardinality * 0.05))
        << "actual_cardinality=" << actual_cardinality;
  }
}

TEST(UtilsTest, TestEstimationCorrectWithExpectationDifferentRates) {
  uint64_t sketch_size = 100000;
  uint64_t actual_cardinality = 1000000;

  for (uint64_t rate = 5; rate <= 30; ++rate) {
    uint64_t num_expected_active_registers =
        GetExpectedActiveRegisterCount(rate, sketch_size, actual_cardinality);
    EXPECT_THAT(EstimateCardinalityLiquidLegions(rate, sketch_size,
                                                 num_expected_active_registers),
                EqWithError(actual_cardinality, actual_cardinality * 0.05))
        << "rate=" << rate;
  }
}

TEST(UtilsTest, TestEstimationCorrectWithMockData) {
  double rate = 10;
  uint64_t sketch_size = 100000;

  uint64_t actual_cardinality = 123456;
  uint64_t num_active_registers =
      GenerateRandomSketchAndGetSize(rate, sketch_size, actual_cardinality);
  EXPECT_THAT(
      EstimateCardinalityLiquidLegions(rate, sketch_size, num_active_registers),
      EqWithError(actual_cardinality, actual_cardinality * 0.05));
}

}  // namespace
}  // namespace wfa::estimation
