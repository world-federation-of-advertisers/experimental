package org.wfanet.anysketch.aggregators;

/** Interface for combining values and serializing into protos. */
public interface Aggregator {
  /** Combines two values. */
  long aggregate(long value1, long value2);

  /** Converts a value into how it's stored in sketch protos. */
  default long encodeToProtoValue(long value) {
    return value;
  }

  /** Converts a value stored in a proto for use in the AnySketch Java class. */
  default long decodeFromProtoValue(long protoValue) {
    return protoValue;
  }
}
