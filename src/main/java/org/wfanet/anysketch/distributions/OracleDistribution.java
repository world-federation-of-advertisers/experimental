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
