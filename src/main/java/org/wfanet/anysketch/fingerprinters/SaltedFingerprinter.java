package org.wfanet.anysketch.fingerprinters;

public class SaltedFingerprinter implements Fingerprinter {
  private final Fingerprinter base;
  private final String salt;

  public SaltedFingerprinter(String salt, Fingerprinter base) {
    this.base = base;
    this.salt = salt;
  }

  @Override
  public long fingerprint(String item) {
    return base.fingerprint(applySalt(item));
  }

  private String applySalt(String item) {
    return String.format("AnySketchFingerprint:%s:%s", item, salt);
  }
}
