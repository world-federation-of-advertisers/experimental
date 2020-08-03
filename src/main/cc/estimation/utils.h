/*
 * Copyright 2020 The Any Sketch Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ADS_XMEDIA_LANCELOT_ESTIMATON_UTILS_H_
#define ADS_XMEDIA_LANCELOT_ESTIMATON_UTILS_H_

#include <cstdint>

namespace wfa::estimation {

// Estimates cardinality of a liquid legions sketch given the decay_rate,
// size and active_register_count for that sketch.

uint64_t EstimateCardinalityLiquidLegions(double decay_rate, uint64_t size,
                                          uint64_t active_register_count);

}  // namespace wfa::estimation

#endif  // ADS_XMEDIA_LANCELOT_ESTIMATON_UTILS_H_