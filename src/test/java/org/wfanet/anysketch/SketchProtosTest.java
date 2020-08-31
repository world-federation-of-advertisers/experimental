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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.anysketch.aggregators.Aggregators;
import org.wfanet.anysketch.distributions.Distributions;
import org.wfanet.anysketch.fingerprinters.FarmFingerprinter;
import org.wfanet.anysketch.fingerprinters.SaltedFingerprinter;
import org.wfanet.measurement.api.v1alpha.Sketch;
import org.wfanet.measurement.api.v1alpha.Sketch.Register;
import org.wfanet.measurement.api.v1alpha.SketchConfig;

@RunWith(JUnit4.class)
public class SketchProtosTest {

  private static final SketchConfig DEFAULT_CONFIG = SketchConfig.getDefaultInstance();

  private static class Mod1000Distribution extends org.wfanet.anysketch.distributions.Distribution {
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
              new ValueFunction(
                  "value-function-1", Aggregators.sum(), Distributions.oracle("feature1", 5, 105)),
              new ValueFunction(
                  "value-function-2", Aggregators.unique(), Distributions.oracle("feature2", 6, 15))));

  @Test
  public void fromAnySketch_invalidArgumentsFails() {
    assertThrows(NullPointerException.class, () -> SketchProtos.fromAnySketch(null, null));
    assertThrows(
        NullPointerException.class, () -> SketchProtos.fromAnySketch(null, DEFAULT_CONFIG));
  }

  @Test
  public void fromAnySketch_emptySketch() {
    Sketch sketch = SketchProtos.fromAnySketch(anySketch, DEFAULT_CONFIG);
    assertThat(sketch).isEqualTo(Sketch.newBuilder().setConfig(DEFAULT_CONFIG).build());
  }

  @Test
  public void fromAnySketch_sketchWithMultipleRegistersSucceeds() {
    anySketch.insert(1L, ImmutableMap.of("feature1", 10L, "feature2", 6L));
    anySketch.insert(2L, ImmutableMap.of("feature1", 11L, "feature2", 7L));
    anySketch.insert(3L, ImmutableMap.of("feature1", 12L, "feature2", 8L));
    anySketch.insert(4L, ImmutableMap.of("feature1", 13L, "feature2", 9L));

    Sketch sketch = SketchProtos.fromAnySketch(anySketch, DEFAULT_CONFIG);

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

  @Test
  public void roundTrip_liquidLegions() throws IOException {
    SketchConfig config = readSketchConfigTextproto("liquid_legions");

    Consumer<AnySketch> insertSomeItems =
        (anySketch) -> {
          anySketch.insert("person one", ImmutableMap.of("frequency", 1L));
          anySketch.insert("persona dos", ImmutableMap.of("frequency", 2L));
          anySketch.insert("personne trois", ImmutableMap.of("frequency", 3L));
          anySketch.insert("qof afar", ImmutableMap.of("frequency", 4L));
        };

    AnySketch anySketch1 = SketchProtos.toAnySketch(config);
    insertSomeItems.accept(anySketch1);

    Sketch sketch1 = SketchProtos.fromAnySketch(anySketch1, config);
    AnySketch anySketch2 = SketchProtos.toAnySketch(config, sketch1);
    Sketch sketch2 = SketchProtos.fromAnySketch(anySketch2, config);

    assertThat(sketch1)
        .ignoringRepeatedFieldOrderOfFields(Sketch.REGISTERS_FIELD_NUMBER)
        .isEqualTo(sketch2);

    AnySketch anySketch3 =
        new AnySketch(
            ImmutableList.of(
                Distributions.exponential(
                    new SaltedFingerprinter("Index", new FarmFingerprinter()), 23.0, 330_000L)),
            ImmutableList.of(
                new ValueFunction(
                    "SamplingIndicator",
                    Aggregators.unique(),
                    Distributions.uniform(
                        new SaltedFingerprinter("SamplingIndicator", new FarmFingerprinter()),
                        0L,
                        9_999_999L)),
                new ValueFunction(
                    "Frequency",
                    Aggregators.sum(),
                    Distributions.oracle("frequency", Long.MIN_VALUE, Long.MAX_VALUE))));
    insertSomeItems.accept(anySketch3);

    Sketch sketch3 = SketchProtos.fromAnySketch(anySketch3, config);
    assertThat(sketch3)
        .ignoringRepeatedFieldOrderOfFields(Sketch.REGISTERS_FIELD_NUMBER)
        .isEqualTo(sketch1);
  }

  private static Register makeRegister(long index, Long... values) {
    return Register.newBuilder().setIndex(index).addAllValues(Arrays.asList(values)).build();
  }

  private static SketchConfig readSketchConfigTextproto(String name) throws IOException {
    String configPath = "testdata/" + name + ".textpb";
    System.err.println(configPath);
    System.err.println(SketchProtosTest.class.getResource(configPath));
    @SuppressWarnings("UnstableApiUsage")
    String textproto = Resources.toString(SketchProtosTest.class.getResource(configPath), UTF_8);
    return TextFormat.parse(textproto, SketchConfig.class);
  }
}
