package org.wfanet.anysketch;

import wfa.measurement.api.v1alpha.SketchOuterClass.Sketch;

public final class SketchJavaEncrypter {
  static {
    System.loadLibrary("sketchencrypter");
  }

  private SketchEncrypterJavaAdapter sketchEncrypter;

  SketchJavaEncrypter(int curveId, int maxCounterValue, byte[] publicKeyG, byte[] publicKeyY) {
    sketchEncrypter =
        SketchEncrypterJavaAdapter.CreateFromPublicKeys(
            curveId, maxCounterValue, publicKeyG, publicKeyY);
  }

  byte[] Encrypt(Sketch plainSketch) {
    return sketchEncrypter.Encrypt(plainSketch.toByteArray());
  }
}
