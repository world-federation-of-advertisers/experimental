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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Correspondence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.anysketch.AnySketch.Register;

@RunWith(JUnit4.class)
public class AnySketchTest {

  static final Correspondence<Register, Register> EQUIVALENCE =
      Correspondence.from(AnySketchTest::registersEquivalent, "is equivalent to");

  static boolean registersEquivalent(@Nullable Register actual, @Nullable Register expected) {
    return actual.getIndex() == expected.getIndex() && actual.getValues().size() == expected
        .getValues().size();
  }

  class FakeIndexFunction implements IndexFunction {

    @Override
    public long getIndex(long hash) {
      return hash * 10;
    }

    @Override
    public long maxIndex() {
      return 3;
    }

    @Override
    public long maxSupportedHash() {
      return 5;
    }
  }

  class FakeHashFunction implements HashFunction {

    @Override
    public long fingerprint(String item) {
      return Long.valueOf(item);
    }
  }

  class FakeValueFunction implements ValueFunction {

    @Override
    public String name() {
      return "some-value-function-name";
    }

    @Override
    public long getValue(long old_value, long new_value) {
      return old_value + new_value;
    }

    @Override
    public long getInitialValue(long new_value) {
      return new_value * 100;
    }
  }

  @Test
  public void AnySketch_testBasicBehavior() {
    AnySketch anySketch = new AnySketch(Arrays.asList(new FakeIndexFunction()),
        Arrays.asList(new FakeValueFunction()), new FakeHashFunction());
    assertThat(anySketch.iterator().hasNext()).isFalse();
  }

  @Test
  public void AnySketch_testInsertMethodSucceeds() {
    AnySketch anySketch = new AnySketch(Arrays.asList(new FakeIndexFunction()),
        Arrays.asList(new FakeValueFunction()), new FakeHashFunction());
    anySketch.insert(1l, Arrays.asList(1l));

    Register expected = new Register(10l, ImmutableList.of(100l));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE)
        .containsExactly(expected);
  }

  @Test
  public void AnySketch_testInsertMethodWithMultipleValuesSucceeds() {
    AnySketch anySketch = new AnySketch(Arrays.asList(new FakeIndexFunction()),
        Arrays.asList(new FakeValueFunction(), new FakeValueFunction(), new FakeValueFunction()),
        new FakeHashFunction());
    anySketch.insert(1l, Arrays.asList(1l, 3l, 10l));

    Register expected = new Register(10l, ImmutableList.of(100l, 300l, 1000l));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE)
        .containsExactly(expected);
  }

  @Test
  public void AnySketch_testInsertMethodWithHashMapSucceeds() {
    AnySketch anySketch = new AnySketch(Arrays.asList(new FakeIndexFunction()),
        Arrays.asList(new FakeValueFunction()), new FakeHashFunction());
    Map<String, Long> values = new HashMap<>();
    values.put("some-value-function-name", 1l);
    anySketch.insert(1l, values);

    Register expected = new Register(10l, ImmutableList.of(100l));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE)
        .containsExactly(expected);
  }

  @Test
  public void AnySketch_testInsertMethodWithHashMapWithNullValueFails() {
    AnySketch anySketch = new AnySketch(Arrays.asList(new FakeIndexFunction()),
        Arrays.asList(new FakeValueFunction()), new FakeHashFunction());
    Map<String, Long> values = new HashMap<>();
    values.put("some-value-function-name", null);

    assertThrows(IllegalStateException.class, () -> anySketch.insert(1l, values));
  }

  @Test
  public void AnySketch_testInsertMethodWithStringKeySucceeds() {
    AnySketch anySketch = new AnySketch(Arrays.asList(new FakeIndexFunction()),
        Arrays.asList(new FakeValueFunction()), new FakeHashFunction());
    anySketch.insert("1", Arrays.asList(1l));

    Register expected = new Register(10l, ImmutableList.of(100l));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE)
        .containsExactly(expected);
  }

  @Test
  public void AnySketch_testInsertMethodWithStringKeyHashMapSucceeds() {
    AnySketch anySketch = new AnySketch(Arrays.asList(new FakeIndexFunction()),
        Arrays.asList(new FakeValueFunction()), new FakeHashFunction());
    Map<String, Long> values = new HashMap<>();
    values.put("some-value-function-name", 1l);
    anySketch.insert("1", values);

    Register expected = new Register(10l, ImmutableList.of(100l));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE)
        .containsExactly(expected);
  }

  @Test
  public void AnySketch_testMergeMethodSucceeds() {
    List<IndexFunction> indexFunctions = new ArrayList<>();
    indexFunctions.add(new FakeIndexFunction());
    List<ValueFunction> valueFunctions = new ArrayList<>();
    valueFunctions.add(new FakeValueFunction());
    AnySketch anySketch = new AnySketch(indexFunctions, valueFunctions, new FakeHashFunction());
    AnySketch otherAnySketch = new AnySketch(indexFunctions, valueFunctions,
        new FakeHashFunction());
    anySketch.insert(1l, Arrays.asList(1l));
    otherAnySketch.insert(2l, Arrays.asList(20l));
    anySketch.merge(otherAnySketch);

    Register expected = new Register(10l, ImmutableList.of(100l));
    Register expected2 = new Register(0l, ImmutableList.of(200000l));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE)
        .containsExactly(expected, expected2);
  }

  @Test
  public void AnySketch_testMergeAllMethodSucceeds() {
    List<IndexFunction> indexFunctions = new ArrayList<>();
    indexFunctions.add(new FakeIndexFunction());
    List<ValueFunction> valueFunctions = new ArrayList<>();
    valueFunctions.add(new FakeValueFunction());
    AnySketch anySketch = new AnySketch(indexFunctions, valueFunctions, new FakeHashFunction());
    AnySketch otherAnySketch = new AnySketch(indexFunctions, valueFunctions,
        new FakeHashFunction());
    anySketch.insert(1l, Arrays.asList(1l));
    otherAnySketch.insert(2l, Arrays.asList(20l));
    anySketch.mergeAll(Arrays.asList(otherAnySketch));

    Register expected = new Register(10l, ImmutableList.of(100l));
    Register expected2 = new Register(0l, ImmutableList.of(200000l));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE)
        .containsExactly(expected, expected2);
  }

  @Test
  public void AnySketch_testInsertMethodMultipleIndexesBugSucceeds() {
    IndexFunction indexFunction = new IndexFunction() {
      @Override
      public long getIndex(long hash) {
        return hash / 2;
      }

      @Override
      public long maxIndex() {
        return 3;
      }

      @Override
      public long maxSupportedHash() {
        return 6;
      }
    };
    AnySketch anySketch = new AnySketch(
        ImmutableList.of(
            indexFunction,
            indexFunction
        ),
        ImmutableList.of(new FakeValueFunction()),
        new FakeHashFunction()
    );
    // Fill up every possible register
    //
    // Since the output of getIndex can be any value in {0, 1, 2},
    // there should be 9 total registers that get populated
    for (long i = 0; i < 10000; i++) {
      anySketch.insert(i, Arrays.asList(0L));
    }

    Set<Long> indexes = new HashSet<>();
    for (Register register : anySketch) {
      indexes.add(register.getIndex());
    }

    assertThat(indexes).containsExactly(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
  }
}
