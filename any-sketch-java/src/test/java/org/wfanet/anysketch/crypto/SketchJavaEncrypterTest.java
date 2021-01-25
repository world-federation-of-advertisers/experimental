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

package org.wfanet.anysketch.crypto;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.protobuf.ByteString;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.wfanet.measurement.api.v1alpha.Sketch;
import org.wfanet.measurement.api.v1alpha.Sketch.Register;
import org.wfanet.measurement.api.v1alpha.SketchConfig;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec;
import org.wfanet.measurement.api.v1alpha.SketchConfig.ValueSpec.Aggregator;

@RunWith(JUnit4.class)
public class SketchJavaEncrypterTest {

  static {
    System.loadLibrary("sketch_encrypter_adapter");
  }

  static final int CURVE_ID = 415; // NID_X9_62_prime256v1
  static final int MAX_COUNTER_VALUE = 10;
  static final byte[] PUBLIC_KEY_G =
      new byte[] {
        3, 107, 23, -47, -14, -31, 44, 66,
        71, -8, -68, -26, -27, 99, -92, 64,
        -14, 119, 3, 125, -127, 45, -21, 51,
        -96, -12, -95, 57, 69, -40, -104, -62,
        -106
      };
  static final byte[] PUBLIC_KEY_Y =
      new byte[] {
        2, 119, -65, 64, 108, 90, -92, 55,
        100, 19, -28, -128, -32, -85, -117, 14,
        -4, -87, -103, -45, 98, 32, 78, 109,
        22, -122, -32, -66, 86, 120, 17, 96,
        77
      };

  Sketch CreateFakeSketch(int numRegisters) {
    Sketch.Builder plaintextSketch =
        Sketch.newBuilder()
            .setConfig(
                SketchConfig.newBuilder()
                    .addValues(ValueSpec.newBuilder().setAggregator(Aggregator.UNIQUE))
                    .addValues(ValueSpec.newBuilder().setAggregator(Aggregator.SUM)));
    Random random = new Random();
    for (int i = 0; i < numRegisters; ++i) {
      plaintextSketch.addRegisters(
          Register.newBuilder()
              .setIndex(random.nextInt())
              .addValues(random.nextInt(10000) + 1) // +1 so it is not destroyed
              .addValues(random.nextInt(100) + 1)); // 1~100
    }

    return plaintextSketch.build();
  }

  @Test
  public void SketchJavaEncrypter_basicbehavior() throws Exception {
    int numRegister = 1000;
    Sketch plaintextSketch = CreateFakeSketch(numRegister);

    EncryptSketchRequest request =
        EncryptSketchRequest.newBuilder()
            .setSketch(plaintextSketch)
            .setCurveId(CURVE_ID)
            .setMaximumValue(MAX_COUNTER_VALUE)
            .setElGamalKeys(
                ElGamalPublicKeys.newBuilder()
                    .setElGamalG(com.google.protobuf.ByteString.copyFrom(PUBLIC_KEY_G))
                    .setElGamalY(com.google.protobuf.ByteString.copyFrom(PUBLIC_KEY_Y)))
            .build();

    EncryptSketchResponse response =
        EncryptSketchResponse.parseFrom(
            SketchEncrypterAdapter.EncryptSketch(request.toByteArray()));

    // Each register contains 3 ciphertexts, each cipherText is 66 bytes.
    assertThat(response.getEncryptedSketch().size()).isEqualTo(66 * 3 * numRegister);
  }

  @Test
  public void SketchJavaEncrypter_invalidPublicKeyShouldThrow() {
    EncryptSketchRequest request =
        EncryptSketchRequest.newBuilder()
            .setCurveId(CURVE_ID)
            .setMaximumValue(MAX_COUNTER_VALUE)
            .setElGamalKeys(
                ElGamalPublicKeys.newBuilder()
                    .setElGamalG(ByteString.copyFromUtf8("invalidKey_a"))
                    .setElGamalY(ByteString.copyFromUtf8("invalidKey_b")))
            .build();

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> SketchEncrypterAdapter.EncryptSketch(request.toByteArray()));
    assertThat(exception).hasMessageThat().contains("Could not decode point");
  }

  @Test
  public void SketchJavaEncrypter_internalCxxErrorIsReturnBackToJava() {
    Sketch sketchWithNoConfig =
        Sketch.newBuilder().addRegisters(Register.newBuilder().setIndex(1).addValues(2)).build();

    EncryptSketchRequest request =
        EncryptSketchRequest.newBuilder()
            .setSketch(sketchWithNoConfig)
            .setCurveId(CURVE_ID)
            .setMaximumValue(MAX_COUNTER_VALUE)
            .setElGamalKeys(
                ElGamalPublicKeys.newBuilder()
                    .setElGamalG(com.google.protobuf.ByteString.copyFrom(PUBLIC_KEY_G))
                    .setElGamalY(com.google.protobuf.ByteString.copyFrom(PUBLIC_KEY_Y)))
            .build();

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> SketchEncrypterAdapter.EncryptSketch(request.toByteArray()));
    assertThat(exception).hasMessageThat().contains("config");
  }

  @Test
  public void SketchJavaEncrypter_combineKeysBasicBehavior() throws Exception {
    CombineElGamalPublicKeysRequest request =
        CombineElGamalPublicKeysRequest.newBuilder()
            .setCurveId(CURVE_ID)
            .addElGamalKeys(
                ElGamalPublicKeys.newBuilder()
                    .setElGamalG(ByteString.copyFromUtf8("foo"))
                    .setElGamalY(ByteString.copyFromUtf8("bar")))
            .build();

    CombineElGamalPublicKeysResponse response =
        CombineElGamalPublicKeysResponse.parseFrom(
            SketchEncrypterAdapter.CombineElGamalPublicKeys(request.toByteArray()));

    // The result is equal to the only key in the request.
    assertThat(response.getElGamalKeys()).isEqualTo(request.getElGamalKeys(0));
  }
}
