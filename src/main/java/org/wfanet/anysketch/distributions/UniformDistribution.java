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

package org.wfanet.anysketch.distributions;

import java.util.Map;
import org.wfanet.anysketch.fingerprinters.Fingerprinter;

class UniformDistribution extends FingerprintingDistribution {
  UniformDistribution(Fingerprinter fingerprinter, long min, long max) {
    super(min, max, fingerprinter);
  }

  @Override
  protected long applyToFingerprint(long fingerprint, Map<String, Long> itemMetadata) {
    return Long.remainderUnsigned(fingerprint, getSize()) + getMinValue();
  }
}
