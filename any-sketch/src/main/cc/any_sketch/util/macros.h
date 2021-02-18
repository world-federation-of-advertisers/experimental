// Copyright 2020 The Cross-Media Measurement Authors
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

#include <utility>

#ifndef SRC_MAIN_CC_ANY_SKETCH_UTIL_MACROS_H_
#define SRC_MAIN_CC_ANY_SKETCH_UTIL_MACROS_H_

#ifndef ASSIGN_OR_RETURN_ERROR
#define ASSIGN_OR_RETURN_ERROR(lhs, rexpr, message)                    \
  SRC_MAIN_CC_ANY_SKETCH_UTIL_ASSIGN_OR_RETURN_ERROR_IMPL_(            \
      SRC_MAIN_CC_ANY_SKETCH_UTIL_MACROS_IMPL_CONCAT_(status_or_value, \
                                                      __COUNTER__),    \
      lhs, rexpr, message)
#endif

// Internal helper.
#define SRC_MAIN_CC_ANY_SKETCH_UTIL_ASSIGN_OR_RETURN_ERROR_IMPL_( \
    statusor, lhs, rexpr, message)                                \
  auto statusor = (rexpr);                                        \
  if (ABSL_PREDICT_FALSE(!statusor.ok())) {                       \
    return absl::InvalidArgumentError(message);                   \
  }                                                               \
  lhs = std::move(statusor).value()

// Internal helper for concatenating macro values.
#define SRC_MAIN_CC_ANY_SKETCH_UTIL_MACROS_IMPL_CONCAT_INNER_(x, y) x##y
#define SRC_MAIN_CC_ANY_SKETCH_UTIL_MACROS_IMPL_CONCAT_(x, y) \
  SRC_MAIN_CC_ANY_SKETCH_UTIL_MACROS_IMPL_CONCAT_INNER_(x, y)

#endif  // SRC_MAIN_CC_ANY_SKETCH_UTIL_MACROS_H_
