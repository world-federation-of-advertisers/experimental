#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_FARM_HASH_FUNCTION_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_FARM_HASH_FUNCTION_H_

#include <cstdint>

#include "hash_function.h"
#include "absl/types/span.h"

namespace wfa_xmedia {

class FarmHashFunction : public HashFunction {
 public:
  FarmHashFunction(FarmHashFunction&& other) = default;
  FarmHashFunction& operator=(FarmHashFunction&& other) = default;
  FarmHashFunction(const FarmHashFunction&) = delete;
  FarmHashFunction& operator=(const FarmHashFunction&) = delete;
  ~FarmHashFunction() override = default;

  uint64_t Fingerprint(absl::Span<const unsigned char> item) const override;
};

}  // namespace wfa_xmedia

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_FARM_HASH_FUNCTION_H_
