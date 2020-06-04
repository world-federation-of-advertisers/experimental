package org.wfanet.anysketch;

import wfa.measurement.api.v1alpha.SketchOuterClass.Sketch;

public final class SketchJavaEncrypter {

  static {
    try {
      System.loadLibrary("sketchencrypter");
    } catch (UnsatisfiedLinkError e) {
      if (e.getMessage().contains("grte")) {
        throw new RuntimeException(
            "This JNI SketchJavaEncrypter doesn't work with googlejdk.  Use another Java version.");
      } else {
        throw e;
      }
    }
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
