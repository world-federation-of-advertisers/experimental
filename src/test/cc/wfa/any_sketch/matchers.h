#ifndef SRC_TEST_CC_WFA_ANY_SKETCH_MATCHERS_H_
#define SRC_TEST_CC_WFA_ANY_SKETCH_MATCHERS_H_

#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"

namespace wfa::any_sketch {

MATCHER(IsOk, "") {
  return testing::ExplainMatchResult(true, arg.ok(), result_listener);
}

MATCHER(IsNotOk, "") {
  return testing::ExplainMatchResult(testing::Not(IsOk()), arg,
                                     result_listener);
}

MATCHER_P(IsOkAndHolds, value, "") {
  if (arg.ok()) {
    return testing::ExplainMatchResult(value, arg.value(), result_listener);
  }

  *result_listener << "expected OK status instead of error code "
                   << absl::StatusCodeToString(arg.status().code())
                   << " and message " << arg.status();
  return false;
}
}  // namespace wfa::any_sketch

#endif  // SRC_TEST_CC_WFA_ANY_SKETCH_MATCHERS_H_
