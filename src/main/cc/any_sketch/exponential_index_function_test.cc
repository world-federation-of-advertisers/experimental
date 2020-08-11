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

#include "exponential_index_function.h"

#include <cmath>
#include <cstdint>
#include <memory>
#include <string>

#include "gtest/gtest.h"

namespace wfa::any_sketch {
namespace {

TEST(ExponentialIndexFunctionTest, TestBasicBehavior) {
  ExponentialIndexFunction exponential_index_function(10, 10);

  EXPECT_EQ(exponential_index_function.max_value(), 9);
  EXPECT_EQ(exponential_index_function.hash_max_value(),
            std::numeric_limits<uint64_t>::max());
}

TEST(ExponentialIndexFunctionTest, TestGetIndexRateOneSuceeds) {
  ExponentialIndexFunction exponential_index_function(1, 10);

  uint64_t hash_input = 50;

  EXPECT_EQ(exponential_index_function.max_value(), 9);
  EXPECT_EQ(exponential_index_function.hash_max_value(),
            std::numeric_limits<uint64_t>::max());
  EXPECT_EQ(exponential_index_function.GetIndex(hash_input), 0);
}

TEST(ExponentialIndexFunctionTest, TestGetIndexSuceeds) {
  ExponentialIndexFunction exponential_index_function(2, 10000);

  uint64_t hash_input = 5 * std::pow(10, 18);

  EXPECT_EQ(exponential_index_function.max_value(), 9999);
  EXPECT_EQ(exponential_index_function.hash_max_value(),
            std::numeric_limits<uint64_t>::max());
  EXPECT_EQ(exponential_index_function.GetIndex(hash_input), 1335);
}

TEST(ExponentialIndexFunctionTest, TestIllegalRateFails) {
  ASSERT_DEATH(ExponentialIndexFunction(0, 10000), "");
}

TEST(ExponentialIndexFunctionTest, TestIllegalSizeFails) {
  ASSERT_DEATH(ExponentialIndexFunction(10, 0), "");
}

}  // namespace
}  // namespace wfa::any_sketch
