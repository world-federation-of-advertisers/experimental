package org.wfanet.anysketch;

/**
 * ValueFunction is a commutative  binary function that takes two values and combines them into one.
 * Addition is the canonical example.
 */
interface ValueFunction {

  /**
   * Returns the String name to find item in the map
   */
  String name();

  /**
   * Returns the new value by computing old and new value related to the algorithm
   */
  long getValue(long old_value, long new_value);

  /**
   * Returns the initial value before any computation
   */
  long getInitialValue(long new_value);
}
