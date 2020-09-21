package org.wfanet.anysketch;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.wfanet.measurement.api.v1alpha.Distribution;
import org.wfanet.measurement.api.v1alpha.ExponentialDistribution;
import org.wfanet.measurement.api.v1alpha.SketchConfig;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec.Aggregator;

/** Converts a SketchConfig into an SQL query. */
public class SketchSql {
  private SketchSql() {}

  /**
   * Converts `config` into an SQL query.
   *
   * <p>The output query will select one row per register of the sketch. The columns are the indexes
   * (in order listed in the config) followed by the values (in order listed in the config). The
   * column names are given by the IndexSpec.name and ValueSpec.name fields.
   *
   * @param impressionTable the table or subquery containing exactly the impressions to sketch
   * @param config the SketchConfig
   * @return SQL to produce a sketch
   */
  public static String fromConfig(String impressionTable, SketchConfig config) {
    StringBuilder sql = new StringBuilder();

    // In intermediate table T1, select a row for each impression. Each row in T1 is equivalent to
    // a sketch containing only that impression.
    sql.append("WITH T1 AS (\n");
    sql.append("  SELECT\n");

    config
        .getIndexesList()
        .forEach(i -> sql.append(selectDistributionSql(i.getName(), i.getDistribution())));

    config
        .getValuesList()
        .forEach(i -> sql.append(selectDistributionSql(i.getName(), i.getDistribution())));

    sql.append(String.format("  FROM %s AS Impressions\n)\n", impressionTable));

    // Aggregate the individual sketches in T1 by grouping by the index columns of T1.
    sql.append("SELECT\n");

    config.getIndexesList().forEach(i -> sql.append(String.format("  T1.%s,\n", i.getName())));

    for (ValueSpec value : config.getValuesList()) {
      sql.append(selectAggregateSql(value.getName(), value.getAggregator()));
    }

    sql.append("FROM T1\nGROUP BY ");

    String groupByFields =
        IntStream.range(1, config.getIndexesCount() + 1)
            .mapToObj(Integer::toString)
            .collect(Collectors.joining(", "));
    sql.append(groupByFields);

    return sql.toString();
  }

  private static String selectAggregateSql(String name, Aggregator aggregator) {
    return String.format("  (%s) AS %s,\n", aggregateSql(name, aggregator), name);
  }

  private static String aggregateSql(String name, Aggregator aggregator) {
    switch (aggregator) {
      case SUM:
        return String.format("SUM(T1.%s)", name);
      case UNIQUE:
        return String.format("IF(MIN(T1.%s) = MAX(T1.%s), ANY_VALUE(T1.%s), -1)", name, name, name);
      case UNRECOGNIZED:
      case AGGREGATOR_UNSPECIFIED:
        break;
    }
    throw new IllegalArgumentException("Unsupported aggregator: " + aggregator);
  }

  private static String selectDistributionSql(String name, Distribution distribution) {
    return String.format("    (%s) AS %s,\n", distributionSql(name, distribution), name);
  }

  private static String distributionSql(String name, Distribution distribution) {
    switch (distribution.getDistributionChoiceCase()) {
      case EXPONENTIAL:
        ExponentialDistribution e = distribution.getExponential();
        return String.format(
            "CAST(LOG(EXP(%f) + %s / BIT_CAST_TO_UINT64(-1) * (1 - EXP(%f)) / %f * %d AS INT64)",
            e.getRate(), fingerprintSql(name), e.getRate(), e.getRate(), e.getNumValues());
      case UNIFORM:
        return String.format(
            "MOD(%s, %s)", fingerprintSql(name), distribution.getUniform().getNumValues());
      case ORACLE:
        return "Impressions." + distribution.getOracle().getKey();
      case GEOMETRIC:
        return String.format(
            "CAST(FLOOR(LOG(%s / BIT_CAST_TO_UINT64(-1), 2)) AS INT64)", fingerprintSql(name));
      case DIRAC_MIXTURE:
      case VERBATIM:
      case CONSTANT:
      case DISTRIBUTIONCHOICE_NOT_SET:
        break;
    }
    throw new IllegalArgumentException("Unsupported distribution: " + distribution);
  }

  private static String fingerprintSql(String name) {
    return String.format(
        "BIT_CAST_TO_UINT64(FARM_FINGERPRINT(CONCAT(%s, Impressions.VirtualId)))", name);
  }
}
