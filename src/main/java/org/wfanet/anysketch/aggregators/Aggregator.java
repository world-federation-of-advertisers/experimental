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

/** Interface for combining values and serializing into protos. */
public interface Aggregator {
  /** Combines two values. */
  long aggregate(long value1, long value2);

  /** Converts a value into how it's stored in sketch protos. */
  default long encodeToProtoValue(long value) {
    return value;
  }

  /** Converts a value stored in a proto for use in the AnySketch Java class. */
  default long decodeFromProtoValue(long protoValue) {
    return protoValue;
  }
}
