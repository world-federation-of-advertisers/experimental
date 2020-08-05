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

package org.wfanet.anysketch;

/**
 * ValueFunction is a commutative binary function that takes two values and combines them into one.
 * For Unique Value Function, if all aggregated values are the same then result of aggregation is
 * equal to the value. Otherwise it is -1. Note that in the proto the destructor will be 0.
 */
public class UniqueValueFunction implements ValueFunction {

  /** The name of this value function is UniqueValueFunction. */
  @Override
  public String name() {
    return "UniqueValueFunction";
  }

  /**
   * The aggregator either stores the value or the destructor (-1). The destructor will be stored as
   * 0 in the proto for space saving reasons.
   */
  @Override
  public long getValue(long oldValue, long newValue) {
    if (oldValue == newValue) {
      return oldValue;
    }
    return -1;
  }

  /**
   * We store the value that is passed to the value function if this is the first assignment to this
   * register.
   */
  @Override
  public long getInitialValue(long newValue) {
    return newValue;
  }

  @Override
  public long encodeToProtoValue(long value) {
    return value + 1;
  }

  @Override
  public long decodeFromProtoValue(long protoValue) {
    return protoValue - 1;
  }
}
