package org.wfanet.anysketch.aggregators;

public class Aggregators {
  private Aggregators() {}

  private static Aggregator sumAggregator = new SumAggregator();
  private static Aggregator uniqueAggregator = new UniqueAggregator();

  public static Aggregator sum() {
    return sumAggregator;
  }

  public static Aggregator unique() {
    return uniqueAggregator;
  }
}
