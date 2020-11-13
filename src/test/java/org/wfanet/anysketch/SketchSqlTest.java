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
import static org.junit.Assert.assertThrows;

import com.google.common.io.Resources;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.measurement.api.v1alpha.SketchConfig;

@RunWith(JUnit4.class)
public class SketchSqlTest {

  @Test
  public void liquidLegions() throws IOException {
    SketchConfig config = readSketchConfigTextproto("liquid_legions");

    String sql = SketchSql.forBigQuery("Foo", config);

    String expected = getTestResource("liquid_legions.sql");
    assertThat(sql).isEqualTo(expected);
  }

  @Test
  public void geometricDistribution() throws IOException {
    SketchConfig config = readSketchConfigTextproto("geometric_distribution");

    String sql = SketchSql.forBigQuery("Foo", config);

    String expected = getTestResource("geometric_distribution.sql");
    assertThat(sql).isEqualTo(expected);
  }

  @Test
  public void diracDistributionIsUnimplemented() throws IOException {
    SketchConfig config = readSketchConfigTextproto("dirac_distribution");

    assertThrows(IllegalArgumentException.class, () -> SketchSql.forBigQuery("Foo", config));
  }

  @Test
  public void verbatimDistributionIsUnimplemented() throws IOException {
    SketchConfig config = readSketchConfigTextproto("verbatim_distribution");

    assertThrows(IllegalArgumentException.class, () -> SketchSql.forBigQuery("Foo", config));
  }

  private static SketchConfig readSketchConfigTextproto(String name) throws IOException {
    return TextFormat.parse(getTestResource(name + ".textpb"), SketchConfig.class);
  }

  @SuppressWarnings("UnstableApiUsage")
  private static String getTestResource(String filename) throws IOException {
    String configPath = Paths.get("testdata", filename).toString();
    return Resources.toString(SketchSqlTest.class.getResource(configPath), UTF_8);
  }
}
