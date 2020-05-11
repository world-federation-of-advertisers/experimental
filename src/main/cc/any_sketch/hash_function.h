#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_HASH_FUNCTION_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_HASH_FUNCTION_H_

#include "absl/types/span.h"

namespace wfa_xmedia {

class HashFunction {
 public:
  HashFunction(HashFunction&& other) = default;
  HashFunction& operator=(HashFunction&& other) = default;
  HashFunction(const HashFunction&) = delete;
  HashFunction& operator=(const HashFunction&) = delete;
  virtual ~HashFunction() = default;

  virtual uint64_t Fingerprint(absl::Span<const unsigned char> item) const = 0;
};

}  // namespace wfa_xmedia

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_HASH_FUNCTION_H_
