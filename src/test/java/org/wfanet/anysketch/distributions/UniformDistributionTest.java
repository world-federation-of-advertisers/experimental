// Copyright 2020 The AnySketch Authors
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UniformDistributionTest {

  @Test
  public void testPositiveFingerprint() {
    UniformDistribution distribution = new UniformDistribution(i -> 7, 1, 5);
    // 7 % 5 + 1 = 2 + 1 = 3
    assertThat(distribution.apply("DoesNotMatter", null)).isEqualTo(3);
  }

  @Test
  public void testNegativeFingerprint() {
    UniformDistribution distribution = new UniformDistribution(i -> -7, 1, 17);
    // -7 signed is bit-equivalent to 2^64-7 unsigned, which is congruent to 11 mod 17.
    assertThat(distribution.apply("DoesNotMatter", null)).isEqualTo(12);
  }

  @Test
  public void testRangeIsClosed() {
    UniformDistribution distribution = new UniformDistribution(i -> 12345, 8, 8);
    assertThat(distribution.apply("DoesNotMatter", null)).isEqualTo(8);
  }
}
