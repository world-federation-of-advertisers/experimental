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

    long first_value = uniqueValueFunction.getValue(5, 5);
    assertThat(uniqueValueFunction.getValue(first_value, 4)).isEqualTo(-1);
  }
}
