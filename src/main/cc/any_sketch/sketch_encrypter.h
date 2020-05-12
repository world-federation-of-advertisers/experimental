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

#ifndef ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_ENCRYPTOR_H_
#define ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_ENCRYPTOR_H_

#include "absl/types/span.h"
#include "util/statusor.h"
#include "wfa/measurement/api/v1alpha/sketch.pb.h"

namespace wfa::any_sketch {

// TODO(wangyaopw): update to ::absl::StatusOr when it is open sourced.
using ::private_join_and_compute::StatusOr;

// ElGamal Encryption of an ECPoint m using randomness r, under public key (g,y)
struct CiphertextString {
  std::string u;  // = g^r
  std::string e;  // = m * y^r
};

// Add ElGamal Encryption to plaintext sketch word by word using the same public
// key.
class SketchEncrypter {
 public:
  virtual ~SketchEncrypter() {}
  // Return the word by word ElGamal encryption of the sketch. The result is
  // concatenated as a byte array.
  virtual StatusOr<absl::Span<unsigned char>> Encrypt(
      const wfa::measurement::api::v1alpha::Sketch& sketch) = 0;

 protected:
  SketchEncrypter() = default;
  SketchEncrypter(SketchEncrypter&& other) = delete;
  SketchEncrypter& operator=(SketchEncrypter&& other) = delete;
  SketchEncrypter(const SketchEncrypter&) = delete;
  SketchEncrypter& operator=(const SketchEncrypter&) = delete;
};

// Creates a new SketchEncrypter object using the provided parameters.
// Returns INVALID_ARGUMENT status instead if the curve_id is not valid or
// INTERNAL status when crypto operations are not successful.
//
// parameters
//   curve_id: the id of the elliptical curve to work on.
//   max_counter_value: max decipherable counter value. Greater values are
//     encrypted as the max_counter_value.
//   public_key_bytes: the public key of the ElGamal cipher used for encryption.
StatusOr<std::unique_ptr<SketchEncrypter>> CreateWithPublicKey(
    int curve_id, size_t max_counter_value,
    const CiphertextString& public_key_bytes);

}  // namespace wfa::any_sketch

#endif  // ORG_WFANET_ANYSKETCH_SRC_MAIN_CC_ANY_SKETCH_ENCRYPTOR_H_
