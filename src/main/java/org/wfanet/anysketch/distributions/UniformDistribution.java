package org.wfanet.anysketch.distributions;

import java.util.Map;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

class UniformDistribution extends FingerprintingDistribution {
  UniformDistribution(Fingerprinter fingerprinter, long min, long max) {
    super(min, max, fingerprinter);
  }

  @Override
  protected long applyToFingerprint(long fingerprint, Map<String, Long> itemMetadata) {
    return Long.remainderUnsigned(fingerprint, getSize()) + getMinValue();
  }
}
