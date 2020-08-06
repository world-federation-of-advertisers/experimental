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

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link HashFunction} using FarmHash's Fingerprint64 algorithm.
 *
 * <p>See {@link Hashing#farmHashFingerprint64} for details on the algorithm.
 */
public class FarmHashFunction implements HashFunction {

  @Override
  public UnsignedLong fingerprint(String item) {
    HashCode hashCode = Hashing.farmHashFingerprint64().hashString(item, StandardCharsets.UTF_8);
    return UnsignedLong.fromLongBits(hashCode.asLong());
  }
}
