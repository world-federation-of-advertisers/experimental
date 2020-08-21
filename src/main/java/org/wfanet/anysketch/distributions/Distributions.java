package org.wfanet.anysketch.distributions;

import org.wfanet.anysketch.fingerprinters.Fingerprinter;

public class Distributions {
  private Distributions() {}

  public static Distribution oracle(String featureName, long minValue, long maxValue) {
    return new OracleDistribution(featureName, minValue, maxValue);
  }

  public static Distribution uniform(Fingerprinter fingerprinter, long minValue, long maxValue) {
    return new UniformDistribution(fingerprinter, minValue, maxValue);
  }

  public static Distribution exponential(Fingerprinter fingerprinter, double rate, long size) {
    return new ExponentialDistribution(fingerprinter, rate, size);
  }

  public static Distribution geometric(Fingerprinter fingerprinter, long minValue, long maxValue) {
    return new GeometricDistribution(minValue, maxValue, fingerprinter);
  }
}
