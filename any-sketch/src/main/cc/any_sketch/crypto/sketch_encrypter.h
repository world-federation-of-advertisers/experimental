/*
 * Copyright 2020 The Cross-Media Measurement Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SRC_MAIN_CC_ANY_SKETCH_CRYPTO_SKETCH_ENCRYPTER_H_
#define SRC_MAIN_CC_ANY_SKETCH_CRYPTO_SKETCH_ENCRYPTER_H_

#include <vector>

#include "absl/status/statusor.h"
#include "wfa/any_sketch/crypto/sketch_encryption_methods.pb.h"
#include "wfa/measurement/api/v1alpha/sketch.pb.h"

namespace wfa::any_sketch::crypto {

// ElGamal Encryption of an ECPoint m using randomness r, under public key (g,y)
struct CiphertextString {
  std::string u;  // = g^r
  std::string e;  // = m * y^r
};

// Add ElGamal Encryption to plaintext sketch word by word using the same public
// key.
class SketchEncrypter {
 public:
  virtual ~SketchEncrypter() = default;

  SketchEncrypter(SketchEncrypter&& other) = delete;
  SketchEncrypter& operator=(SketchEncrypter&& other) = delete;
  SketchEncrypter(const SketchEncrypter&) = delete;
  SketchEncrypter& operator=(const SketchEncrypter&) = delete;

  // Return the word by word ElGamal encryption of the sketch. The result is
  // the concatenation of all ciphertext strings.
  virtual absl::StatusOr<std::string> Encrypt(
      const wfa::measurement::api::v1alpha::Sketch& sketch,
      EncryptSketchRequest::DestroyedRegisterStrategy
          destroyed_register_strategy) = 0;

 protected:
  SketchEncrypter() = default;
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
absl::StatusOr<std::unique_ptr<SketchEncrypter>> CreateWithPublicKey(
    int curve_id, size_t max_counter_value,
    const CiphertextString& public_key_bytes);

// Combine a vector of ElGamalPublicKeys whose contain the same generator.
absl::StatusOr<ElGamalPublicKeys> CombineElGamalPublicKeys(
    int curve_id, const std::vector<ElGamalPublicKeys>& keys);

}  // namespace wfa::any_sketch::crypto

#endif  // SRC_MAIN_CC_ANY_SKETCH_CRYPTO_SKETCH_ENCRYPTER_H_
