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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UniqueValueFunctionTest {

  @Test
  public void UniqueValueFunctionTest_testBasicBehavior() {
    UniqueValueFunction uniqueValueFunction = new UniqueValueFunction();
    assertThat(uniqueValueFunction.name()).isEqualTo("UniqueValueFunction");
    assertThat(uniqueValueFunction.getInitialValue(5)).isEqualTo(5);
    assertThat(uniqueValueFunction.getValue(5, 5)).isEqualTo(5);
    assertThat(uniqueValueFunction.getValue(4, 5)).isEqualTo(-1);
  }

  @Test
  public void UniqueValueFunctionTest_testMultipleAllocation() {
    UniqueValueFunction uniqueValueFunction = new UniqueValueFunction();
    assertThat(uniqueValueFunction.name()).isEqualTo("UniqueValueFunction");

    long firstValue = uniqueValueFunction.getValue(5, 5);
    assertThat(uniqueValueFunction.getValue(firstValue, 4)).isEqualTo(-1);
  }
}
