package org.wfanet.anysketch;

/**
 * HashFunction class provides multiple hashing options to hash the key
 */
interface HashFunction {

  /**
   * Returns hashed value of the key inserted into Sketch
   *
   * @param item Key to be hashed
   */
  long fingerprint(String item);
}
