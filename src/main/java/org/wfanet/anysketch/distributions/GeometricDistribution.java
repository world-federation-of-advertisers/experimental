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

import com.google.common.base.Preconditions;
import java.util.Map;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

/**
 * Geometric distribution with success probability 0.5.
 *
 * <p>The probability of returning getMinValue() + k is 0.5^(k+1), except that the probability of
 * returning getMaxValue() is 0.5^(getSize()-1).
 *
 * <p>It is an error to build a GeometricDistribution with size exceeding 65.
 */
class GeometricDistribution extends FingerprintingDistribution {

  GeometricDistribution(long minValue, long maxValue, Fingerprinter fingerprinter) {
    super(minValue, maxValue, fingerprinter);
    Preconditions.checkArgument(getSize() <= 65);
  }

  @Override
  protected long applyToFingerprint(long fingerprint, Map<String, Long> itemMetadata) {
    long trailingZeros = Long.numberOfTrailingZeros(fingerprint);
    return Long.min(trailingZeros, getSize() - 1) + getMinValue();
  }
}
