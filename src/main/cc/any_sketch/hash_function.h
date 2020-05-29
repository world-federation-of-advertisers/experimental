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
