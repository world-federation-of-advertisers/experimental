/*
 * Copyright 2020 The Any Sketch Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ADS_XMEDIA_LANCELOT_ANY_SKETCH_ANY_SKETCH_H_
#define ADS_XMEDIA_LANCELOT_ANY_SKETCH_ANY_SKETCH_H_

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "absl/container/fixed_array.h"
#include "absl/container/flat_hash_map.h"
#include "absl/container/node_hash_map.h"
#include "absl/random/bit_gen_ref.h"
#include "hash_function.h"
#include "index_function.h"
#include "value_function.h"

namespace wfa::any_sketch {
// A generalized sketch class.
// This sketch class generalizes the data structure required to
// capture Bloom filters, HLLs, Cascading Legions, Vector of Counts, and
// other sketch types. It uses a map of register keys to a register
// value which is a tuple of counts.
class AnySketch {
 public:
  // Each register of the sketch holds a tuple of ValueTypes. Depending on the
  // ValueFunction, these serve as indicators or counts.
  using ValueType = int64_t;

  struct Register {
    uint64_t index;
    absl::FixedArray<ValueType> values;
  };

  class Iterator {
   public:
    using iterator_category = std::input_iterator_tag;
    using value_type = Register;
    using difference_type = void;
    using pointer = void;
    using reference = value_type;

    Register operator*() const;

    Iterator &operator++();

    bool operator!=(const Iterator &other) const;

   private:
    friend class AnySketch;

    explicit Iterator(
        absl::node_hash_map<uint64_t,
                            absl::FixedArray<ValueType>>::const_iterator pos)
        : pos_(pos) {}
    absl::node_hash_map<uint64_t, absl::FixedArray<ValueType>>::const_iterator
        pos_;
  };

  // Creates AnySketch object expecting IndexFunction, ValueFunction,
  // and HashFunction arguments.
  // Takes many IndexFunction to determine the index of the hashmap.
  // Takes many ValueFunction to be able to insert value into hashmap.
  // Takes a single HashFunction to use one of the different hashing options.
  AnySketch(absl::FixedArray<std::unique_ptr<IndexFunction>> index_functions,
            absl::FixedArray<std::unique_ptr<ValueFunction>> value_functions,
            std::unique_ptr<HashFunction> hash_function);

  AnySketch(const AnySketch &) = delete;

  AnySketch &operator=(const AnySketch &) = delete;

  ~AnySketch() = default;

  // Adds `item` to the Sketch.
  //
  // Insert determines a register by hashing `item` and applying the index
  // functions described in the config. The values in the register are updated
  // by invoking the value functions described in the config with the old value
  // and the value provided here to produce a new value.
  //
  // If `values` is a flat_hash_map, it must contain a key for each value
  // name in the config. If `values` is a Span, it must have the same number of
  // values as in the config.
  //
  // The values are presumed to be in the same order as
  // described in the config.
  void Insert(absl::Span<const unsigned char> item,
              const absl::flat_hash_map<std::string, ValueType> &values);

  void Insert(absl::Span<const unsigned char> item,
              absl::Span<const ValueType> values);

  void Insert(uint64_t item,
              const absl::flat_hash_map<std::string, ValueType> &values);

  void Insert(uint64_t item, absl::Span<const ValueType> values);

  void Insert(absl::string_view item,
              const absl::flat_hash_map<std::string, ValueType> &values);

  void Insert(absl::string_view item, absl::Span<const ValueType> values);

  // Merges the other sketch into this one. The result is equivalent to
  // sketching the union of the sets that went into this and the other sketch.
  void Merge(const AnySketch &other);

  // Merges all the other sketches into this one. The resuls is equivalent to
  // sketching the union of all of the sets.
  void MergeAll(absl::Span<const std::unique_ptr<AnySketch>> others);

  Iterator begin() const;

  Iterator end() const;

 private:
  absl::node_hash_map<uint64_t, absl::FixedArray<ValueType>> registers_;
  std::unique_ptr<HashFunction> hash_function_;
  absl::FixedArray<std::unique_ptr<IndexFunction>> index_functions_;
  absl::FixedArray<std::unique_ptr<ValueFunction>> value_functions_;

  uint64_t GetIndex(uint64_t partial_fingerprint) const;

  size_t register_size() const;

  uint64_t UpdateFingerprint(uint64_t *fingerprint, uint64_t max_value);

  void InsertPreHashed(uint64_t fingerprint,
                       absl::Span<const ValueType> values);
};

}  // namespace wfa::any_sketch

#endif  // ADS_XMEDIA_LANCELOT_ANY_SKETCH_ANY_SKETCH_H_
