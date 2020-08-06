 // Copyright 2020 The Any Sketch Authors
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


#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_EXPONENTIAL_INDEX_FUNCTION_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_EXPONENTIAL_INDEX_FUNCTION_H_

#include <cstdint>
#include "index_function.h"

namespace wfa::any_sketch {


 // ExponentialIndexFunction maps the output of a HashFunction to a coordinate using
 // exponential distribution with param rate and multiplies the output with param size.
class ExponentialIndexFunction : public IndexFunction {
 public:
   ExponentialIndexFunction(double rate, uint64_t size);
   ExponentialIndexFunction(ExponentialIndexFunction&& other) = default;
   ExponentialIndexFunction& operator=(ExponentialIndexFunction&& other) = default;
   ExponentialIndexFunction(const ExponentialIndexFunction&) = delete;
   ExponentialIndexFunction& operator=(const ExponentialIndexFunction&) = delete;
   ~ExponentialIndexFunction() override = default;

  // Returns the index of the hashmap given the hashed key.
  //
  // This uses inverse CDF of the truncated exponential distribution. To sample
  // from number of legionaries, it multiplies the result with the size.
  uint64_t GetIndex(uint64_t hash) const override;

  uint64_t max_value() const override;

  uint64_t hash_max_value() const override;

 private:
  double rate_;
  uint64_t size_;
  double rate_exponential_;
  };

}  // namespace wfa::any_sketch

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_EXPONENTIAL_INDEX_FUNCTION_H_
