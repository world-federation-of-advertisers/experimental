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

#include <algorithm>
#include <cstdint>
#include <iostream>
#include <type_traits>

#include "absl/base/macros.h"
#include "absl/container/fixed_array.h"
#include "absl/container/flat_hash_map.h"
#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"
#include "src/main/cc/any_sketch/aggregators.h"
#include "src/main/cc/any_sketch/distributions.h"
#include "src/main/cc/any_sketch/hash_function.h"
#include "src/main/cc/any_sketch/util/macros.h"
#include "src/main/cc/any_sketch/value_function.h"

namespace wfa::any_sketch {

AnySketch::AnySketch(std::vector<std::unique_ptr<Distribution>> indexes,
                     std::vector<ValueFunction> values)
    : indexes_(indexes.size()), values_(values.size()) {
  std::move(indexes.begin(), indexes.end(), indexes_.begin());
  std::move(values.begin(), values.end(), values_.begin());
}

size_t AnySketch::register_size() const { return values_.size(); }

absl::Status AnySketch::AggregateIntoRegister(
    int64_t index, absl::Span<const int64_t> new_values) {
  if (new_values.size() != register_size()) {
    return absl::InvalidArgumentError(
        absl::StrCat("Input has wrong dimension. Expected ", register_size(),
                     " but got ", new_values.size()));
  }
  ABSL_ASSERT(new_values.size() == register_size());

  auto [register_itr, inserted] =
      registers_.try_emplace(index, register_size());
  absl::FixedArray<ValueType>& register_values = register_itr->second;

  if (inserted) {
    std::copy(new_values.begin(), new_values.end(), register_values.begin());
    return absl::OkStatus();
  }

  // Otherwise, merge.
  for (size_t i = 0; i < register_values.size(); ++i) {
    const Aggregator& aggregator = GetAggregator(values_[i].aggregator_type);
    register_values[i] =
        aggregator.Aggregate(register_values[i], new_values[i]);
  }
  return absl::OkStatus();
}

absl::StatusOr<int64_t> AnySketch::GetIndex(
    absl::string_view item, const ItemMetadata& item_metadata) const {
  uint64_t product = 1;
  uint64_t linearized_index = 0;
  for (const std::unique_ptr<Distribution>& distribution : indexes_) {
    ASSIGN_OR_RETURN(int64_t distribution_value,
                     distribution->Apply(item, item_metadata));
    int64_t index_part = distribution_value - distribution->min_value();
    linearized_index = product * linearized_index + index_part;
    product *= distribution->size();
  }
  return linearized_index;
}

absl::Status AnySketch::Insert(absl::Span<const unsigned char> item,
                               const ItemMetadata& item_metadata) {
  absl::string_view item_as_string_view(
      reinterpret_cast<const char*>(item.data()), item.size());

  return Insert(item_as_string_view, item_metadata);
}

absl::Status AnySketch::Insert(uint64_t item,
                               const ItemMetadata& item_metadata) {
  std::array<unsigned char, sizeof(item)> arr{};
  absl::little_endian::Store64(arr.data(), item);
  return Insert(arr, item_metadata);
}

absl::Status AnySketch::Insert(absl::string_view item,
                               const ItemMetadata& item_metadata) {
  ASSIGN_OR_RETURN(int64_t index, GetIndex(item, item_metadata));
  absl::FixedArray<int64_t> new_values(register_size());
  for (size_t i = 0; i < register_size(); ++i) {
    ASSIGN_OR_RETURN(new_values[i],
                     values_[i].distribution->Apply(item, item_metadata));
  }
  return AggregateIntoRegister(index, new_values);
}

absl::Status AnySketch::Merge(const AnySketch& other) {
  // TODO(yunyeng): Check compatibility
  for (const auto& [item, new_values] : other.registers_) {
    RETURN_IF_ERROR(AggregateIntoRegister(item, new_values));
  }
  return absl::OkStatus();
}

absl::Status AnySketch::MergeAll(
    absl::Span<const std::unique_ptr<AnySketch>> others) {
  for (const auto& other : others) {
    RETURN_IF_ERROR(Merge(*other));
  }
  return absl::OkStatus();
}

AnySketch::Iterator& AnySketch::Iterator::operator++() {
  ++pos_;
  return *this;
}

AnySketch::Register AnySketch::Iterator::operator*() const {
  return {pos_->first, pos_->second};
}

bool AnySketch::Iterator::operator!=(const Iterator& other) const {
  return pos_ != other.pos_;
}

AnySketch::Iterator AnySketch::begin() const {
  return Iterator(registers_.begin());
}

AnySketch::Iterator AnySketch::end() const {
  return Iterator(registers_.end());
}

}  // namespace wfa::any_sketch
