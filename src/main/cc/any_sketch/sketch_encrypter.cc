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

#include "absl/container/flat_hash_map.h"
#include "crypto/commutative_elgamal.h"
#include "crypto/context.h"
#include "crypto/ec_group.h"
#include "sketch_encrypter.h"

namespace wfa::any_sketch {

namespace {
using ::private_join_and_compute::CommutativeElGamal;
using ::private_join_and_compute::Context;
using ::private_join_and_compute::ECGroup;
using ::private_join_and_compute::ECPoint;
using ::private_join_and_compute::InternalError;

class SketchEncrypterImpl : public SketchEncrypter {
 public:
  SketchEncrypterImpl(std::unique_ptr<CommutativeElGamal> el_gamal_cipher,
                      std::unique_ptr<Context> ctx,
                      std::unique_ptr<ECGroup> ec_group,
                      size_t max_counter_value);
  ~SketchEncrypterImpl() override = default;
  SketchEncrypterImpl(SketchEncrypterImpl&& other) = delete;
  SketchEncrypterImpl& operator=(SketchEncrypterImpl&& other) = delete;
  SketchEncrypterImpl(const SketchEncrypterImpl&) = delete;
  SketchEncrypterImpl& operator=(const SketchEncrypterImpl&) = delete;

  StatusOr<absl::Span<unsigned char>> Encrypt(
      const wfa::measurement::api::v1alpha::Sketch& sketch) override;

 private:
  // ElGamal cipher used to do the encryption
  std::unique_ptr<CommutativeElGamal> el_gamal_cipher_;
  // Context used for storing temporary values to be resued across openssl
  // function calls for better performance.
  std::unique_ptr<Context> ctx_;
  // The EC Group representing the curve definition.
  std::unique_ptr<ECGroup> ec_group_;
  // The max distinguishable counter value, all greater values are encrypted as
  // this max_counter_value_.
  size_t max_counter_value_;
  // A cache storing the mapping of integers to their corresponding EC Points.
  // Once a new integer is mapped to the curve, we store the mapping for future
  // reference.
  absl::flat_hash_map<int, ECPoint> integer_to_ec_point_map_;
};

SketchEncrypterImpl::SketchEncrypterImpl(
    std::unique_ptr<CommutativeElGamal> el_gamal_cipher,
    std::unique_ptr<Context> ctx, std::unique_ptr<ECGroup> ec_group,
    size_t max_counter_value)
    : el_gamal_cipher_(std::move(el_gamal_cipher)),
      ctx_(std::move(ctx)),
      ec_group_(std::move(ec_group)),
      max_counter_value_(max_counter_value) {}

StatusOr<absl::Span<unsigned char>> SketchEncrypterImpl::Encrypt(
    const wfa::measurement::api::v1alpha::Sketch& sketch) {
  return InternalError("Unimplemented.");
}

}  // namespace

StatusOr<std::unique_ptr<SketchEncrypter>> CreateWithPublicKey(
    int curve_id, size_t max_counter_value,
    const CiphertextString& public_key_bytes) {
  auto ctx = absl::make_unique<Context>();
  ASSIGN_OR_RETURN(auto temp_ec_group, ECGroup::Create(curve_id, ctx.get()));
  auto ec_group = absl::make_unique<ECGroup>(std::move(temp_ec_group));
  ASSIGN_OR_RETURN(
      auto el_gamal_cipher,
      CommutativeElGamal::CreateFromPublicKey(
          curve_id, std::make_pair(public_key_bytes.e, public_key_bytes.u)));
  std::unique_ptr<SketchEncrypter> result =
      absl::make_unique<SketchEncrypterImpl>(
          std::move(el_gamal_cipher), std::move(ctx), std::move(ec_group),
          max_counter_value);
  return {std::move(result)};
}

}  // namespace wfa::any_sketch