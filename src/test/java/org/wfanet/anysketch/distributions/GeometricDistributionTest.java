package org.wfanet.anysketch.distributions;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

@RunWith(JUnit4.class)
public class GeometricDistributionTest {

  private long fingerprint = 0L;
  private final Fingerprinter fingerprinter = (i) -> fingerprint;
  Distribution distribution = new GeometricDistribution(0L, 64L, fingerprinter);

  private long applyDistributionWithFingerprint(long fingerprint) {
    this.fingerprint = fingerprint;
    return distribution.apply("IrrelevantItem", ImmutableMap.of());
  }

  @Test
  public void testBasicOperation() {
    assertThat(applyDistributionWithFingerprint(0L)).isEqualTo(64L);
    assertThat(applyDistributionWithFingerprint(1L)).isEqualTo(0L);
    assertThat(applyDistributionWithFingerprint(0xABCDEF << 7)).isEqualTo(7L);
    assertThat(applyDistributionWithFingerprint(1L << 14)).isEqualTo(14L);
    assertThat(applyDistributionWithFingerprint(Long.MAX_VALUE)).isEqualTo(0L);
  }

  @Test
  public void testNegativeNumbers() {
    assertThat(applyDistributionWithFingerprint(-1L)).isEqualTo(0L);
    assertThat(applyDistributionWithFingerprint(Long.MIN_VALUE)).isEqualTo(63L);
  }

  @Test
  public void testMinAndMax() {
    distribution = new GeometricDistribution(5L, 10L, fingerprinter);
    assertThat(applyDistributionWithFingerprint(1L)).isEqualTo(5L);
    assertThat(applyDistributionWithFingerprint(0L)).isEqualTo(10L);
    assertThat(applyDistributionWithFingerprint(1L << 10)).isEqualTo(10L);
    assertThat(applyDistributionWithFingerprint(1L << 11)).isEqualTo(10L);
  }
}
