package org.wfanet.anysketch.distributions;

import java.util.Map;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

public class UniformDistribution extends FingerprintingDistribution {
  public UniformDistribution(
      Fingerprinter fingerprinter, String distributionName, long min, long max) {
    super(distributionName, min, max, fingerprinter);
  }

  @Override
  protected long applyToFingerprint(long fingerprint, Map<String, Long> itemMetadata) {
    return Math.floorMod(fingerprint, getSize()) + getMinValue();
  }
}
