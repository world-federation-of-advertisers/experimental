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

#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_VALUE_FUNCTION_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_VALUE_FUNCTION_H_

#include <cstdint>

#include "absl/strings/string_view.h"

namespace wfa::any_sketch {

class ValueFunction {
 public:
  virtual ~ValueFunction() = default;

  virtual absl::string_view name() const = 0;

  virtual int64_t GetValue(int64_t old_value, int64_t new_value) const = 0;

  virtual int64_t GetInitialValue(int64_t new_value) const = 0;

 protected:
  ValueFunction() = default;
  ValueFunction(const ValueFunction&) = delete;
  ValueFunction& operator=(const ValueFunction&) = delete;
};

}  // namespace wfa::any_sketch

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_VALUE_FUNCTION_H_
