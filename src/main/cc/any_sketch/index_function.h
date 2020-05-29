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
