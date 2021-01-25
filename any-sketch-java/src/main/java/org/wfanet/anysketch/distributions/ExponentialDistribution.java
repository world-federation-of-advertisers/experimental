// Copyright 2020 The Cross-Media Measurement Authors
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
import com.google.common.primitives.UnsignedLong;
import java.util.Map;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

/**
 * Maps the output of a {@link Fingerprinter} to a coordinate using an exponential distribution with
 * param rate and multiplies the output with param size.
 */
class ExponentialDistribution extends FingerprintingDistribution {

  private final double rate;
  private final double expRate;
  private final long size;

  /**
   * Creates ExponentialIndexFunction object expecting rate and size arguments.
   *
   * @param rate Rate of the exponential distribution
   * @param size Number of legionaries
   */
  ExponentialDistribution(Fingerprinter fingerprinter, double rate, long size) {
    super(0, size - 1, fingerprinter);
    Preconditions.checkArgument(rate > 0);
    Preconditions.checkArgument(size > 0);
    this.rate = rate;
    this.expRate = Math.exp(rate);
    this.size = size;
  }

  @Override
  protected long applyToFingerprint(long fingerprint, Map<String, Long> itemMetadata) {
    double fingerprintAsDouble = UnsignedLong.fromLongBits(fingerprint).doubleValue();
    double u = fingerprintAsDouble / UnsignedLong.MAX_VALUE.doubleValue();
    double x = 1 - Math.log(expRate + u * (1 - expRate)) / rate;
    double fractionalIndex = x * size;
    return (long) fractionalIndex;
  }
}
