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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OracleDistributionTest {
  private static final String FEATURE_NAME = "some-feature-name";
  private static final String OTHER_FEATURE_NAME = "other-feature-name";

  Distribution distribution = new OracleDistribution(FEATURE_NAME, 1, 10);

  @Test
  public void testSuccess() {
    assertThat(distribution.apply("irrelevant", ImmutableMap.of(FEATURE_NAME, 5L))).isEqualTo(5L);
  }

  @Test
  public void testExtraMetadata() {
    Map<String, Long> metadata = ImmutableMap.of(FEATURE_NAME, 5L, OTHER_FEATURE_NAME, 6L);
    assertThat(distribution.apply("irrelevant", metadata)).isEqualTo(5L);
  }

  @Test
  public void testMissingMetadata() {
    Map<String, Long> metadata = ImmutableMap.of(OTHER_FEATURE_NAME, 6L);
    assertThrows(IllegalArgumentException.class, () -> distribution.apply("irrelevant", metadata));
  }
}
