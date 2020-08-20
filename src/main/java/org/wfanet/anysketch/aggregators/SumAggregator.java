package org.wfanet.anysketch.aggregators;

class SumAggregator implements Aggregator {
  @Override
  public long aggregate(long value1, long value2) {
    return value1 + value2;
  }
}
