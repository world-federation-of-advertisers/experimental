package org.wfanet.anysketch;

/**
 * IndexFunction maps the output of a {@link HashFunction} to a coordinate in that dimension
 */
interface IndexFunction {

  /**
   * Returns the index of the hashmap given the hashed key
   *
   * @param hash hashed key
   */
  long getIndex(long hash);

  /**
   * Returns the max index value as output value to calculate next indexes
   */
  long maxIndex();

  /**
   * Returns the max supported hash value to divide fingerprint into chunks
   */
  long maxSupportedHash();
}
