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
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import java.util.List;
import org.wfanet.anysketch.AnySketch.Register;
import org.wfanet.anysketch.aggregators.Aggregator;
import org.wfanet.anysketch.aggregators.Aggregators;
import org.wfanet.anysketch.distributions.Distributions;
import org.wfanet.anysketch.fingerprinters.FarmFingerprinter;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;
import org.wfanet.anysketch.fingerprinters.SaltedFingerprinter;
import org.wfanet.measurement.api.v1alpha.Distribution;
import org.wfanet.measurement.api.v1alpha.ExponentialDistribution;
import org.wfanet.measurement.api.v1alpha.GeometricDistribution;
import org.wfanet.measurement.api.v1alpha.OracleDistribution;
import org.wfanet.measurement.api.v1alpha.Sketch;
import org.wfanet.measurement.api.v1alpha.SketchConfig;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec;
import org.wfanet.measurement.api.v1alpha.UniformDistribution;

/** Utilities for converting between {@link AnySketch} object and {@link Sketch} protos. */
@SuppressWarnings("UnstableApiUsage") // For toImmutableList and Streams.zip
public class SketchProtos {

  /**
   * Returns {@link Sketch} proto built from AnySketch Java object and SketchConfig proto.
   *
   * @param anySketch {@link AnySketch} Java object holding all the registers.
   * @param config {@link SketchConfig} proto holding the specifications for computation.
   * @return {@link Sketch} proto
   */
  public static Sketch fromAnySketch(AnySketch anySketch, SketchConfig config) {
    Preconditions.checkNotNull(anySketch);
    Preconditions.checkNotNull(config);
    Sketch.Builder builder = Sketch.newBuilder();
    builder.setConfig(config);
    for (Register register : anySketch) {
      builder
          .addRegistersBuilder()
          .setIndex(register.getIndex())
          .addAllValues(encodeValuesToProto(anySketch.getValueFunctions(), register.getValues()))
          .build();
    }
    return builder.build();
  }

  public static AnySketch toAnySketch(SketchConfig config, Sketch sketch) {
    AnySketch anySketch = toAnySketch(config);
    List<ValueFunction> valueFunctions = anySketch.getValueFunctions();
    for (Sketch.Register register : sketch.getRegistersList()) {
      List<Long> values = decodeValuesFromProto(valueFunctions, register.getValuesList());
      anySketch.aggregateIntoRegister(register.getIndex(), values);
    }
    return anySketch;
  }

  public static AnySketch toAnySketch(SketchConfig config) {
    List<org.wfanet.anysketch.distributions.Distribution> indexDistributions =
        config.getIndexesList().stream()
            .map(i -> makeDistribution(i.getName(), i.getDistribution()))
            .collect(toImmutableList());

    List<ValueFunction> valueFunctions =
        config.getValuesList().stream()
            .map(SketchProtos::makeValueFunction)
            .collect(toImmutableList());

    return new AnySketch(indexDistributions, valueFunctions);
  }

  private static Iterable<Long> encodeValuesToProto(
      List<ValueFunction> valueFunctions, List<Long> values) {
    return Streams.zip(
            valueFunctions.stream().map(ValueFunction::getAggregator),
            values.stream(),
            Aggregator::encodeToProtoValue)
        .collect(toList());
  }

  private static List<Long> decodeValuesFromProto(
      List<ValueFunction> valueFunctions, List<Long> values) {
    return Streams.zip(
            valueFunctions.stream().map(ValueFunction::getAggregator),
            values.stream(),
            Aggregator::decodeFromProtoValue)
        .collect(toList());
  }

  private static ValueFunction makeValueFunction(ValueSpec valueSpec) {
    return new ValueFunction(
        valueSpec.getName(),
        makeAggregator(valueSpec.getAggregator()),
        makeDistribution(valueSpec.getName(), valueSpec.getDistribution()));
  }

  private static Aggregator makeAggregator(ValueSpec.Aggregator aggregator) {
    switch (aggregator) {
      case SUM:
        return Aggregators.sum();
      case UNIQUE:
        return Aggregators.unique();
      case AGGREGATOR_UNSPECIFIED:
      case UNRECOGNIZED:
        break;
    }
    throw new IllegalArgumentException(
        String.format("Unsupported aggregator type '%s'", aggregator));
  }

  private static Fingerprinter makeFingerprinter(String name) {
    return new SaltedFingerprinter(name, new FarmFingerprinter());
  }

  private static org.wfanet.anysketch.distributions.Distribution makeDistribution(
      String name, Distribution distribution) {
    switch (distribution.getDistributionChoiceCase()) {
      case EXPONENTIAL:
        ExponentialDistribution exponential = distribution.getExponential();
        return Distributions.exponential(
            makeFingerprinter(name), exponential.getRate(), exponential.getNumValues());
      case UNIFORM:
        UniformDistribution uniform = distribution.getUniform();
        return Distributions.uniform(makeFingerprinter(name), 0L, uniform.getNumValues() - 1L);
      case ORACLE:
        // TODO: enrich proto to support min and max values
        OracleDistribution oracle = distribution.getOracle();
        return Distributions.oracle(oracle.getKey(), Long.MIN_VALUE, Long.MAX_VALUE);
      case GEOMETRIC:
        GeometricDistribution geometric = distribution.getGeometric();
        return Distributions.geometric(makeFingerprinter(name), 0, geometric.getNumValues() - 1);
      case CONSTANT:
      case DIRAC_MIXTURE:
      case VERBATIM:
      case DISTRIBUTIONCHOICE_NOT_SET:
        break;
    }
    throw new IllegalArgumentException(
        String.format("Unsupported distribution with name '%s': %s", name, distribution));
  }
}
