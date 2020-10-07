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

package org.wfanet.anysketch.distributions;

import java.util.Map;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

/** Base class for distributions that operate on a fingerprint of the input item. */
abstract class FingerprintingDistribution extends Distribution {
  private final Fingerprinter fingerprinter;

  /**
   * Creates a FingerprintingDistribution.
   *
   * @param minValue the smallest value that applyToFingerprint will return
   * @param maxValue the largest value that applyToFingerprint will return
   * @param fingerprinter the Fingerprinter to use
   */
  FingerprintingDistribution(long minValue, long maxValue, Fingerprinter fingerprinter) {
    super(minValue, maxValue);
    this.fingerprinter = fingerprinter;
  }

  @Override
  public final long apply(String item, Map<String, Long> itemMetadata) {
    long fingerprint = fingerprinter.fingerprint(item);
    return applyToFingerprint(fingerprint, itemMetadata);
  }

  /**
   * Applies the distribution to a fingerprint.
   *
   * @param fingerprint possibly-negative fingerprint of an item
   * @param itemMetadata oracle values associated with an item inserted into the sketch
   * @return the value from the distribution for the fingerprint
   */
  protected abstract long applyToFingerprint(long fingerprint, Map<String, Long> itemMetadata);
}
