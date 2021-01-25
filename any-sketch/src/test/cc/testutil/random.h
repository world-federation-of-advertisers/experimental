/*
 * Copyright 2020 The Cross-Media Measurement Authors
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

#ifndef SRC_TEST_CC_WFA_TESTUTIL_RANDOM_H_
#define SRC_TEST_CC_WFA_TESTUTIL_RANDOM_H_

#include <cstdint>
#include <limits>

namespace wfa {

int64_t RandomInt64(int64_t max = std::numeric_limits<int64_t>::max());

}  // namespace wfa

#endif  // SRC_TEST_CC_WFA_TESTUTIL_RANDOM_H_
