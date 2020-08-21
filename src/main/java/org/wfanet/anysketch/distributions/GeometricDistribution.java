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
