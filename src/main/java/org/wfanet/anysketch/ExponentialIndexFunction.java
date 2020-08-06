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

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;

/**
 * ExponentialIndexFunction maps the output of a {@link HashFunction} to a coordinate using
 * exponential distribution with param rate and multiplies the output with param size.
 */
public class ExponentialIndexFunction implements IndexFunction {

  private final double rate;
  private final double expRate;
  private final UnsignedLong size;

  /**
   * Creates ExponentialIndexFunction object expecting rate and size arguments.
   *
   * @param rate Rate of the exponential distribution
   * @param size Number of legionaries
   */
  public ExponentialIndexFunction(double rate, UnsignedLong size) {
    Preconditions.checkArgument(rate > 0);
    Preconditions.checkArgument(!size.equals(UnsignedLong.ZERO));
    this.rate = rate;
    this.expRate = Math.exp(rate);
    this.size = size;
  }

  /**
   * Returns the register index of a fingerprint.
   *
   * @param fingerprint hashed key
   *     <p>ExponentialIndexFunction getIndex uses inverse CDF of the truncated exponential
   *     distribution. To sample from number of legionaries, it multiplies the result with the size.
   *     <p>It is possible to precompute inverse CDF for each register and do a O(log(size)) lookup
   *     for a fingerprint. This implementation takes a simpler approach of calculating inverse CDF
   *     for each fingerprint separately.
   */
  @Override
  public UnsignedLong getIndex(UnsignedLong fingerprint) {
    Preconditions.checkArgument(fingerprint.compareTo(maxSupportedHash()) <= 0);
    double u = fingerprint.doubleValue() / maxSupportedHash().doubleValue();
    double x = 1 - Math.log(expRate + u * (1 - expRate)) / rate;
    double fractionalIndex = x * size.doubleValue();
    return roundDoubleToUnsignedLong(fractionalIndex);
  }

  private UnsignedLong roundDoubleToUnsignedLong(double value) {
    return UnsignedLong.valueOf(BigDecimal.valueOf(value).toBigInteger());
  }

  @Override
  public UnsignedLong maxIndex() {
    return size.minus(UnsignedLong.ONE);
  }

  @Override
  public UnsignedLong maxSupportedHash() {
    // TODO: this should be a much smaller value.
    return UnsignedLong.MAX_VALUE;
  }
}
