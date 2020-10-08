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

#include "src/main/cc/any_sketch/any_sketch.h"

#include <cstdint>
#include <memory>
#include <string>
#include <utility>

#include "absl/container/fixed_array.h"
#include "absl/memory/memory.h"
#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/strings/str_join.h"
#include "absl/types/span.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "src/test/cc/wfa/any_sketch/matchers.h"

namespace wfa::any_sketch {
namespace {
using ::testing::ExplainMatchResult;
using ::testing::IsEmpty;
using ::testing::Matcher;
using ::testing::MatcherInterface;
using ::testing::MatchResultListener;
using ::testing::UnorderedElementsAre;

class RegisterIsMatcher : public MatcherInterface<const AnySketch::Register&> {
 public:
  explicit RegisterIsMatcher(uint64_t index, absl::FixedArray<int64_t> values)
      : index_(index), values_(std::move(values)) {}

  bool MatchAndExplain(const wfa::any_sketch::AnySketch::Register& reg,
                       MatchResultListener* /* listener */) const override {
    return reg.index == index_ && reg.values.size() == values_.size();
  }

  void DescribeTo(std::ostream* os) const override {
    *os << "has index " << index_ << " and values ["
        << absl::StrJoin(values_, ", ") << "]";
  }

  void DescribeNegationTo(std::ostream* os) const override {
    *os << "does not have index " << index_ << " and values ["
        << absl::StrJoin(values_, ", ") << "]";
  }

 private:
  uint64_t index_;
  absl::FixedArray<int64_t> values_;
};

Matcher<AnySketch::Register> RegisterIs(uint64_t index,
                                        absl::FixedArray<int64_t> values) {
  return Matcher<AnySketch::Register>(
      new RegisterIsMatcher(index, std::move(values)));
}

class FakeDistribution : public Distribution {
 public:
  absl::StatusOr<int64_t> Apply(
      absl::string_view item,
      const ItemMetadata& item_metadata) const override {
    return item.size();
  }

  int64_t min_value() const override { return 0; }

  int64_t max_value() const override { return 10; }
};

std::unique_ptr<Distribution> MakeFakeDistribution() {
  return absl::make_unique<FakeDistribution>();
}

template <class T>
std::vector<T> MakeSingleItemVector(T&& t) {
  std::vector<T> v;
  v.push_back(std::forward<T>(t));
  return v;
}

std::vector<std::unique_ptr<Distribution>> MakeFakeDistributionIndex() {
  return MakeSingleItemVector(MakeFakeDistribution());
}

ValueFunction MakeValueFunction(AggregatorType aggregator,
                                std::unique_ptr<Distribution> distribution) {
  return {.name = "SomeValueFunction",
          .aggregator_type = aggregator,
          .distribution = std::move(distribution)};
}

ValueFunction MakeOracleValueFunction(absl::string_view feature) {
  return MakeValueFunction(AggregatorType::kSum,
                           GetOracleDistribution(feature, 5, 15));
}

std::vector<AnySketch::Register> GetRegisters(const AnySketch& sketch) {
  return {sketch.begin(), sketch.end()};
}

TEST(AnySketchTest, EmptySketch) {
  AnySketch sketch(MakeFakeDistributionIndex(), {});

  EXPECT_THAT(GetRegisters(sketch), IsEmpty());
}

TEST(AnySketchTest, SingleOracleDistribution) {
  AnySketch sketch(MakeFakeDistributionIndex(),
                   MakeSingleItemVector(MakeOracleValueFunction("foo")));

  EXPECT_THAT(GetRegisters(sketch), IsEmpty());

  ASSERT_THAT(sketch.Insert("abc", {{"foo", 5}}), IsOk());
  EXPECT_THAT(GetRegisters(sketch), UnorderedElementsAre(RegisterIs(3, {5})));

  // Should aggregate by summation
  ASSERT_THAT(sketch.Insert("abc", {{"foo", 9}}), IsOk());
  EXPECT_THAT(GetRegisters(sketch), UnorderedElementsAre(RegisterIs(3, {14})));

  // Different register
  ASSERT_THAT(sketch.Insert("abcdef", {{"foo", 7}}), IsOk());
  EXPECT_THAT(GetRegisters(sketch),
              UnorderedElementsAre(RegisterIs(3, {14}), RegisterIs(6, {7})));

  EXPECT_THAT(sketch.Insert("abcdef", {{"wrong-key", 7}}), IsNotOk());
}

TEST(AnySketchTest, MultipleOracleDistributions) {
  std::vector<ValueFunction> value_functions;
  value_functions.push_back(MakeOracleValueFunction("key1"));
  value_functions.push_back(MakeOracleValueFunction("key2"));
  AnySketch sketch(MakeFakeDistributionIndex(), std::move(value_functions));

  ASSERT_THAT(sketch.Insert("ABCD", {{"key1", 10}, {"key2", 11}}), IsOk());

  EXPECT_THAT(GetRegisters(sketch),
              UnorderedElementsAre(RegisterIs(4, {10, 11})));
}

TEST(AnySketchTest, FakeDistribution) {
  AnySketch sketch(MakeFakeDistributionIndex(),
                   MakeSingleItemVector(MakeValueFunction(
                       AggregatorType::kSum, MakeFakeDistribution())));

  ASSERT_THAT(sketch.Insert("a", {}), IsOk());
  ASSERT_THAT(sketch.Insert("b", {}), IsOk());
  ASSERT_THAT(sketch.Insert("c", {}), IsOk());
  ASSERT_THAT(sketch.Insert("aa", {}), IsOk());

  EXPECT_THAT(GetRegisters(sketch),
              UnorderedElementsAre(RegisterIs(1, {3}), RegisterIs(2, {2})));
}

TEST(AnySketchTest, Merge) {
  auto make_sketch = []() {
    return AnySketch(MakeFakeDistributionIndex(),
                     MakeSingleItemVector(MakeOracleValueFunction("foo")));
  };

  AnySketch sketch1 = make_sketch();
  AnySketch sketch2 = make_sketch();

  ASSERT_THAT(sketch1.Insert("a", {{"foo", 5}}), IsOk());
  ASSERT_THAT(sketch1.Insert("aa", {{"foo", 6}}), IsOk());

  ASSERT_THAT(sketch2.Insert("a", {{"foo", 7}}), IsOk());
  ASSERT_THAT(sketch2.Insert("aaa", {{"foo", 8}}), IsOk());

  ASSERT_THAT(sketch1.Merge(sketch2), IsOk());
  EXPECT_THAT(GetRegisters(sketch1),
              UnorderedElementsAre(RegisterIs(1, {12}), RegisterIs(2, {6}),
                                   RegisterIs(3, {8})));
}
}  // namespace
}  // namespace wfa::any_sketch
