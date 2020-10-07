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

import org.wfanet.anysketch.fingerprinters.Fingerprinter;

public class Distributions {
  private Distributions() {}

  public static Distribution oracle(String featureName, long minValue, long maxValue) {
    return new OracleDistribution(featureName, minValue, maxValue);
  }

  public static Distribution uniform(Fingerprinter fingerprinter, long minValue, long maxValue) {
    return new UniformDistribution(fingerprinter, minValue, maxValue);
  }

  public static Distribution exponential(Fingerprinter fingerprinter, double rate, long size) {
    return new ExponentialDistribution(fingerprinter, rate, size);
  }

  public static Distribution geometric(Fingerprinter fingerprinter, long minValue, long maxValue) {
    return new GeometricDistribution(minValue, maxValue, fingerprinter);
  }
}
