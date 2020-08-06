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

package org.wfanet.anysketch;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExponentialIndexFunctionTest {

  private static final UnsignedLong EXPECTED_MAX_HASH =
      UnsignedLong.fromLongBits(0xFFFF_FFFF_FFFF_FFFFL);  // 2^64 - 1

  @Test
  public void ExponentialIndexFunctionTest_testBasicBehavior() {
    ExponentialIndexFunction exponentialIndexFunction =
        new ExponentialIndexFunction(10, UnsignedLong.valueOf(10));
    assertThat(exponentialIndexFunction.maxIndex().intValue()).isEqualTo(9);
    assertThat(exponentialIndexFunction.maxSupportedHash()).isEqualTo(EXPECTED_MAX_HASH);
  }

  @Test
  public void ExponentialIndexFunctionTest_testGetIndexRateOneSucceeds() {
    // Maps everything to the first register.
    ExponentialIndexFunction exponentialIndexFunction =
        new ExponentialIndexFunction(1, UnsignedLong.valueOf(10));
    UnsignedLong hashInput = UnsignedLong.valueOf(50);
    assertThat(exponentialIndexFunction.maxIndex().intValue()).isEqualTo(9);
    assertThat(exponentialIndexFunction.maxSupportedHash()).isEqualTo(EXPECTED_MAX_HASH);
    assertThat(exponentialIndexFunction.getIndex(hashInput).intValue()).isEqualTo(0);
  }

  @Test
  public void ExponentialIndexFunctionTest_testGetIndexSucceeds() {
    ExponentialIndexFunction exponentialIndexFunction =
        new ExponentialIndexFunction(2, UnsignedLong.valueOf(10000));
    UnsignedLong hashInput = UnsignedLong.valueOf(5L * (long) Math.pow(10, 18));
    // u = hashInput / EXPECTED_MAX_HASH = 5e18 / 0xFFFFFFFFFFFFFFFF = 0.2710505431213761
    // x = 1 - log(e^2 + u * (1 - e^2)) / 2 = 0.1335267174441641
    // index = floor(x * 10000) = 1335
    assertThat(exponentialIndexFunction.maxIndex().intValue()).isEqualTo(9999);
    assertThat(exponentialIndexFunction.maxSupportedHash()).isEqualTo(EXPECTED_MAX_HASH);
    assertThat(exponentialIndexFunction.getIndex(hashInput).intValue()).isEqualTo(1335);
  }

  @Test
  public void ExponentialIndexFunctionTest_testIllegalRateFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ExponentialIndexFunction(0, UnsignedLong.valueOf(10000)));
  }

  // TODO: add a test that tries passing in a too-large fingerprint.
}
