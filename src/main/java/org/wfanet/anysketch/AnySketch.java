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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.wfanet.anysketch.AnySketch.Register;
import org.wfanet.anysketch.distributions.Distribution;

/**
 * A generalized sketch class. This sketch class generalizes the data structure required to capture
 * Bloom filters, HLLs, Cascading Legions, Vector of Counts, and other sketch types. It uses a map
 * of register keys to a register value which is a tuple of numeric values.
 */
public class AnySketch implements Iterable<Register> {
  /** Represents a single register of an AnySketch. */
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

    @Override
    public String toString() {
      return "<Register index: " + index + " values: " + values + ">";
    }
  }

  private final ImmutableList<Distribution> indexDistributions;
  private final ImmutableList<ValueFunction> valueFunctions;
  private final Map<Long, List<Long>> registers;

  /**
   * Creates an AnySketch.
   *
   * @param indexDistributions Distributions for determining register index
   * @param valueFunctions Distributions for determining register values
   */
  public AnySketch(List<Distribution> indexDistributions, List<ValueFunction> valueFunctions) {
    this.indexDistributions = ImmutableList.copyOf(indexDistributions);
    this.valueFunctions = ImmutableList.copyOf(valueFunctions);
    this.registers = new HashMap<>();

    long indexesLeft = Long.MAX_VALUE;
    for (Distribution distribution : indexDistributions) {
      indexesLeft /= distribution.getSize();
    }
    Preconditions.checkArgument(indexesLeft > 0, "A register index could possibly exceed 2^63 - 1");
  }

  /** Retries the ValueFunctions that the AnySketch was constructed with. */
  public ImmutableList<ValueFunction> getValueFunctions() {
    return valueFunctions;
  }

  /** Merges a set of values into a register. */
  void aggregateIntoRegister(long index, List<Long> values) {
    Preconditions.checkArgument(values.size() == registerSize());
    registers.merge(index, new ArrayList<>(values), this::aggregateIntoExistingRegister);
  }

  /**
   * Adds `item` to the Sketch.
   *
   * <p>While itemMetadata can contain arbitrary values, certain Distributions may require some keys
   * to be present. In particular, OracleDistributions each require a specific key to exist.
   *
   * <p>It is not an error to include unnecessary keys in itemMetadata--they will simply be ignored.
   *
   * @param item an element to insert into the sketch
   * @param itemMetadata values that Distributions may access
   */
  public void insert(String item, Map<String, Long> itemMetadata) {
    long index = getLinearizedIndex(item, itemMetadata);
    @SuppressWarnings("UnstableApiUsage") // For toImmutableList()
    ImmutableList<Long> values =
        valueFunctions.stream()
            .map(valueFunction -> valueFunction.getDistribution().apply(item, itemMetadata))
            .collect(toImmutableList());
    aggregateIntoRegister(index, values);
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
   * Merges the other sketch into this one. The result is equivalent to sketching the union of the
   * sets that went into this and the other sketch.
   *
   * @param other {@link AnySketch} object
   */
  public void merge(AnySketch other) {
    for (Map.Entry<Long, List<Long>> entry : other.registers.entrySet()) {
      aggregateIntoRegister(entry.getKey(), entry.getValue());
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

  private int registerSize() {
    return valueFunctions.size();
  }

  private long getLinearizedIndex(String item, Map<String, Long> itemMetadata) {
    long product = 1L;
    long linearizedIndex = 0;

    for (Distribution distribution : indexDistributions) {
      long indexPart = distribution.apply(item, itemMetadata) - distribution.getMinValue();
      linearizedIndex = product * linearizedIndex + indexPart;
      product *= distribution.getMaxValue() - distribution.getMinValue();
    }
    return linearizedIndex;
  }

  private List<Long> aggregateIntoExistingRegister(List<Long> oldValues, List<Long> newValues) {
    Preconditions.checkState(oldValues.size() == registerSize());
    for (int i = 0; i < registerSize(); i++) {
      long oldValue = oldValues.get(i);
      long newValue = valueFunctions.get(i).getAggregator().aggregate(oldValue, newValues.get(i));
      oldValues.set(i, newValue);
    }
    return oldValues;
  }
}
