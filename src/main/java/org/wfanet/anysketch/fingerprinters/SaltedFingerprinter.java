// Copyright 2020 The AnySketch Authors
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

public class SaltedFingerprinter implements Fingerprinter {
  private final Fingerprinter base;
  private final String salt;

  public SaltedFingerprinter(String salt, Fingerprinter base) {
    this.base = base;
    this.salt = salt;
  }

  @Override
  public long fingerprint(String item) {
    return base.fingerprint(applySalt(item));
  }

  private String applySalt(String item) {
    return String.format("AnySketchFingerprint:%s:%s", item, salt);
  }
}
