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

import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import java.util.List;
import org.wfanet.anysketch.AnySketch.Register;
import wfa.measurement.api.v1alpha.SketchOuterClass.Sketch;
import wfa.measurement.api.v1alpha.SketchOuterClass.SketchConfig;

/** SketchProtoConverter class converts {@link AnySketch} object into {@link Sketch} proto. */
public class SketchProtoConverter {

  /**
   * Returns {@link Sketch} proto built from AnySketch Java object and SketchConfig proto.
   *
   * @param anySketch {@link AnySketch} Java object holding all the registers.
   * @param config {@link SketchConfig} proto holding the specifications for computation.
   * @return {@link Sketch} proto
   */
  public static Sketch convert(AnySketch anySketch, SketchConfig config) {
    Preconditions.checkNotNull(anySketch);
    Preconditions.checkNotNull(config);
    Sketch.Builder builder = Sketch.newBuilder();
    builder.setConfig(config);
    for (Register register : anySketch) {
      builder
          .addRegistersBuilder()
          .setIndex(register.getIndex())
          .addAllValues(encodeValues(anySketch.getValueFunctions(), register.getValues()))
          .build();
    }
    return builder.build();
  }

  private static Iterable<Long> encodeValues(
      List<ValueFunction> valueFunctions, List<Long> values) {
    return Streams.zip(
            valueFunctions.stream(),
            values.stream(),
            (valueFn, value) -> valueFn.encodeToProtoValue(value))
        .collect(toList());
  }
}
