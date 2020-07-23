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

#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_HASH_FUNCTION_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_HASH_FUNCTION_H_

#include "absl/types/span.h"

namespace wfa::any_sketch {

class HashFunction {
 public:
  virtual ~HashFunction() = default;
  HashFunction(HashFunction&&) = default;
  HashFunction& operator=(HashFunction&&) = default;

  virtual uint64_t Fingerprint(absl::Span<const unsigned char> item) const = 0;

 protected:
  HashFunction() = default;
  HashFunction(const HashFunction&) = delete;
  HashFunction& operator=(const HashFunction&) = delete;
};

}  // namespace wfa::any_sketch

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_HASH_FUNCTION_H_
