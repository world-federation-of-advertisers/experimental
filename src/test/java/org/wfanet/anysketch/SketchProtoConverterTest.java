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

import com.google.common.primitives.UnsignedLong;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import wfa.measurement.api.v1alpha.SketchOuterClass.Sketch;
import wfa.measurement.api.v1alpha.SketchOuterClass.Sketch.Register;
import wfa.measurement.api.v1alpha.SketchOuterClass.SketchConfig;

@RunWith(JUnit4.class)
public class SketchProtoConverterTest {

  private static final SketchConfig DEFAULT_CONFIG = SketchConfig.getDefaultInstance();

  class FakeIndexFunction implements IndexFunction {

    @Override
    public UnsignedLong getIndex(UnsignedLong fingerprint) {
      return fingerprint.times(UnsignedLong.valueOf(2L));
    }

    @Override
    public UnsignedLong maxIndex() {
      return UnsignedLong.valueOf(20);
    }

    @Override
    public UnsignedLong maxSupportedHash() {
      return UnsignedLong.valueOf(10);
    }
  }

  class FakeHashFunction implements HashFunction {

    @Override
    public UnsignedLong fingerprint(String item) {
      return UnsignedLong.valueOf(item);
    }
  }

  @Test
  public void SketchProtoConverter_testInvalidArgumentsFails() {
    assertThrows(NullPointerException.class, () -> SketchProtoConverter.convert(null, null));
    assertThrows(
        NullPointerException.class, () -> SketchProtoConverter.convert(null, DEFAULT_CONFIG));
  }

  @Test
  public void SketchProtoConverter_testBasicBehaviorSucceeds() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            singletonList(new UniqueValueFunction()),
            new FakeHashFunction());
    Sketch sketch = SketchProtoConverter.convert(anySketch, DEFAULT_CONFIG);
    assertThat(sketch).isEqualTo(Sketch.newBuilder().setConfig(DEFAULT_CONFIG).build());
  }

  @Test
  public void SketchProtoConverter_testSketchWithMultipleRegistersSucceeds() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            singletonList(new UniqueValueFunction()),
            new FakeHashFunction());
    anySketch.insert(1L, singletonList(812L));
    anySketch.insert(2L, singletonList(321L));
    anySketch.insert(3L, singletonList(5L));
    anySketch.insert(4L, singletonList(96L));
    Sketch sketch = SketchProtoConverter.convert(anySketch, DEFAULT_CONFIG);

    // We expect the values to be incremented by 1, so that "-1" is represented as "0" in protos.
    Sketch expected =
        Sketch.newBuilder()
            .setConfig(DEFAULT_CONFIG)
            .addAllRegisters(
                Arrays.asList(
                    Register.newBuilder().setIndex(2).addValues(813L).build(),
                    Register.newBuilder().setIndex(4).addValues(322L).build(),
                    Register.newBuilder().setIndex(6).addValues(6L).build(),
                    Register.newBuilder().setIndex(8).addValues(97L).build()))
            .build();
    assertThat(sketch).isEqualTo(expected);
  }
}
