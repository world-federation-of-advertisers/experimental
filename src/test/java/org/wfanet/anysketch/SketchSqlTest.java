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

import com.google.common.io.Resources;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.measurement.api.v1alpha.Distribution;
import org.wfanet.measurement.api.v1alpha.ExponentialDistribution;
import org.wfanet.measurement.api.v1alpha.GeometricDistribution;
import org.wfanet.measurement.api.v1alpha.OracleDistribution;
import org.wfanet.measurement.api.v1alpha.SketchConfig;
import org.wfanet.measurement.api.v1alpha.SketchConfig.IndexSpec;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec.Aggregator;
import org.wfanet.measurement.api.v1alpha.UniformDistribution;

@RunWith(JUnit4.class)
public class SketchSqlTest {

  @Test
  public void liquidLegions() throws IOException {
    SketchConfig config = readSketchConfigTextproto("liquid_legions");

    String sql = SketchSql.fromConfig("Foo", config);

    assertThat(sql)
        .isEqualTo(
            ""
                + "WITH T1 AS (\n"
                + "  SELECT\n"
                + "    (CAST(LOG(EXP(23.000000) +"
                + " BIT_CAST_TO_UINT64(FARM_FINGERPRINT(CONCAT(Index, Impressions.VirtualId))) /"
                + " BIT_CAST_TO_UINT64(-1) * (1 - EXP(23.000000)) / 23.000000 * 330000 AS INT64))"
                + " AS Index,\n"
                + "    (MOD(BIT_CAST_TO_UINT64(FARM_FINGERPRINT(CONCAT(SamplingIndicator,"
                + " Impressions.VirtualId))), 10000000)) AS SamplingIndicator,\n"
                + "    (Impressions.frequency) AS Frequency,\n"
                + "  FROM Foo AS Impressions\n"
                + ")\n"
                + "SELECT\n"
                + "  T1.Index,\n"
                + "  (IF(MIN(T1.SamplingIndicator) = MAX(T1.SamplingIndicator),"
                + " ANY_VALUE(T1.SamplingIndicator), -1)) AS SamplingIndicator,\n"
                + "  (SUM(T1.Frequency)) AS Frequency,\n"
                + "FROM T1\n"
                + "GROUP BY 1");
  }

  @Test
  public void everything() throws IOException {
    SketchConfig config =
        SketchConfig.newBuilder()
            .addIndexes(
                IndexSpec.newBuilder()
                    .setName("Index1")
                    .setDistribution(
                        Distribution.newBuilder()
                            .setUniform(UniformDistribution.newBuilder().setNumValues(123))))
            .addIndexes(
                IndexSpec.newBuilder()
                    .setName("Index2")
                    .setDistribution(
                        Distribution.newBuilder()
                            .setGeometric(GeometricDistribution.newBuilder().setNumValues(64))))
            .addValues(
                ValueSpec.newBuilder()
                    .setName("Value1")
                    .setDistribution(
                        Distribution.newBuilder()
                            .setOracle(OracleDistribution.newBuilder().setKey("OracleKey")))
                    .setAggregator(Aggregator.SUM))
            .addValues(
                ValueSpec.newBuilder()
                    .setName("Value2")
                    .setDistribution(
                        Distribution.newBuilder()
                            .setExponential(
                                ExponentialDistribution.newBuilder()
                                    .setRate(45.6)
                                    .setNumValues(789)))
                    .setAggregator(Aggregator.UNIQUE))
            .build();

    String sql = SketchSql.fromConfig("OriginalTable", config);

    assertThat(sql)
        .isEqualTo(
            ""
                + "WITH T1 AS (\n"
                + "  SELECT\n"
                + "    (MOD(BIT_CAST_TO_UINT64(FARM_FINGERPRINT(CONCAT(Index1,"
                + " Impressions.VirtualId))), 123)) AS Index1,\n"
                + "    (CAST(FLOOR(LOG(BIT_CAST_TO_UINT64(FARM_FINGERPRINT(CONCAT(Index2,"
                + " Impressions.VirtualId))) / BIT_CAST_TO_UINT64(-1), 2)) AS INT64)) AS Index2,\n"
                + "    (Impressions.OracleKey) AS Value1,\n"
                + "    (CAST(LOG(EXP(45.600000) +"
                + " BIT_CAST_TO_UINT64(FARM_FINGERPRINT(CONCAT(Value2, Impressions.VirtualId))) /"
                + " BIT_CAST_TO_UINT64(-1) * (1 - EXP(45.600000)) / 45.600000 * 789 AS INT64)) AS"
                + " Value2,\n"
                + "  FROM OriginalTable AS Impressions\n"
                + ")\n"
                + "SELECT\n"
                + "  T1.Index1,\n"
                + "  T1.Index2,\n"
                + "  (SUM(T1.Value1)) AS Value1,\n"
                + "  (IF(MIN(T1.Value2) = MAX(T1.Value2), ANY_VALUE(T1.Value2), -1)) AS Value2,\n"
                + "FROM T1\n"
                + "GROUP BY 1, 2");
  }

  private static SketchConfig readSketchConfigTextproto(String name) throws IOException {
    String configPath = "testdata/" + name + ".textpb";
    System.err.println(configPath);
    System.err.println(SketchSqlTest.class.getResource(configPath));
    @SuppressWarnings("UnstableApiUsage")
    String textproto = Resources.toString(SketchSqlTest.class.getResource(configPath), UTF_8);
    return TextFormat.parse(textproto, SketchConfig.class);
  }
}
