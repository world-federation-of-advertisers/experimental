/*
 * Copyright 2020 The Cross-Media Measurement Authors
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

#ifndef SRC_MAIN_CC_ANY_SKETCH_AGGREGATORS_H_
#define SRC_MAIN_CC_ANY_SKETCH_AGGREGATORS_H_

#include <cstdint>
#include <memory>

namespace wfa::any_sketch {

enum class AggregatorType { kSum, kUnique };

// Interface for aggregating values.
class Aggregator {
 public:
  virtual ~Aggregator() = default;

  // Combines two values.
  virtual int64_t Aggregate(int64_t value1, int64_t value2) const = 0;

  // Converts a value into how it's stored in sketch protos.
  virtual int64_t EncodeToProtoValue(int64_t value) const;

  // Converts a value stored in a proto for use in AnySketch.
  virtual int64_t DecodeFromProtoValue(int64_t value) const;
};

// Returns a singleton instance of an Aggregator of the given type.
const Aggregator& GetAggregator(AggregatorType type);

inline constexpr int64_t kUniqueAggregatorDestroyedValue = -1;

}  // namespace wfa::any_sketch

#endif  // SRC_MAIN_CC_ANY_SKETCH_AGGREGATORS_H_
