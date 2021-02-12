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

#ifndef SRC_MAIN_CC_WFA_ANY_SKETCH_ANY_SKETCH_H_
#define SRC_MAIN_CC_WFA_ANY_SKETCH_ANY_SKETCH_H_

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "absl/container/fixed_array.h"
#include "absl/container/flat_hash_map.h"
#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"
#include "absl/types/span.h"
#include "src/main/cc/any_sketch/distributions.h"
#include "src/main/cc/any_sketch/fingerprinters.h"
#include "src/main/cc/any_sketch/value_function.h"

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
    absl::Span<const ValueType> values;
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

    using MapType = absl::flat_hash_map<uint64_t, absl::FixedArray<ValueType>>;
    using MapIteratorType = MapType::const_iterator;

    explicit Iterator(MapIteratorType pos) : pos_(pos) {}

    MapIteratorType pos_;
  };

  // Creates a new, empty AnySketch.
  //
  // The inputs will be moved from.
  AnySketch(std::vector<std::unique_ptr<Distribution>> indexes,
            std::vector<ValueFunction> values);

  AnySketch(const AnySketch &) = delete;
  AnySketch &operator=(const AnySketch &) = delete;

  ~AnySketch() = default;

  // Merges a set of values into a register.
  ABSL_MUST_USE_RESULT absl::Status AggregateIntoRegister(
      int64_t index, absl::Span<const int64_t> values);

  // Adds `item` to the Sketch.
  //
  // While itemMetadata can contain arbitrary values, certain Distributions may
  // require some keys to be present. In particular, OracleDistributions each
  // require a specific key to exist.
  //
  // It is not an error to include unnecessary keys in item_metadata--they will
  // simply be ignored.
  ABSL_MUST_USE_RESULT absl::Status Insert(absl::Span<const unsigned char> item,
                                           const ItemMetadata &item_metadata);
  ABSL_MUST_USE_RESULT absl::Status Insert(uint64_t item,
                                           const ItemMetadata &item_metadata);
  ABSL_MUST_USE_RESULT absl::Status Insert(absl::string_view item,
                                           const ItemMetadata &item_metadata);

  // Merges the other sketch into this one. The result is equivalent to
  // sketching the union of the sets that went into this and the other sketch.
  ABSL_MUST_USE_RESULT absl::Status Merge(const AnySketch &other);

  // Merges all the other sketches into this one. The resuls is equivalent to
  // sketching the union of all of the sets.
  ABSL_MUST_USE_RESULT absl::Status MergeAll(
      absl::Span<const std::unique_ptr<AnySketch>> others);

  Iterator begin() const;

  Iterator end() const;

 private:
  absl::flat_hash_map<uint64_t, absl::FixedArray<ValueType>> registers_;
  absl::FixedArray<std::unique_ptr<Distribution>> indexes_;
  absl::FixedArray<ValueFunction> values_;

  size_t register_size() const;

  absl::StatusOr<int64_t> GetIndex(absl::string_view item,
                                   const ItemMetadata &item_metadata) const;
};

}  // namespace wfa::any_sketch

#endif  // SRC_MAIN_CC_WFA_ANY_SKETCH_ANY_SKETCH_H_
