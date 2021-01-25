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

package org.wfanet.anysketch.aggregators;

import static com.google.common.truth.Truth.assertThat;
import static org.wfanet.anysketch.aggregators.UniqueAggregator.DESTROYED;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UniqueAggregatorTest {
  private final Aggregator aggregator = new UniqueAggregator();

  @Test
  public void testBasicBehavior() {
    assertThat(aggregator.aggregate(1, 2)).isEqualTo(DESTROYED);
    assertThat(aggregator.aggregate(123, 123)).isEqualTo(123);
    assertThat(aggregator.aggregate(456, 456)).isEqualTo(456);
    assertThat(aggregator.aggregate(456, DESTROYED)).isEqualTo(DESTROYED);
  }
}
