#include "any_sketch.h"

#include <cstdint>
#include <iostream>
#include <type_traits>

#include "absl/base/macros.h"
#include "absl/container/fixed_array.h"
#include "absl/container/node_hash_map.h"

namespace wfa::any_sketch {

namespace {
// ConsumeBits divides the fingerprint into chunks with max_value size.
uint64_t ConsumeBits(uint64_t& fingerprint, uint64_t max_value) {
  ABSL_ASSERT(max_value > 0);
  uint64_t result = fingerprint % max_value;
  fingerprint /= max_value;
  return result;
}
}  // namespace

AnySketch::AnySketch(
    absl::FixedArray<std::unique_ptr<IndexFunction>> index_functions,
    absl::FixedArray<std::unique_ptr<ValueFunction>> value_functions,
    std::unique_ptr<HashFunction> hash_function)
    : hash_function_(std::move(hash_function)),
      index_functions_(std::move(index_functions)),
      value_functions_(std::move(value_functions)) {}

size_t AnySketch::register_size() const { return value_functions_.size(); }

uint64_t AnySketch::GetIndex(uint64_t partial_fingerprint) const {
  uint64_t product = 1;
  uint64_t linearized_index = 0;
  for (const auto& index_function : index_functions_) {
    uint64_t max_value = index_function->hash_max_value();
    uint64_t index_fingerprint = ConsumeBits(partial_fingerprint, max_value);
    linearized_index += product * index_function->GetIndex(index_fingerprint);
    product *= max_value;
  }
  return linearized_index;
}

void AnySketch::Insert(
    absl::Span<const unsigned char> item,
    const absl::flat_hash_map<std::string, ValueType>& values) {
  ABSL_ASSERT(values.size() == register_size());
  absl::FixedArray<ValueType> values_array(register_size());
  for (size_t i = 0; i < values_array.size(); ++i) {
    values_array[i] = values.at(value_functions_[i]->name());
  }
  Insert(item, values_array);
}

void AnySketch::Insert(absl::Span<const unsigned char> item,
                       absl::Span<const ValueType> values) {
  uint64_t fingerprint = hash_function_->Fingerprint(item);
  InsertPreHashed(fingerprint, values);
}

void AnySketch::InsertPreHashed(uint64_t fingerprint,
                                absl::Span<const ValueType> values) {
  uint64_t index = GetIndex(fingerprint);
  auto [register_itr, inserted] =
      registers_.try_emplace(index, register_size());
  absl::FixedArray<ValueType>& register_values = register_itr->second;
  ABSL_ASSERT(register_values.size() == register_size());
  for (int i = 0; i < register_values.size(); ++i) {
    register_values[i] =
        inserted ? value_functions_[i]->GetInitialValue(values[i])
                 : value_functions_[i]->GetValue(register_values[i], values[i]);
  }
}

void AnySketch::Insert(
    uint64_t item, const absl::flat_hash_map<std::string, ValueType>& values) {
  std::array<unsigned char, sizeof(item)> arr;
  absl::little_endian::Store64(arr.data(), item);
  Insert(arr, values);
}

void AnySketch::Insert(uint64_t item, absl::Span<const ValueType> values) {
  std::array<unsigned char, sizeof(item)> arr;
  absl::little_endian::Store64(arr.data(), item);
  Insert(arr, values);
}

void AnySketch::Insert(
    absl::string_view item,
    const absl::flat_hash_map<std::string, ValueType>& values) {
  absl::Span<const unsigned char> item_as_span = absl::MakeConstSpan(
      reinterpret_cast<const unsigned char*>(item.data()), item.size());
  Insert(item_as_span, values);
}

void AnySketch::Insert(absl::string_view item,
                       absl::Span<const ValueType> values) {
  absl::Span<const unsigned char> item_as_span = absl::MakeConstSpan(
      reinterpret_cast<const unsigned char*>(item.data()), item.size());
  Insert(item_as_span, values);
}

void AnySketch::Merge(const AnySketch& other) {
  for (const auto& reg : other.registers_) {
    InsertPreHashed(reg.first, reg.second);
  }
}

// TODO(yunyeng): Check compatibility
void AnySketch::MergeAll(absl::Span<const std::unique_ptr<AnySketch>> others) {
  for (const auto& other : others) {
    Merge(*other);
  }
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
