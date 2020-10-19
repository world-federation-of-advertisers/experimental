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

package org.wfanet.anysketch.fingerprinters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FarmFingerprinterTest {
  private final FarmFingerprinter fingerprinter = new FarmFingerprinter();

  @Test
  public void testFingerPrintWithNullValueFails() {
    assertThrows(NullPointerException.class, () -> fingerprinter.fingerprint(null));
  }

  @Test
  public void testTwoSameStringsMatchSucceeds() {
    String x = "Foo";
    String y = "Foo";
    assertThat(fingerprinter.fingerprint(x)).isEqualTo(fingerprinter.fingerprint(y));
  }

  @Test
  public void testTwoDifferentStringsMatchFails() {
    String x = "Foo";
    String y = "Bar";
    assertThat(fingerprinter.fingerprint(x)).isNotEqualTo(fingerprinter.fingerprint(y));
  }
}
