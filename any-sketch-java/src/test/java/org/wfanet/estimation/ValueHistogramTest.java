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

package org.wfanet.estimation;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.anysketch.AnySketch;
import org.wfanet.anysketch.SketchProtos;
import org.wfanet.measurement.api.v1alpha.SketchConfig;

@RunWith(JUnit4.class)
public class ValueHistogramTest {
  private static final String VAL_FREQ = "Frequency";
  private static final String DIST_FREQ = "frequency";

  private static SketchConfig readSketchConfigTextproto(String name) throws IOException {
    String configPath = "/org/wfanet/anysketch/testdata/" + name + ".textpb";
    System.err.println(configPath);
    System.err.println(ValueHistogramTest.class.getResource(configPath));
    String textproto = Resources.toString(ValueHistogramTest.class.getResource(configPath), UTF_8);
    return TextFormat.parse(textproto, SketchConfig.class);
  }

  @Test
  public void testThrowsNoFreqValueFunction() throws IOException {
    SketchConfig config = readSketchConfigTextproto("liquid_legions");
    AnySketch anySketch = SketchProtos.toAnySketch(config);
    anySketch.insert("person one", ImmutableMap.of(DIST_FREQ, 1L));

    assertThrows(
        NoSuchElementException.class,
        () -> ValueHistogram.calculateHistogram(anySketch, "wrong_value", r -> true));
  }

  @Test
  public void testCorrectHistogram() throws IOException {
    SketchConfig config = readSketchConfigTextproto("liquid_legions");
    AnySketch anySketch = SketchProtos.toAnySketch(config);
    anySketch.insert("person one", ImmutableMap.of(DIST_FREQ, 1L));
    anySketch.insert("person one", ImmutableMap.of(DIST_FREQ, 2L));
    anySketch.insert("personne trois", ImmutableMap.of(DIST_FREQ, 3L));
    anySketch.insert("qof afar", ImmutableMap.of(DIST_FREQ, 4L));

    ImmutableMap<Long, Double> result =
        ValueHistogram.calculateHistogram(anySketch, VAL_FREQ, r -> true);
    assertThat(result).hasSize(2);
    assertEquals(0.667, result.get(3L), 0.01);
    assertEquals(0.333, result.get(4L), 0.01);
  }

  @Test
  public void testCorrectHistogramWithDestroyedRegisters() throws IOException {
    SketchConfig config = readSketchConfigTextproto("liquid_legions");
    AnySketch anySketch = SketchProtos.toAnySketch(config);
    anySketch.insert("destroyed one", ImmutableMap.of(DIST_FREQ, 1L));
    anySketch.insert("destroyed two", ImmutableMap.of(DIST_FREQ, 3L));

    assertThat(ValueHistogram.calculateHistogram(anySketch, VAL_FREQ, r -> false))
        .isEqualTo(ImmutableMap.of());
  }
}
