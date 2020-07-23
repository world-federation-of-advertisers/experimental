package org.wfanet.anysketch;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExponentialIndexFunctionTest {

  private static final long EXPECTED_MAX_HASH = 9223372036854775807L; // max long

  @Test
  public void ExponentialIndexFunctionTest_testBasicBehavior() {
    ExponentialIndexFunction exponentialIndexFunction = new ExponentialIndexFunction(10, 10);
    assertThat(exponentialIndexFunction.maxIndex()).isEqualTo(9);
    assertThat(exponentialIndexFunction.maxSupportedHash()).isEqualTo(EXPECTED_MAX_HASH);
  }

  @Test
  public void ExponentialIndexFunctionTest_testGetIndexRateOneSuceeds() {
    // Maps everything to the first register.
    ExponentialIndexFunction exponentialIndexFunction = new ExponentialIndexFunction(1, 10);
    long hash_input = 50;
    assertThat(exponentialIndexFunction.maxIndex()).isEqualTo(9);
    assertThat(exponentialIndexFunction.maxSupportedHash()).isEqualTo(EXPECTED_MAX_HASH);
    assertThat(exponentialIndexFunction.getIndex(hash_input)).isEqualTo(0);
  }

  @Test
  public void ExponentialIndexFunctionTest_testGetIndexSuceeds() {
    ExponentialIndexFunction exponentialIndexFunction = new ExponentialIndexFunction(2, 10000);
    long hash_input = 5l * (long) Math.pow(10, 18);
    assertThat(exponentialIndexFunction.maxIndex()).isEqualTo(9999);
    assertThat(exponentialIndexFunction.maxSupportedHash()).isEqualTo(EXPECTED_MAX_HASH);
    assertThat(exponentialIndexFunction.getIndex(hash_input)).isEqualTo(3162);
  }

  @Test
  public void ExponentialIndexFunctionTest_testIllegalRateFails() {
    assertThrows(
        java.lang.IllegalArgumentException.class, () -> new ExponentialIndexFunction(0, 10000));
  }

  @Test
  public void ExponentialIndexFunctionTest_testIllegalSizeFails() {
    assertThrows(
        java.lang.IllegalArgumentException.class, () -> new ExponentialIndexFunction(10, -1));
  }

  @Test
  public void ExponentialIndexFunctionTest_testIllegalHashFails() {
    ExponentialIndexFunction exponentialIndexFunction = new ExponentialIndexFunction(1, 10);
    assertThrows(
        java.lang.IllegalArgumentException.class, () -> exponentialIndexFunction.getIndex(-10));
  }
}