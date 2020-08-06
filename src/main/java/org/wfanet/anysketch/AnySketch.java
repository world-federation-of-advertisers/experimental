// Copyright 2020 The Any Sketch Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wfanet.anysketch;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
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

  private final ImmutableList<IndexFunction> indexFunctions;
  private final ImmutableList<ValueFunction> valueFunctions;
  private final HashFunction hashFunction;
  private final Map<UnsignedLong, List<Long>> registers;

  /** Enriched cardinality sketch register class. */
  public static class Register {

    // Linearized register index
    private UnsignedLong index;
    // Values of the register
    private ImmutableList<Long> values;

    Register(UnsignedLong index, ImmutableList<Long> values) {
      this.index = index;
      this.values = values;
    }

    Register(long index, ImmutableList<Long> values) {
      this(UnsignedLong.valueOf(index), values);
    }

    public UnsignedLong getIndex() {
      return index;
    }

    public ImmutableList<Long> getValues() {
      return values;
    }

    @Override
    public String toString() {
      return "<Register index: " + index + " values: " + values + ">";
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
    this.indexFunctions = ImmutableList.copyOf(indexFunctions);
    this.valueFunctions = ImmutableList.copyOf(valueFunctions);
    this.hashFunction = hashFunction;
    this.registers = new HashMap<>();
  }

  /** Returns the list of ValueFunctions for the sketch. */
  public ImmutableList<ValueFunction> getValueFunctions() {
    return valueFunctions;
  }

  // Returns the number of values per Register
  private int registerSize() {
    return valueFunctions.size();
  }

  // ConsumeBits divides the fingerprint into chunks with maxValue size
  private UnsignedLong consumeBits(UnsignedLong fingerprint, UnsignedLong maxValue) {
    Preconditions.checkArgument(!maxValue.equals(UnsignedLong.ZERO));
    return fingerprint.mod(maxValue);
  }

  private UnsignedLong getIndex(UnsignedLong fingerprint) {
    UnsignedLong product = UnsignedLong.ONE;
    UnsignedLong linearizedIndex = UnsignedLong.ZERO;
    for (IndexFunction indexFunction : indexFunctions) {
      UnsignedLong hashMaxValue = indexFunction.maxSupportedHash();

      UnsignedLong indexFingerprint = consumeBits(fingerprint, hashMaxValue);
      fingerprint = fingerprint.dividedBy(hashMaxValue);

      UnsignedLong indexPart = indexFunction.getIndex(indexFingerprint);
      linearizedIndex = linearizedIndex.plus(product.times(indexPart));
      product = product.times(indexFunction.maxIndex());
    }
    return linearizedIndex;
  }

  private void insertPreHashed(UnsignedLong fingerprint, ImmutableList<Long> values) {
    UnsignedLong index = getIndex(fingerprint);
    List<Long> registerValues = registers.computeIfAbsent(index, key -> new ArrayList<>());
    boolean inserted = registerValues.isEmpty();
    Preconditions.checkState(inserted || registerValues.size() == registerSize());
    for (int i = 0; i < registerSize(); i++) {
      ValueFunction valueFunction = valueFunctions.get(i);
      long value = values.get(i);
      if (inserted) {
        registerValues.add(valueFunction.getInitialValue(value));
      } else {
        registerValues.set(i, valueFunction.getValue(registerValues.get(i), value));
      }
    }
  }

  /**
   * Adds `item` to the Sketch.
   *
   * <p>Insert determines a register by hashing `item` and applying the index functions described in
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
    UnsignedLong fingerprint = hashFunction.fingerprint(item);
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
    for (Map.Entry<UnsignedLong, List<Long>> entry : other.registers.entrySet()) {
      insertPreHashed(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
    }
  }

  /**
   * Merges all the other sketches into this one. The result is equivalent to sketching the union of
   * all of the sets.
   *
   * @param others List of {@link AnySketch} objects
   */
  public void mergeAll(Iterable<AnySketch> others) {
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
    Iterator<Map.Entry<UnsignedLong, List<Long>>> iterator = registers.entrySet().iterator();
    return new Iterator<Register>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Register next() {
        Map.Entry<UnsignedLong, List<Long>> entry = iterator.next();
        return new Register(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
      }
    };
  }
}
