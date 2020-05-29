/*
 * Copyright 2020 Google Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SRC_MAIN_CC_ANY_SKETCH_SKETCH_ENCRYPTER_JAVA_ADAPTER_H_
#define SRC_MAIN_CC_ANY_SKETCH_SKETCH_ENCRYPTER_JAVA_ADAPTER_H_

#include "absl/memory/memory.h"
#include "sketch_encrypter.h"
#include "util/canonical_errors.h"
#include "util/status_macros.h"
#include "util/statusor.h"

namespace wfa::any_sketch {

// A help class used to create a JNI wrapper on the SketchEncrypter.
// The interface, i.e., inputs and outputs, are simplified such that it is
// easier to define the swig wrapper.
// Note: this class shouldn't be used in any c++ binary, use SketchEncrypter
// directly.
class SketchEncrypterJavaAdapter {
 public:
  SketchEncrypterJavaAdapter(std::unique_ptr<SketchEncrypter> sketch_encrypter)
      : sketch_encrypter_(std::move(sketch_encrypter)) {}
  SketchEncrypterJavaAdapter(SketchEncrypterJavaAdapter&& other) = delete;
  SketchEncrypterJavaAdapter& operator=(SketchEncrypterJavaAdapter&& other) =
      delete;
  SketchEncrypterJavaAdapter(const SketchEncrypterJavaAdapter&) = delete;
  SketchEncrypterJavaAdapter& operator=(const SketchEncrypterJavaAdapter&) =
      delete;

  static private_join_and_compute::StatusOr<
      std::unique_ptr<SketchEncrypterJavaAdapter>>
  CreateFromPublicKeys(int curve_id, int max_counter_value,
                       const std::string& public_key_u,
                       const std::string& public_key_e) {
    ASSIGN_OR_RETURN(
        auto sketch_encrypter,
        CreateWithPublicKey(curve_id, max_counter_value,
                            {.u = public_key_u, .e = public_key_e}));
    auto result = absl::make_unique<SketchEncrypterJavaAdapter>(
        std::move(sketch_encrypter));
    return {std::move(result)};
  }

  // Encrypt a Sketch proto.
  // The input should be the serialized proto.
  // The output is a c++ string, which would be converted to a java byte[].
  private_join_and_compute::StatusOr<std::string> Encrypt(
      const std::string& serialized_sketch) const {
    wfa::measurement::api::v1alpha::Sketch sketch;
    if (!sketch.ParseFromString(serialized_sketch)) {
      return ::private_join_and_compute::InternalError(
          "failed to parse the sketch proto.");
    }
    ASSIGN_OR_RETURN(auto encrypted_sketch, sketch_encrypter_->Encrypt(sketch));
    std::string result(encrypted_sketch.begin(), encrypted_sketch.end());
    return result;
  }

 private:
  std::unique_ptr<SketchEncrypter> sketch_encrypter_;
};

}  // namespace wfa::any_sketch

#endif  // SRC_MAIN_CC_ANY_SKETCH_SKETCH_ENCRYPTER_JAVA_ADAPTER_H_
