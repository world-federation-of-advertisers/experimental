// Copyright 2020 The AnySketch Authors
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

#include <glog/logging.h>

#include <cstdint>

namespace wfa::any_sketch {
namespace {
class SumAggregator : public Aggregator {
 public:
  int64_t Aggregate(int64_t value1, int64_t value2) const override {
    return value1 + value2;
  }
};

class UniqueAggregator : public Aggregator {
 public:
  int64_t Aggregate(int64_t value1, int64_t value2) const override {
    return (value1 == value2) ? value1 : kUniqueAggregatorDestroyedValue;
  }
  int64_t EncodeToProtoValue(int64_t value) const override { return value + 1; }
  int64_t DecodeFromProtoValue(int64_t value) const override {
    return value - 1;
  }
};

const Aggregator& GetSumAggregator() {
  static const Aggregator* const aggregator = new SumAggregator();
  return *aggregator;
}

const Aggregator& GetUniqueAggregator() {
  static const Aggregator* const aggregator = new UniqueAggregator();
  return *aggregator;
}
}  // namespace

int64_t Aggregator::EncodeToProtoValue(int64_t value) const { return value; }

int64_t Aggregator::DecodeFromProtoValue(int64_t value) const { return value; }

const Aggregator& GetAggregator(AggregatorType type) {
  switch (type) {
    case AggregatorType::kSum:
      return GetSumAggregator();
    case AggregatorType::kUnique:
      return GetUniqueAggregator();
  }
  LOG(FATAL) << "Unsupported AggregatorType: " << static_cast<int>(type);
}
}  // namespace wfa::any_sketch
