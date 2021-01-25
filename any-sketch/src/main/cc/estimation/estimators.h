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

#ifndef SRC_MAIN_CC_WFA_ESTIMATION_ESTIMATON_UTILS_H_
#define SRC_MAIN_CC_WFA_ESTIMATION_ESTIMATON_UTILS_H_

#include <cstdint>

namespace wfa::estimation {

// Estimates cardinality of a liquid legions sketch given the decay rate,
// size and number of non-empty registers for that sketch.
int64_t EstimateCardinalityLiquidLegions(double decay_rate, uint64_t size,
                                         uint64_t active_register_count);

}  // namespace wfa::estimation

#endif  // SRC_MAIN_CC_WFA_ESTIMATION_ESTIMATON_UTILS_H_
