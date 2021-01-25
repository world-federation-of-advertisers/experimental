// Copyright 2020 The Cross-Media Measurement Authors
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

import org.wfanet.anysketch.aggregators.Aggregator;
import org.wfanet.anysketch.distributions.Distribution;

/**
 * Represents how register values are updated. An AnySketch with k values per register must be
 * initialized with k ValueFunctions.
 *
 * <p>TODO: rename ValueFunction to something more appropriate -- it's not really a function.
 */
class ValueFunction {
  private Aggregator aggregator;
  private Distribution distribution;
  private String name;

  /**
   * Constructs a ValueFunction.
   *
   * @param name name of the value function
   * @param aggregator how to combine values
   * @param distribution how to transform items and user-provided data to form a register value
   */
  public ValueFunction(String name, Aggregator aggregator, Distribution distribution) {
    this.name = name;
    this.aggregator = aggregator;
    this.distribution = distribution;
  }

  public String getName(){
    return name;
  }

  public Aggregator getAggregator() {
    return aggregator;
  }

  public Distribution getDistribution() {
    return distribution;
  }
}
