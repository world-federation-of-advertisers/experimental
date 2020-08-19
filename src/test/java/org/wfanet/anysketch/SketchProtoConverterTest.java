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
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.anysketch.aggregators.SumAggregator;
import org.wfanet.anysketch.aggregators.UniqueAggregator;
import org.wfanet.anysketch.distributions.Distribution;
import org.wfanet.anysketch.distributions.OracleDistribution;
import org.wfanet.measurement.api.v1alpha.Sketch;
import org.wfanet.measurement.api.v1alpha.Sketch.Register;
import org.wfanet.measurement.api.v1alpha.SketchConfig;

@RunWith(JUnit4.class)
public class SketchProtoConverterTest {

  private static final SketchConfig DEFAULT_CONFIG = SketchConfig.getDefaultInstance();

  private static class Mod1000Distribution extends Distribution {
    public Mod1000Distribution() {
      super(0, 999);
    }

    @Override
    public long apply(String item, Map<String, Long> itemMetadata) {
      return Long.parseLong(item) % 1000;
    }
  }

  private final AnySketch anySketch =
      new AnySketch(
          singletonList(new Mod1000Distribution()),
          ImmutableList.of(
              new ValueFunction(new SumAggregator(), new OracleDistribution("feature1", 5, 105)),
              new ValueFunction(
                  new UniqueAggregator(), new OracleDistribution("feature2", 6, 15))));

  @Test
  public void testInvalidArgumentsFails() {
    assertThrows(NullPointerException.class, () -> SketchProtoConverter.convert(null, null));
    assertThrows(
        NullPointerException.class, () -> SketchProtoConverter.convert(null, DEFAULT_CONFIG));
  }

  @Test
  public void testEmptySketch() {
    Sketch sketch = SketchProtoConverter.convert(anySketch, DEFAULT_CONFIG);
    assertThat(sketch).isEqualTo(Sketch.newBuilder().setConfig(DEFAULT_CONFIG).build());
  }

  @Test
  public void testSketchWithMultipleRegistersSucceeds() {
    anySketch.insert(1L, ImmutableMap.of("feature1", 10L, "feature2", 6L));
    anySketch.insert(2L, ImmutableMap.of("feature1", 11L, "feature2", 7L));
    anySketch.insert(3L, ImmutableMap.of("feature1", 12L, "feature2", 8L));
    anySketch.insert(4L, ImmutableMap.of("feature1", 13L, "feature2", 9L));

    Sketch sketch = SketchProtoConverter.convert(anySketch, DEFAULT_CONFIG);

    // We expect feature2 values to be incremented by 1, so that "-1" is represented as "0" in
    // protos.
    Sketch expected =
        Sketch.newBuilder()
            .setConfig(DEFAULT_CONFIG)
            .addAllRegisters(
                Arrays.asList(
                    makeRegister(1L, 10L, 7L),
                    makeRegister(2L, 11L, 8L),
                    makeRegister(3L, 12L, 9L),
                    makeRegister(4L, 13L, 10L)))
            .build();
    assertThat(sketch).isEqualTo(expected);
  }

  private static Register makeRegister(long index, Long... values) {
    return Register.newBuilder().setIndex(index).addAllValues(Arrays.asList(values)).build();
  }
}
