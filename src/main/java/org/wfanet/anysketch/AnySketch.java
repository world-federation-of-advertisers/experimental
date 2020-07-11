package org.wfanet.anysketch;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.wfanet.anysketch.AnySketch.Register;

/**
 * A generalized sketch class. This sketch class generalizes the data structure required to capture
 * Bloom filters, HLLs, Cascading Legions, Vector of Counts, and other sketch types. It uses a map
 * of register keys to a register value which is a tuple of counts.
 */
public class AnySketch implements Iterable<Register> {

  private List<IndexFunction> indexFunctions;
  private List<ValueFunction> valueFunctions;
  private HashFunction hashFunction;
  private Map<Long, List<Long>> registers;

  /**
   * Enriched cardinality sketch register class.
   */
  public static class Register {

    // Linearized register index
    private long index;
    // Values of the register
    private ImmutableList<Long> values;

    Register(long index, ImmutableList<Long> values) {
      this.index = index;
      this.values = values;
    }

    public long getIndex() {
      return index;
    }

    public ImmutableList<Long> getValues() {
      return values;
    }
  }

  /**
   * Creates AnySketch object expecting IndexFunction, ValueFunction, and HashFunction arguments.
   *
   * @param indexFunctions List of {@link IndexFunction} objects
   * @param valueFunctions List of {@link ValueFunction} objects
   * @param hashFunction a {@link HashFunction} object
   */
  public AnySketch(
      List<IndexFunction> indexFunctions,
      List<ValueFunction> valueFunctions,
      HashFunction hashFunction) {
    this.indexFunctions = indexFunctions;
    this.valueFunctions = valueFunctions;
    this.hashFunction = hashFunction;
    this.registers = new HashMap<>();
  }

  // Returns the number of values per Register
  private int registerSize() {
    return valueFunctions.size();
  }

  // ConsumeBits divides the fingerprint into chunks with maxValue size
  private long consumeBits(long fingerprint, long maxValue) {
    Preconditions.checkArgument(maxValue > 0);
    return fingerprint % maxValue;
  }

  private long getIndex(long fingerprint) {
    long product = 1;
    long linearized_index = 0;
    for (IndexFunction indexFunction : indexFunctions) {
      long hashMaxValue = indexFunction.maxSupportedHash();
      long indexFingerprint = consumeBits(fingerprint, hashMaxValue);
      fingerprint /= hashMaxValue;
      linearized_index += product * indexFunction.getIndex(indexFingerprint);
      product *= indexFunction.maxIndex();
    }
    return linearized_index;
  }

  private void insertPreHashed(long fingerprint, ImmutableList<Long> values) {
    long index = getIndex(fingerprint);
    List<Long> registerValues = registers.putIfAbsent(index, new ArrayList<>(values));
    boolean inserted = false;
    if (registerValues == null) {
      inserted = true;
      registerValues = new ArrayList<>(values);
    }
    Preconditions.checkState(registerValues.size() == registerSize());
    for (int i = 0; i < registerSize(); i++) {
      registerValues.set(i,
          inserted ? valueFunctions.get(i).getInitialValue(values.get(i))
              : valueFunctions.get(i).getValue(registerValues.get(i), values.get(i))
      );
    }
  }

  /**
   * Adds `item` to the Sketch.
   *
   * Insert determines a register by hashing `item` and applying the index functions described in
   * the config. The values in the register are updated by invoking the value functions described in
   * the config with the old value and the value provided here to produce a new value.
   *
   * @param item String key
   * @param values Map of values
   */
  public void insert(String item, Map<String, Long> values) {
    Preconditions.checkState(!values.containsValue(null));
    List<Long> valuesArray = new ArrayList<>();
    for (int i = 0; i < registerSize(); i++) {
      valuesArray.add(values.get(valueFunctions.get(i).name()));
    }
    insert(item, valuesArray);
  }

  /**
   * Inserts a new item to the AnySketch object with following arguments.
   *
   * @param item String key
   * @param values List of values
   */
  public void insert(String item, List<Long> values) {
    Preconditions.checkState(values.size() == registerSize());
    long fingerprint = hashFunction.fingerprint(item);
    insertPreHashed(fingerprint, ImmutableList.copyOf(values));
  }

  /**
   * Inserts a new item to the AnySketch object with following arguments.
   *
   * @param item Long key
   * @param values Map of values
   */
  public void insert(Long item, Map<String, Long> values) {
    insert(item.toString(), values);
  }

  /**
   * Inserts a new item to the AnySketch object with following arguments.
   *
   * @param item Long key
   * @param values List of values
   */
  public void insert(Long item, List<Long> values) {
    insert(item.toString(), values);
  }

  /**
   * Merges the other sketch into this one. The result is equivalent to sketching the union of the
   * sets that went into this and the other sketch.
   *
   * @param other {@link AnySketch} object
   */
  public void merge(AnySketch other) {
    for (Map.Entry<Long, List<Long>> entry : other.registers.entrySet()) {
      insertPreHashed(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
    }
  }

  /**
   * Merges all the other sketches into this one. The result is equivalent to sketching the union of
   * all of the sets.
   *
   * @param others List of {@link AnySketch} objects
   */
  public void mergeAll(List<AnySketch> others) {
    for (AnySketch other : others) {
      merge(other);
    }
  }

  /**
   * Iterates through the registers that the AnySketch object holds.
   *
   * @return Iterator of {@link Register}
   */
  @Override
  public Iterator<Register> iterator() {
    Iterator<Map.Entry<Long, List<Long>>> iterator = registers.entrySet().iterator();
    return new Iterator<Register>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Register next() {
        Map.Entry<Long, List<Long>> entry = iterator.next();
        return new Register(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
      }
    };
  }
}
