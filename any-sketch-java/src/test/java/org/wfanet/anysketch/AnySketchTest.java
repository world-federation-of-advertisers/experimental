// Copyright 2020 The Cross-Media Measurement Authors
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Correspondence;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.anysketch.AnySketch.Register;
import org.wfanet.anysketch.aggregators.Aggregators;
import org.wfanet.anysketch.distributions.Distribution;
import org.wfanet.anysketch.distributions.Distributions;

@SuppressWarnings("UnstableApiUsage") // For toImmutableList.
@RunWith(JUnit4.class)
public class AnySketchTest {

  private static final Correspondence<Register, Register> EQUIVALENCE =
      Correspondence.from(AnySketchTest::registersEquivalent, "is equivalent to");

  private static boolean registersEquivalent(
      @Nullable Register actual, @Nullable Register expected) {
    if (actual == null || expected == null) {
      return actual == expected;
    }
    return actual.getIndex() == expected.getIndex()
        && actual.getValues().containsAll(expected.getValues())
        && expected.getValues().containsAll(actual.getValues());
  }

  private static class FakeDistribution extends Distribution {
    public FakeDistribution() {
      super(0, 999);
    }

    @Override
    public long apply(String item, Map<String, Long> itemMetadata) {
      return (Long.parseLong(item) * 10L) % 1000;
    }
  }

  private static Distribution makeOracleDistribution(String feature) {
    return Distributions.oracle(feature, 5, 10_005);
  }

  private static AnySketch makeSingleIndexAnySketch(Distribution... distributions) {
    return new AnySketch(
        singletonList(new FakeDistribution()),
        Arrays.stream(distributions)
            .map(d -> new ValueFunction("FakeValueFunction", Aggregators.sum(), d))
            .collect(toImmutableList()));
  }

  private void assertAnySketchIs(AnySketch sketch, Register... expectedRegisters) {
    assertThat(sketch)
        .comparingElementsUsing(EQUIVALENCE)
        .containsExactlyElementsIn(expectedRegisters);
  }

  @Test
  public void testEmptyAnySketch() {
    AnySketch anySketch = makeSingleIndexAnySketch(new FakeDistribution());
    assertThat(anySketch.iterator().hasNext()).isFalse();
  }

  @Test
  public void testSingleOracleDistribution() {
    Distribution distribution = makeOracleDistribution("some-feature");
    AnySketch anySketch = makeSingleIndexAnySketch(distribution);

    anySketch.insert(1L, ImmutableMap.of("some-feature", 123L, "some-other-feature", 456L));
    assertAnySketchIs(anySketch, new Register(10L, ImmutableList.of(123L)));

    anySketch.insert(1L, ImmutableMap.of("some-feature", 789L));
    assertAnySketchIs(anySketch, new Register(10L, ImmutableList.of(123L + 789L)));
  }

  @Test
  public void testMultipleOracleDistributions() {
    Distribution distribution1 = makeOracleDistribution("feature1");
    Distribution distribution2 = makeOracleDistribution("feature2");
    AnySketch anySketch = makeSingleIndexAnySketch(distribution1, distribution2);

    anySketch.insert(1L, ImmutableMap.of("feature1", 123L, "feature2", 456L));

    assertAnySketchIs(anySketch, new Register(10L, ImmutableList.of(123L, 456L)));
  }

  @Test
  public void testConstantDistribution() {
    // A distribution that maps everything to 12345.
    Distribution distribution =
        new Distribution(12345L, 12345L) {
          @Override
          public long apply(String item, Map<String, Long> itemMetadata) {
            return 12345L;
          }
        };

    AnySketch anySketch = makeSingleIndexAnySketch(distribution);

    anySketch.insert(1L, ImmutableMap.of());
    anySketch.insert(2L, ImmutableMap.of());
    assertAnySketchIs(
        anySketch,
        new Register(10L, ImmutableList.of(12345L)),
        new Register(20L, ImmutableList.of(12345L)));
  }

  @Test
  public void testUniformDistribution() {
    // The fingerprinter always returns 9999, so this should map every element to 999.
    Distribution distribution = Distributions.uniform(i -> 9999, 0, 999);

    AnySketch anySketch = makeSingleIndexAnySketch(distribution);

    anySketch.insert(1L, ImmutableMap.of());
    assertAnySketchIs(anySketch, new Register(10L, ImmutableList.of(999L)));
  }

  @Test
  public void testMerge() {
    Distribution distribution = makeOracleDistribution("feature1");
    AnySketch sketch1 = makeSingleIndexAnySketch(distribution);
    AnySketch sketch2 = makeSingleIndexAnySketch(distribution);

    sketch1.insert(1L, ImmutableMap.of("feature1", 1000L));
    sketch1.insert(2L, ImmutableMap.of("feature1", 2000L));

    sketch2.insert(1L, ImmutableMap.of("feature1", 500L));
    sketch2.insert(3L, ImmutableMap.of("feature1", 600L));

    sketch1.merge(sketch2);

    assertAnySketchIs(
        sketch1,
        new Register(10L, ImmutableList.of(1500L)),
        new Register(20L, ImmutableList.of(2000L)),
        new Register(30L, ImmutableList.of(600L)));

    assertAnySketchIs(
        sketch2,
        new Register(10L, ImmutableList.of(500L)),
        new Register(30L, ImmutableList.of(600L)));
  }

  @Test
  public void testMultipleIndexes() {
    AnySketch sketch =
        new AnySketch(
            ImmutableList.of(
                makeOracleDistribution("feature1"), makeOracleDistribution("feature2")),
            ImmutableList.of(
                new ValueFunction("FakeValueFunction", Aggregators.sum(), new FakeDistribution())));

    sketch.insert(0L, ImmutableMap.of("feature1", 6L, "feature2", 10L));

    assertAnySketchIs(sketch, new Register(10_005L, ImmutableList.of(0L)));
  }

  @Test
  public void testTooBigIndexes() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AnySketch(
                ImmutableList.of(
                    Distributions.uniform(Long::parseLong, 0, 1L << 33L),
                    Distributions.uniform(Long::parseLong, 0, 1L << 33L)),
                ImmutableList.of(
                    new ValueFunction(
                        "FakeValueFunction", Aggregators.sum(), new FakeDistribution()))));
  }

  @Test
  public void testNoValues() {
    AnySketch sketch = makeSingleIndexAnySketch();
    sketch.insert(1L, null);
    assertAnySketchIs(sketch, new Register(10L, ImmutableList.of()));
  }
}
