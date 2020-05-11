#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_VALUE_FUNCTION_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_VALUE_FUNCTION_H_

#include <cstdint>

#include "absl/strings/string_view.h"

namespace wfa_xmedia {

class ValueFunction {
 public:
  ValueFunction(ValueFunction&& other) = default;
  ValueFunction& operator=(ValueFunction&& other) = default;
  ValueFunction(const ValueFunction&) = delete;
  ValueFunction& operator=(const ValueFunction&) = delete;
  virtual ~ValueFunction() = default;

  virtual absl::string_view name() const = 0;

  virtual int64_t GetValue(int64_t old_value, int64_t new_value) const = 0;

  virtual int64_t GetInitialValue(int64_t new_value) const = 0;
};

}  // namespace wfa_xmedia

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_VALUE_FUNCTION_H_
