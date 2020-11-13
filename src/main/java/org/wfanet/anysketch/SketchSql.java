// Copyright 2020 The AnySketch Authors
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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.wfanet.measurement.api.v1alpha.Distribution;
import org.wfanet.measurement.api.v1alpha.Distribution.DistributionChoiceCase;
import org.wfanet.measurement.api.v1alpha.ExponentialDistribution;
import org.wfanet.measurement.api.v1alpha.SketchConfig;
import org.wfanet.measurement.api.v1alpha.SketchConfig.IndexSpec;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec.Aggregator;

/** Converts a SketchConfig into an SQL query. */
public class SketchSql {
  private SketchSql() {}

  @SuppressWarnings("UnstableApiUsage")
  private static final Supplier<String> SQL_TEMPLATE =
      Suppliers.memoize(
          () -> {
            URL sqlTemplateResource = SketchSql.class.getResource("sql/bigquery_template.sql");
            Preconditions.checkNotNull(sqlTemplateResource);
            try {
              return Resources.toString(sqlTemplateResource, Charsets.UTF_8);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });

  /**
   * Converts `config` into a BigQuery-compatible SQL query.
   *
   * <p>The output query will select one row per register of the sketch. The columns are the indexes
   * (in order listed in the config) followed by the values (in order listed in the config). The
   * column names are given by the IndexSpec.name and ValueSpec.name fields.
   *
   * <p>The impressionTable must have a column called "VirtualId", as well as a column for each
   * metadata key referenced by any distribution.
   *
   * @param impressionTable the table or subquery containing exactly the impressions to sketch
   * @param config the SketchConfig
   * @return SQL to produce a sketch
   */
  public static String forBigQuery(String impressionTable, SketchConfig config) {
    String fingerprintSelect = selectFingerprintsSql(config);
    String distributionSelect = selectDistributionSql(config);
    String aggregationSelect = selectAggregations(config);

    String groupByColumns =
        IntStream.range(1, config.getIndexesCount() + 1)
            .mapToObj(Integer::toString)
            .collect(Collectors.joining(", "));

    return SQL_TEMPLATE
        .get()
        .replace("$FINGERPRINT_SELECT", fingerprintSelect.trim())
        .replace("$ORIGINAL_TABLE", impressionTable.trim())
        .replace("$DISTRIBUTION_SELECT", distributionSelect.trim())
        .replace("$AGGREGATION_SELECT", aggregationSelect.trim())
        .replace("$GROUP_BY_COLUMNS", groupByColumns.trim());
  }

  private static String selectOnDistributions(
      SketchConfig config, BiFunction<String, Distribution, String> distributionMapper) {
    StringBuilder sql = new StringBuilder();

    for (IndexSpec indexSpec : config.getIndexesList()) {
      sql.append(distributionMapper.apply(indexSpec.getName(), indexSpec.getDistribution()));
    }

    for (ValueSpec i : config.getValuesList()) {
      sql.append(distributionMapper.apply(i.getName(), i.getDistribution()));
    }

    return sql.toString();
  }

  private static String selectFingerprintsSql(SketchConfig config) {
    return selectOnDistributions(
        config, (n, d) -> selectAs(selectSingleFingerprintSql(n, d), n, /* spaces = */ 4));
  }

  private static String selectDistributionSql(SketchConfig config) {
    return selectOnDistributions(
        config, (n, d) -> selectAs(selectSingleDistributionSql(n, d), n, /* spaces = */ 4));
  }

  private static String selectSingleFingerprintSql(String name, Distribution distribution) {
    if (distribution.getDistributionChoiceCase() == DistributionChoiceCase.ORACLE) {
      return "Impressions." + distribution.getOracle().getKey();
    }
    String template = "FINGERPRINT(\"%s\", Impressions.VirtualId)";
    return String.format(template, name);
  }

  private static String selectSingleDistributionSql(String name, Distribution distribution) {
    switch (distribution.getDistributionChoiceCase()) {
      case EXPONENTIAL:
        {
          ExponentialDistribution e = distribution.getExponential();
          double rate = e.getRate();
          long size = e.getNumValues();
          String template = "EXPONENTIAL_DISTRIBUTION(%s, %f, %d)";
          return String.format(template, name, rate, size);
        }
      case UNIFORM:
        {
          String template = "UNIFORM_DISTRIBUTION(%s, %s)";
          return String.format(template, name, distribution.getUniform().getNumValues());
        }
      case ORACLE:
        return name;
      case GEOMETRIC:
        return String.format("GEOMETRIC_DISTRIBUTION(%s)", name);
      case CONSTANT:
        return String.valueOf(distribution.getConstant().getValue());
      case DIRAC_MIXTURE:
      case VERBATIM:
      case DISTRIBUTIONCHOICE_NOT_SET:
        break;
    }
    throw new IllegalArgumentException("Unsupported distribution: " + distribution);
  }

  private static String selectAggregations(SketchConfig config) {
    StringBuilder sql = new StringBuilder();

    for (IndexSpec i : config.getIndexesList()) {
      sql.append(String.format("  T2.%s,\n", i.getName()));
    }

    for (ValueSpec value : config.getValuesList()) {
      sql.append(selectSingleAggregationSql(value.getName(), value.getAggregator()));
    }

    return sql.toString();
  }

  private static String selectSingleAggregationSql(String name, Aggregator aggregator) {
    return selectAs(aggregateSql(name, aggregator), name, 2);
  }

  private static String aggregateSql(String name, Aggregator aggregator) {
    switch (aggregator) {
      case SUM:
        return String.format("SUM(T2.%s)", name);
      case UNIQUE:
        return String.format("IF(MIN(T2.%s) = MAX(T2.%s), ANY_VALUE(T2.%s), -1)", name, name, name);
      case UNRECOGNIZED:
      case AGGREGATOR_UNSPECIFIED:
        break;
    }
    throw new IllegalArgumentException("Unsupported aggregator: " + aggregator);
  }

  private static String selectAs(String expression, String name, int spaces) {
    if (expression.isEmpty()) {
      return expression;
    }
    return String.format("%s(%s) AS %s,\n", Strings.repeat(" ", spaces), expression, name);
  }
}
