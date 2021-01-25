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

#include "src/main/cc/any_sketch/aggregators.h"

#include <string>

#include "gtest/gtest.h"

namespace wfa::any_sketch {
namespace {

TEST(AggregatorsTest, SumAggregator) {
  const Aggregator& aggregator = GetAggregator(AggregatorType::kSum);

  EXPECT_EQ(aggregator.Aggregate(0, 0), 0);
  EXPECT_EQ(aggregator.Aggregate(-1, 1), 0);
  EXPECT_EQ(aggregator.Aggregate(1, 2), 3);

  EXPECT_EQ(aggregator.EncodeToProtoValue(0), 0);
  EXPECT_EQ(aggregator.EncodeToProtoValue(-1), -1);
  EXPECT_EQ(aggregator.EncodeToProtoValue(123), 123);

  EXPECT_EQ(aggregator.DecodeFromProtoValue(0), 0);
  EXPECT_EQ(aggregator.DecodeFromProtoValue(-1), -1);
  EXPECT_EQ(aggregator.DecodeFromProtoValue(123), 123);
}

TEST(AggregatorsTest, UniqueAggregator) {
  const Aggregator& aggregator = GetAggregator(AggregatorType::kUnique);

  EXPECT_EQ(aggregator.Aggregate(0, 0), 0);
  EXPECT_EQ(aggregator.Aggregate(-1, 1), kUniqueAggregatorDestroyedValue);
  EXPECT_EQ(aggregator.Aggregate(-1, -1), kUniqueAggregatorDestroyedValue);
  EXPECT_EQ(aggregator.Aggregate(1, 2), kUniqueAggregatorDestroyedValue);

  EXPECT_EQ(aggregator.EncodeToProtoValue(0), 1);
  EXPECT_EQ(aggregator.EncodeToProtoValue(-1), 0);
  EXPECT_EQ(aggregator.EncodeToProtoValue(123), 124);

  EXPECT_EQ(aggregator.DecodeFromProtoValue(0), -1);
  EXPECT_EQ(aggregator.DecodeFromProtoValue(-1), -2);
  EXPECT_EQ(aggregator.DecodeFromProtoValue(123), 122);
}
}  // namespace
}  // namespace wfa::any_sketch
