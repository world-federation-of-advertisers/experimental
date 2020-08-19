// Copyright 2020 The Any Sketch Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wfanet.anysketch.distributions;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

@RunWith(JUnit4.class)
public class ExponentialDistributionTest {
  private static final Map<String, Long> USER_PROVIDED_VALUES = ImmutableMap.of();

  private static class FakeFingerprinter implements Fingerprinter {
    long fingerprintToReturn = 0;

    @Override
    public long fingerprint(String item) {
      return fingerprintToReturn;
    }
  }

  private final FakeFingerprinter fingerprinter = new FakeFingerprinter();

  private ExponentialDistribution makeDistribution(double rate, long size) {
    return new ExponentialDistribution("Foo", fingerprinter, rate, size);
  }

  @Test
  public void testRange() {
    ExponentialDistribution exponentialDistribution = makeDistribution(10.0, 10);
    assertThat(exponentialDistribution.getMinValue()).isEqualTo(0);
    assertThat(exponentialDistribution.getMaxValue()).isEqualTo(9);
  }

  @Test
  public void testRate1() {
    ExponentialDistribution exponentialDistribution = makeDistribution(1.0, 10);
    fingerprinter.fingerprintToReturn = 50;
    assertThat(exponentialDistribution.apply("abc", USER_PROVIDED_VALUES)).isEqualTo(0);
  }

  @Test
  public void testRate2() {
    ExponentialDistribution exponentialDistribution = makeDistribution(2.0, 10000);
    fingerprinter.fingerprintToReturn = (long) 5e18;
    // u = hashInput / EXPECTED_MAX_HASH = 5e18 / 0xFFFFFFFFFFFFFFFF = 0.2710505431213761
    // x = 1 - log(e^2 + u * (1 - e^2)) / 2 = 0.1335267174441641
    // index = floor(x * 10000) = 1335
    assertThat(exponentialDistribution.apply("abc", USER_PROVIDED_VALUES)).isEqualTo(1335);
  }

  @Test
  public void testIllegalRateFails() {
    assertThrows(IllegalArgumentException.class, () -> makeDistribution(0, 10000));
  }
}
