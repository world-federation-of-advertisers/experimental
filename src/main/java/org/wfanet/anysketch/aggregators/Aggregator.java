package org.wfanet.anysketch.aggregators;

public interface Aggregator {
  long aggregate(long value1, long value2);

  /** Converts a value into how its stored in sketch protos. */
  default long encodeToProtoValue(long value) {
    return value;
  }

  /** Converts a value stored in a proto for use in the AnySketch Java class. */
  default long decodeFromProtoValue(long protoValue) {
    return protoValue;
  }
}
