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

package org.wfanet.anysketch.fingerprinters;

/** HashFunction class provides multiple hashing options to hash the key */
public interface Fingerprinter {

  /**
   * Computes a deterministic fingerprint.
   *
   * @param item What to fingerprint
   * @return a 64-bit fingerprint of the input
   */
  long fingerprint(String item);
}
