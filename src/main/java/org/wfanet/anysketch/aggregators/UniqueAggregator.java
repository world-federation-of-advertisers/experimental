package org.wfanet.anysketch.aggregators;

/**
 * Aggregator that tracks a single value.
 *
 * <p>For identical inputs, this returns that value. For different inputs, this returns a special
 * value DESTROYED.
 *
 * <p>To encode more efficiently into protos (since negative numbers consume more bits in int64
 * fields) we shift each value up by 1. In other words, DESTROYED encodes as zero in protos, 0 as 1,
 * 1 as 2, etc.
 */
public class UniqueAggregator implements Aggregator {
  public static final long DESTROYED = -1;

  @Override
  public long encodeToProtoValue(long value) {
    return value + 1;
  }

  @Override
  public long decodeFromProtoValue(long protoValue) {
    return protoValue - 1;
  }

  @Override
  public long aggregate(long value1, long value2) {
    return value1 == value2 ? value1 : DESTROYED;
  }
}
