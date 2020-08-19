package org.wfanet.anysketch.distributions;

import java.util.Map;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

/** Base class for distributions that operate on a fingerprint of the input item. */
public abstract class FingerprintingDistribution extends Distribution {
  private final Fingerprinter fingerprinter;
  private final String salt;

  /**
   * Creates a FingerprintingDistribution.
   *
   * @param salt a value mixed into fingerprints to ensure uniqueness from other distributions
   * @param minValue the smallest value that applyToFingerprint will return
   * @param maxValue the largest value that applyToFingerprint will return
   * @param fingerprinter the Fingerprinter to use
   */
  public FingerprintingDistribution(
      String salt, long minValue, long maxValue, Fingerprinter fingerprinter) {
    super(minValue, maxValue);
    this.fingerprinter = fingerprinter;
    this.salt = salt;
  }

  @Override
  public final long apply(String item, Map<String, Long> itemMetadata) {
    long fingerprint = fingerprinter.fingerprint(item + salt);
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
