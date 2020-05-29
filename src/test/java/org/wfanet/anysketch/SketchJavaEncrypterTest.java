package org.wfanet.anysketch;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import wfa.measurement.api.v1alpha.SketchOuterClass.Sketch;
import wfa.measurement.api.v1alpha.SketchOuterClass.Sketch.Register;
import wfa.measurement.api.v1alpha.SketchOuterClass.SketchConfig;
import wfa.measurement.api.v1alpha.SketchOuterClass.SketchConfig.IndexSpec;
import wfa.measurement.api.v1alpha.SketchOuterClass.SketchConfig.ValueSpec;
import wfa.measurement.api.v1alpha.SketchOuterClass.SketchConfig.ValueSpec.Aggregator;

@RunWith(JUnit4.class)
public class SketchJavaEncrypterTest {

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
                    .addIndexes(IndexSpec.getDefaultInstance())
                    .addValues(ValueSpec.newBuilder().setAggregator(Aggregator.UNIQUE))
                    .addValues(ValueSpec.newBuilder().setAggregator(Aggregator.SUM)));
    Random random = new Random();
    for (int i = 0; i < numRegisters; ++i) {
      plaintextSketch.addRegisters(
          Register.newBuilder()
              .addIndexes(random.nextInt())
              .addValues(random.nextInt())
              .addValues(random.nextInt()));
    }

    return plaintextSketch.build();
  }

  @Test
  public void SketchJavaEncrypter_basicbehavior() {
    SketchJavaEncrypter sketchJavaEncrypter =
        new SketchJavaEncrypter(CURVE_ID, MAX_COUNTER_VALUE, PUBLIC_KEY_G, PUBLIC_KEY_Y);

    int numRegister = 1000;
    Sketch plaintextSketch = CreateFakeSketch(numRegister);

    byte[] encrptedSketch = sketchJavaEncrypter.Encrypt(plaintextSketch);

    // Each register contains 3 ciphertexts, each cipherText is 66 bytes.
    assertThat(encrptedSketch.length).isEqualTo(66 * 3 * numRegister);
  }

  @Test
  public void SketchJavaEncrypter_emptyProtoGetEmptyResults() {
    SketchJavaEncrypter sketchJavaEncrypter =
        new SketchJavaEncrypter(CURVE_ID, MAX_COUNTER_VALUE, PUBLIC_KEY_G, PUBLIC_KEY_Y);
    assertThat(sketchJavaEncrypter.Encrypt(Sketch.getDefaultInstance()).length).isEqualTo(0);
  }

  @Test
  public void SketchJavaEncrypter_invalidPublicKeyShouldThrow() {
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                new SketchJavaEncrypter(
                    CURVE_ID,
                    MAX_COUNTER_VALUE,
                    "invalidKey_a".getBytes(),
                    "invalidKey_b".getBytes()));
    assertThat(exception).hasMessageThat().contains("Could not decode point");
  }

  @Test
  public void SketchJavaEncrypter_internalCxxErrorIsReturnBackToJava() {
    SketchJavaEncrypter sketchJavaEncrypter =
        new SketchJavaEncrypter(CURVE_ID, MAX_COUNTER_VALUE, PUBLIC_KEY_G, PUBLIC_KEY_Y);

    Sketch sketchWithNoConfig =
        Sketch.newBuilder().addRegisters(Register.newBuilder().addIndexes(1)).build();

    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> sketchJavaEncrypter.Encrypt(sketchWithNoConfig));
    assertThat(exception).hasMessageThat().contains("config");
  }
}
