#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_INDEX_FUNCTION_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_INDEX_FUNCTION_H_

#include <cstdint>

namespace wfa_xmedia {

class IndexFunction {
 public:
  IndexFunction(IndexFunction&& other) = default;
  IndexFunction& operator=(IndexFunction&& other) = default;
  IndexFunction(const IndexFunction&) = delete;
  IndexFunction& operator=(const IndexFunction&) = delete;
  virtual ~IndexFunction() = default;

  virtual uint64_t GetIndex(uint64_t hash) const = 0;

  virtual uint64_t max_value() const = 0;

  virtual uint64_t hash_max_value() const = 0;
};

}  // namespace wfa_xmedia

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_INDEX_FUNCTION_H_
