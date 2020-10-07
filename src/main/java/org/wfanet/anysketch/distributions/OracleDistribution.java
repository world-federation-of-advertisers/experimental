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

/**
 * Distribution that returns the value associated with a particular metadata key.
 *
 * <p>If the item metadata does not include the specified key or it falls outside the range of
 * allowed values, this will throw.
 */
class OracleDistribution extends Distribution {
  private String featureName;

  /**
   * Constructs an OracleDistribution.
   *
   * @param featureName the metadata key to use
   * @param minValue the smallest value allowed
   * @param maxValue the largest value allowed
   */
  OracleDistribution(String featureName, long minValue, long maxValue) {
    super(minValue, maxValue);
    this.featureName = featureName;
  }

  @Override
  public long apply(String item, Map<String, Long> itemMetadata) {
    Long v = itemMetadata.get(featureName);
    Preconditions.checkArgument(v != null, "Item metadata is missing key %s", featureName);
    if (v < getMinValue() || v > getMaxValue()) {
      throw new IllegalArgumentException(
          String.format(
              "Item metadata value for %s (%d) is out of closed range [%d, %d]",
              featureName, v, getMinValue(), getMaxValue()));
    }
    return v;
  }
}
