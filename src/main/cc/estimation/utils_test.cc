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

#include "utils.h"

#include "absl/types/span.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"

namespace wfa::estimation {
namespace {

class UtilsTest : public ::testing::Test {};

TEST(UtilsTest, TestEstimateCardinalityLiquidLegions) {
  uint64_t result = EstimateCardinalityLiquidLegions(0.0, 0, 0);
  EXPECT_EQ(result, 0);
}

}  // namespace
}  // namespace wfa::estimation