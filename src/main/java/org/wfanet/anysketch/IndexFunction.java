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

import com.google.common.primitives.UnsignedLong;

/** IndexFunction maps the output of a {@link HashFunction} to a coordinate in that dimension */
interface IndexFunction {

  /**
   * Returns the register index corresponding to a fingerprint.
   *
   * <p>It is an error to pass in a fingerprint larger than maxSupportedHash().
   *
   * @param fingerprint the fingerprint of an item; see {@link HashFunction#fingerprint}
   * @return a register index in the closed interval [0, maxIndex()]
   */
  UnsignedLong getIndex(UnsignedLong fingerprint);

  /** Returns the maximum register index that getIndex() can return. */
  UnsignedLong maxIndex();

  /** Returns the maximum supported value for input to {@link #getIndex}. */
  UnsignedLong maxSupportedHash();
}
