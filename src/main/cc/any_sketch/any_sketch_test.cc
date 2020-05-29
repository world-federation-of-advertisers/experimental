#include "any_sketch.h"

#include <cstdint>
#include <memory>
#include <string>

#include "absl/types/span.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"

namespace wfa::any_sketch {
namespace {

using ::testing::Matcher;
using ::testing::MatchResultListener;
using ::testing::UnorderedElementsAre;

class FakeIndexFunction : public IndexFunction {
 public:
  FakeIndexFunction() = default;
  uint64_t GetIndex(uint64_t hash) const override { return hash * 10; }

  uint64_t max_value() const override { return 1000; }

  uint64_t hash_max_value() const override { return 100; }
};

class FakeValueFunction : public ValueFunction {
 public:
  FakeValueFunction() = default;
  absl::string_view name() const override { return "some-value-function-name"; }

  int64_t GetInitialValue(int64_t x) const override { return x * 100; }
  int64_t GetValue(int64_t x, int64_t y) const override { return x + y; }
};

class FakeHashFunction : public HashFunction {
 public:
  FakeHashFunction() = default;
  uint64_t Fingerprint(absl::Span<const unsigned char> x) const override {
    return x[0];
  }
};

class RegisterIsMatcher
    : public testing::MatcherInterface<const AnySketch::Register&> {
 public:
  explicit RegisterIsMatcher(uint64_t index, absl::FixedArray<int64_t> values)
      : index_(index), values_(values) {}

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
  return Matcher<AnySketch::Register>(new RegisterIsMatcher(index, values));
}

std::unique_ptr<AnySketch> MakeBasicAnySketch() {
  absl::FixedArray<std::unique_ptr<IndexFunction>> index_functions(1);
  index_functions[0] = absl::make_unique<FakeIndexFunction>();

  absl::FixedArray<std::unique_ptr<ValueFunction>> value_functions(1);
  value_functions[0] = absl::make_unique<FakeValueFunction>();

  auto hash_function = absl::make_unique<FakeHashFunction>();

  return absl::make_unique<AnySketch>(std::move(index_functions),
                                      std::move(value_functions),
                                      std::move(hash_function));
}

TEST(AnySketchTest, TestInsertMethod) {
  std::unique_ptr<AnySketch> any_sketch = MakeBasicAnySketch();

  any_sketch->Insert(1, {1});

  std::vector<AnySketch::Register> registers(any_sketch->begin(),
                                             any_sketch->end());
  EXPECT_THAT(registers, UnorderedElementsAre(RegisterIs(10, {10})));
}

TEST(AnySketchTest, TestInsertMethodWithHashMap) {
  std::unique_ptr<AnySketch> any_sketch = MakeBasicAnySketch();

  any_sketch->Insert(1, {
                            {"some-value-function-name", 1},
                        });
  std::vector<AnySketch::Register> registers(any_sketch->begin(),
                                             any_sketch->end());
  EXPECT_THAT(registers, UnorderedElementsAre(RegisterIs(10, {10})));
}

TEST(AnySketchTest, TestInsertMethodStringView) {
  std::unique_ptr<AnySketch> any_sketch = MakeBasicAnySketch();
  any_sketch->Insert("1", {1});
  std::vector<AnySketch::Register> registers(any_sketch->begin(),
                                             any_sketch->end());
  // We expect the fingerprint of "1" to be the first byte, e.g. 49
  // ('1' in ascii is 49). So the linearized index is 49 * 10 = 490.
  EXPECT_THAT(registers, UnorderedElementsAre(RegisterIs(490, {10})));
}

TEST(AnySketchTest, TestInsertMethodStringViewWithHashMap) {
  std::unique_ptr<AnySketch> any_sketch = MakeBasicAnySketch();
  any_sketch->Insert("1", {
                              {"some-value-function-name", 1},
                          });
  std::vector<AnySketch::Register> registers(any_sketch->begin(),
                                             any_sketch->end());
  // We expect the fingerprint of "1" to be the first byte, e.g. 49
  // ('1' in ascii is 49). So the linearized index is 49 * 10 = 490.
  EXPECT_THAT(registers, UnorderedElementsAre(RegisterIs(490, {10})));
}

TEST(AnySketchTest, TestInsertMethodUnsignedChar) {
  std::unique_ptr<AnySketch> any_sketch = MakeBasicAnySketch();

  any_sketch->Insert({'a', 'b', 'c'}, {999});

  std::vector<AnySketch::Register> registers(any_sketch->begin(),
                                             any_sketch->end());
  EXPECT_THAT(registers, UnorderedElementsAre(RegisterIs(970, {9990})));
}

TEST(AnySketchTest, TestInsertMethodUnsignedCharWithHashMap) {
  std::unique_ptr<AnySketch> any_sketch = MakeBasicAnySketch();

  any_sketch->Insert({'a', 'b', 'c'}, {
                                          {"some-value-function-name", 1},
                                      });

  std::vector<AnySketch::Register> registers(any_sketch->begin(),
                                             any_sketch->end());
  EXPECT_THAT(registers, UnorderedElementsAre(RegisterIs(970, {10})));
}

TEST(AnySketchTest, TestMergeMethod) {
  std::unique_ptr<AnySketch> any_sketch = MakeBasicAnySketch();

  absl::FixedArray<std::unique_ptr<IndexFunction>> other_index_functions(1);
  other_index_functions[0] = absl::make_unique<FakeIndexFunction>();

  absl::FixedArray<std::unique_ptr<ValueFunction>> other_value_functions(1);
  other_value_functions[0] = absl::make_unique<FakeValueFunction>();

  auto other_hash_function = absl::make_unique<FakeHashFunction>();
  AnySketch other_any_sketch(std::move(other_index_functions),
                             std::move(other_value_functions),
                             std::move(other_hash_function));

  any_sketch->Insert(1, {1});
  other_any_sketch.Insert(2, {20});
  any_sketch->Merge(other_any_sketch);

  std::vector<AnySketch::Register> registers(any_sketch->begin(),
                                             any_sketch->end());
  EXPECT_THAT(registers, UnorderedElementsAre(RegisterIs(10, {100}),
                                              RegisterIs(200, {200000})));
}

TEST(AnySketchTest, TestMergeAllMethod) {
  std::unique_ptr<AnySketch> any_sketch = MakeBasicAnySketch();
  std::unique_ptr<AnySketch> other_any_sketch = MakeBasicAnySketch();

  any_sketch->Insert(1, {1});
  other_any_sketch->Insert(2, {20});
  any_sketch->MergeAll({std::move(other_any_sketch)});

  std::vector<AnySketch::Register> registers(any_sketch->begin(),
                                             any_sketch->end());
  EXPECT_THAT(registers, UnorderedElementsAre(RegisterIs(10, {100}),
                                              RegisterIs(200, {200000})));
}

}  // namespace
}  // namespace wfa::any_sketch
