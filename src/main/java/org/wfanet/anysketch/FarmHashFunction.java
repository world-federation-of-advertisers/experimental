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

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

/**
 * 64bit FarmHash an open-source fingerprinting algorithm for strings that supports equality testing
 * and sorting.
 */
public class FarmHashFunction implements HashFunction {

  /**
   * Returns the long value of HashCode produced from the String parameter.
   *
   * @param item Key to be hashed
   */
  @Override
  public long fingerprint(String item) {
    return Hashing.farmHashFingerprint64().hashString(item, StandardCharsets.UTF_8).asLong();
  }
}
