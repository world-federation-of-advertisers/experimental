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
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import com.google.common.truth.Correspondence;
import java.util.ArrayList;
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
    return actual.getIndex().equals(expected.getIndex())
        && actual.getValues().containsAll(expected.getValues())
        && expected.getValues().containsAll(actual.getValues());
  }

  class FakeIndexFunction implements IndexFunction {

    @Override
    public UnsignedLong getIndex(UnsignedLong hash) {
      return hash.times(UnsignedLong.valueOf(10L));
    }

    @Override
    public UnsignedLong maxIndex() {
      return UnsignedLong.valueOf(3L);
    }

    @Override
    public UnsignedLong maxSupportedHash() {
      return UnsignedLong.valueOf(5L);
    }
  }

  class FakeHashFunction implements HashFunction {

    @Override
    public UnsignedLong fingerprint(String item) {
      return UnsignedLong.valueOf(item);
    }
  }

  class FakeValueFunction implements ValueFunction {

    @Override
    public String name() {
      return "some-value-function-name";
    }

    @Override
    public long getValue(long oldValue, long newValue) {
      return oldValue + newValue;
    }

    @Override
    public long getInitialValue(long newValue) {
      return newValue * 100;
    }
  }

  @Test
  public void AnySketch_testBasicBehavior() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            singletonList(new FakeValueFunction()),
            new FakeHashFunction());
    assertThat(anySketch.iterator().hasNext()).isFalse();
  }

  @Test
  public void AnySketch_testInsertMethodSucceeds() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            singletonList(new FakeValueFunction()),
            new FakeHashFunction());
    anySketch.insert(1L, singletonList(1L));

    Register expected = new Register(10L, ImmutableList.of(100L));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE).containsExactly(expected);
  }

  @Test
  public void AnySketch_testInsertMethodWithMultipleValuesSucceeds() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            asList(new FakeValueFunction(), new FakeValueFunction(), new FakeValueFunction()),
            new FakeHashFunction());
    anySketch.insert(1L, asList(1L, 3L, 10L));

    Register expected = new Register(10L, ImmutableList.of(100L, 300L, 1000L));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE).containsExactly(expected);
  }

  @Test
  public void AnySketch_testInsertMethodWithHashMapSucceeds() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            singletonList(new FakeValueFunction()),
            new FakeHashFunction());
    Map<String, Long> values = new HashMap<>();
    values.put("some-value-function-name", 1L);
    anySketch.insert(1L, values);

    Register expected = new Register(10L, ImmutableList.of(100L));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE).containsExactly(expected);
  }

  @Test
  public void AnySketch_testInsertMethodWithHashMapWithNullValueFails() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            singletonList(new FakeValueFunction()),
            new FakeHashFunction());
    Map<String, Long> values = new HashMap<>();
    values.put("some-value-function-name", null);

    assertThrows(IllegalStateException.class, () -> anySketch.insert(1L, values));
  }

  @Test
  public void AnySketch_testInsertMethodWithStringKeySucceeds() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            singletonList(new FakeValueFunction()),
            new FakeHashFunction());
    anySketch.insert("1", singletonList(1L));

    Register expected = new Register(10L, ImmutableList.of(100L));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE).containsExactly(expected);
  }

  @Test
  public void AnySketch_testInsertMethodWithStringKeyHashMapSucceeds() {
    AnySketch anySketch =
        new AnySketch(
            singletonList(new FakeIndexFunction()),
            singletonList(new FakeValueFunction()),
            new FakeHashFunction());
    Map<String, Long> values = new HashMap<>();
    values.put("some-value-function-name", 1L);
    anySketch.insert("1", values);

    Register expected = new Register(10L, ImmutableList.of(100L));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE).containsExactly(expected);
  }

  @Test
  public void AnySketch_testMergeMethodSucceeds() {
    List<IndexFunction> indexFunctions = new ArrayList<>();
    indexFunctions.add(new FakeIndexFunction());
    List<ValueFunction> valueFunctions = new ArrayList<>();
    valueFunctions.add(new FakeValueFunction());
    AnySketch anySketch = new AnySketch(indexFunctions, valueFunctions, new FakeHashFunction());
    AnySketch otherAnySketch =
        new AnySketch(indexFunctions, valueFunctions, new FakeHashFunction());
    anySketch.insert(1L, singletonList(1L));
    otherAnySketch.insert(2L, singletonList(20L));
    anySketch.merge(otherAnySketch);

    Register expected = new Register(10L, ImmutableList.of(100L));
    Register expected2 = new Register(0L, ImmutableList.of(200000L));
    assertThat(anySketch).comparingElementsUsing(EQUIVALENCE).containsExactly(expected, expected2);
  }

  @Test
  public void AnySketch_testMergeAllMethodSucceeds() {
    List<IndexFunction> indexFunctions = singletonList(new FakeIndexFunction());
    List<ValueFunction> valueFunctions = singletonList(new FakeValueFunction());
    HashFunction hashFunction = new FakeHashFunction();

    AnySketch anySketch = new AnySketch(indexFunctions, valueFunctions, hashFunction);
    anySketch.insert(1L, singletonList(1L));

    assertThat(anySketch)
        .comparingElementsUsing(EQUIVALENCE)
        .containsExactly(new Register(10L, ImmutableList.of(100L)));

    AnySketch otherAnySketch = new AnySketch(indexFunctions, valueFunctions, hashFunction);
    otherAnySketch.insert(10L, singletonList(20L));

    assertThat(otherAnySketch)
        .comparingElementsUsing(EQUIVALENCE)
        .containsExactly(new Register(0L, ImmutableList.of(2000L)));

    anySketch.mergeAll(singletonList(otherAnySketch));

    // Note: we expect the "2000" value from the previous sketch to be run through FakeValueFunction
    // again. This is a little counter-intuitive but correct; hence the value "200000" below.
    assertThat(anySketch)
        .comparingElementsUsing(EQUIVALENCE)
        .containsExactly(
            new Register(10L, ImmutableList.of(100L)), new Register(0L, ImmutableList.of(200000L)));
  }

  @Test
  public void AnySketch_testInsertMethodMultipleIndexesBugSucceeds() {
    IndexFunction indexFunction =
        new IndexFunction() {
          @Override
          public UnsignedLong getIndex(UnsignedLong hash) {
            return hash.dividedBy(UnsignedLong.valueOf(2L));
          }

          @Override
          public UnsignedLong maxIndex() {
            return UnsignedLong.valueOf(3L);
          }

          @Override
          public UnsignedLong maxSupportedHash() {
            return UnsignedLong.valueOf(6L);
          }
        };
    AnySketch anySketch =
        new AnySketch(
            ImmutableList.of(indexFunction, indexFunction),
            ImmutableList.of(new FakeValueFunction()),
            new FakeHashFunction());

    // Fill up every possible register
    //
    // Since the output of getIndex can be any value in {0, 1, 2},
    // there should be 9 total registers that get populated
    for (long i = 0; i < 10000; i++) {
      anySketch.insert(i, singletonList(0L));
    }

    Set<Long> indexes = new HashSet<>();
    for (Register register : anySketch) {
      indexes.add(register.getIndex().longValue());
    }

    assertThat(indexes).containsExactly(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
  }
}
