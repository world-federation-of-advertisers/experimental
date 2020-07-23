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
 * IndexFunction maps the output of a {@link HashFunction} to a coordinate in that dimension
 */
interface IndexFunction {

  /**
   * Returns the index of the hashmap given the hashed key
   *
   * @param hash hashed key
   */
  long getIndex(long hash);

  /**
   * Returns the max index value as output value to calculate next indexes
   */
  long maxIndex();

  /**
   * Returns the max supported hash value to divide fingerprint into chunks
   */
  long maxSupportedHash();
}
