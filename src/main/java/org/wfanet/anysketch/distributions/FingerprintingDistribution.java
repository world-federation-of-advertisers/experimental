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
