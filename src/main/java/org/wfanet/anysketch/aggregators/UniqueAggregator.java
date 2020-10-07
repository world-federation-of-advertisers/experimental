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

package org.wfanet.anysketch.aggregators;

/**
 * Aggregator that tracks a single value.
 *
 * <p>For identical inputs, this returns that value. For different inputs, this returns a special
 * value DESTROYED.
 *
 * <p>To encode more efficiently into protos (since negative numbers consume more bits in int64
 * fields) we shift each value up by 1. In other words, DESTROYED encodes as zero in protos, 0 as 1,
 * 1 as 2, etc.
 */
class UniqueAggregator implements Aggregator {
  static final long DESTROYED = -1;

  @Override
  public long encodeToProtoValue(long value) {
    return value + 1;
  }

  @Override
  public long decodeFromProtoValue(long protoValue) {
    return protoValue - 1;
  }

  @Override
  public long aggregate(long value1, long value2) {
    return value1 == value2 ? value1 : DESTROYED;
  }
}
