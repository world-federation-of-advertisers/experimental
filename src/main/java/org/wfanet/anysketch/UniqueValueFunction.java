package org.wfanet.anysketch;

/**
 * ValueFunction is a commutative binary function that takes two values and combines them into one.
 * For Unique Value Function, if all aggregated values are the same then result of aggregation is
 * equal to the value. Otherwise it is -1. Note that in the proto the destructor will be 0.
 */
class UniqueValueFunction implements ValueFunction {

  /**
   * The name of this value function is UniqueValueFunction.
   */
  public String name() {
    return "UniqueValueFunction";
  }

  /**
   * The aggregator either stores the value or the destructor (-1). The destructor will be stored as
   * 0 in the proto for space saving reasons.
   */
  public long getValue(long old_value, long new_value) {
    if (old_value == new_value) {
      return old_value;
    }
    return -1;
  }

  /**
   * We store the value that is passed to the value function if this is the first assignment to this
   * register.
   */
  public long getInitialValue(long new_value) {
    return new_value;
  }
}