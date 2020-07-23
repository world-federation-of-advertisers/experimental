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

#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_INDEX_FUNCTION_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_INDEX_FUNCTION_H_

#include <cstdint>

namespace wfa::any_sketch {

class IndexFunction {
 public:
  virtual ~IndexFunction() = default;
  IndexFunction(const IndexFunction&) = delete;

  virtual uint64_t GetIndex(uint64_t hash) const = 0;

  virtual uint64_t max_value() const = 0;

  virtual uint64_t hash_max_value() const = 0;

 protected:
  IndexFunction() = default;
  IndexFunction& operator=(const IndexFunction&) = delete;
};

}  // namespace wfa::any_sketch

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_INDEX_FUNCTION_H_
