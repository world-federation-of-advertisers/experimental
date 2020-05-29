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

#include "sketch_encrypter.h"

// TODO(wangyaopw): use "external/*" path for external dependencies
#include "absl/container/flat_hash_map.h"
#include "crypto/commutative_elgamal.h"
#include "crypto/context.h"
#include "crypto/ec_group.h"
#include "util/macros.h"

namespace wfa::any_sketch {

namespace {
using ::private_join_and_compute::BigNum;
using ::private_join_and_compute::CommutativeElGamal;
using ::private_join_and_compute::Context;
using ::private_join_and_compute::ECGroup;
using ::private_join_and_compute::ECPoint;
using ::private_join_and_compute::InternalError;
using ::private_join_and_compute::Status;
using ::wfa::measurement::api::v1alpha::Sketch;
using ::wfa::measurement::api::v1alpha::SketchConfig;
using BlindersCiphertext = std::pair<std::string, std::string>;
// Each Compression of ECPoint has size 33-bytes (32 bytes for x, 1 byte for the
// sign of y). An ElGamal ciphertext contains two ECPoints, i.e., u and e.
constexpr int kBytesPerCipherText = 66;
constexpr char KUnitECPointSeed[] = "unit_ec_point";

// Check if the sketch is valid or not.
// A Sketch is valid if and only if all its registers contain the same number
// of indexes and same number of values as the SketchConfig specifies.
bool ValidateSketch(const Sketch& sketch) {
  const int indexes_size = sketch.config().indexes_size();
  const int values_size = sketch.config().values_size();
  for (int i = 0; i < sketch.registers_size(); i++) {
    const Sketch::Register& reg = sketch.registers(i);
    if (indexes_size != reg.indexes_size() ||
        values_size != reg.values_size()) {
      return false;
    }
  }
  return true;
}

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

  StatusOr<std::vector<unsigned char>> Encrypt(const Sketch& sketch) override;

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
  // A cache storing the mapping of integers to their corresponding ECPoints.
  // Once a new integer is mapped to the curve, we store the value for future
  // reference.
  absl::flat_hash_map<uint64_t, std::string> integer_to_ec_point_map_;

  // Encrypt a Register and append the result to the encrypted_sketch.
  StatusOr<bool> EncryptAdditionalRegister(
      const Sketch::Register& reg, const SketchConfig& sketch_config,
      std::vector<unsigned char>& encrypted_sketch);
  // Encrypt an ECPoint and append the result to the encrypted_sketch.
  StatusOr<bool> EncryptAdditionalECPoint(
      const std::string& ec_point,
      std::vector<unsigned char>& encrypted_sketch) const;
  // Lookup the corresponding ECPoint of the input integer in the map.
  // If the ECPoint doesn't exist in the map, calculate it and insert the result
  // to the map. n can not be 0 since there is no string representation of the
  // identity element (Point At Infinity) in the ECGroup.
  StatusOr<std::string> GetECPointForInteger(uint64_t n);
  // Hash a plaintext string to the elliptical curve and return the compressed
  // bytes of the corresponding ECPoint.
  StatusOr<std::string> MapToCurve(const std::string& plaintext);
};

SketchEncrypterImpl::SketchEncrypterImpl(
    std::unique_ptr<CommutativeElGamal> el_gamal_cipher,
    std::unique_ptr<Context> ctx, std::unique_ptr<ECGroup> ec_group,
    size_t max_counter_value)
    : el_gamal_cipher_(std::move(el_gamal_cipher)),
      ctx_(std::move(ctx)),
      ec_group_(std::move(ec_group)),
      max_counter_value_(max_counter_value) {}

StatusOr<std::vector<unsigned char>> SketchEncrypterImpl::Encrypt(
    const Sketch& sketch) {
  if (!ValidateSketch(sketch)) {
    return InternalError("Sketch data doesn't match the config.");
  }
  const int num_registers = sketch.registers_size();
  // number of plaintext(ciphertext) indexes and values in a
  // plaintext(ciphertext) register.
  const int register_size =
      sketch.config().indexes_size() + sketch.config().values_size();
  const int total_cipher_sketch_bytes =
      num_registers * register_size * kBytesPerCipherText;
  std::vector<unsigned char> encrypted_sketch;
  encrypted_sketch.reserve(total_cipher_sketch_bytes);
  for (auto& reg : sketch.registers()) {
    RETURN_IF_ERROR(
        EncryptAdditionalRegister(reg, sketch.config(), encrypted_sketch)
            .status());
  }
  return {std::move(encrypted_sketch)};
}

StatusOr<bool> SketchEncrypterImpl::EncryptAdditionalRegister(
    const Sketch::Register& reg, const SketchConfig& sketch_config,
    std::vector<unsigned char>& encrypted_sketch) {
  for (int64_t index : reg.indexes()) {
    // For indexes, we encrypt the value as a string, since we don't need to do
    // addition on it.
    ASSIGN_OR_RETURN(std::string ec_point_string,
                     MapToCurve(std::to_string(index)));
    EncryptAdditionalECPoint(ec_point_string, encrypted_sketch);
  }
  for (int i = 0; i < reg.values_size(); i++) {
    CiphertextString cipher_index;
    // For values, we encrypt the value as a string if it uses UNIQUE
    // aggregator, or as an integer if it uses SUM aggregator.
    switch (sketch_config.values(i).aggregator()) {
      case SketchConfig::ValueSpec::UNIQUE: {
        ASSIGN_OR_RETURN(std::string ec_point_string,
                         MapToCurve(std::to_string(reg.values(i))));
        EncryptAdditionalECPoint(ec_point_string, encrypted_sketch);
        break;
      }
      case SketchConfig::ValueSpec::SUM: {
        if (reg.values(i) == 0) {
          // 0 is mapping to the PAI (Point At Infinity), which has no string
          // representation. As a result, we cannot first get this ECPoint and
          // then call Encrypt() on it. But instead, we get the encryption of
          // the PAI directly by calling EncryptIdentityElement();
          ASSIGN_OR_RETURN(BlindersCiphertext zero,
                           el_gamal_cipher_->EncryptIdentityElement());
          encrypted_sketch.insert(encrypted_sketch.end(), zero.first.begin(),
                                  zero.first.end());
          encrypted_sketch.insert(encrypted_sketch.end(), zero.second.begin(),
                                  zero.second.end());
        } else {
          // All other values are calculated based on the unit ECPoint P, which
          // is defined by KUnitECPointSeed. Integer n is mapping to nP.
          ASSIGN_OR_RETURN(std::string ec_point_string,
                           GetECPointForInteger(reg.values(i)));
          EncryptAdditionalECPoint(ec_point_string, encrypted_sketch);
        }
        break;
      }
      case SketchConfig::ValueSpec::AGGREGATOR_UNSPECIFIED:
      default:
        return InternalError("Invalid Aggregator type.");
    }
  }
  return true;
}

StatusOr<bool> SketchEncrypterImpl::EncryptAdditionalECPoint(
    const std::string& ec_point,
    std::vector<unsigned char>& encrypted_sketch) const {
  ASSIGN_OR_RETURN(BlindersCiphertext ciphertext,
                   el_gamal_cipher_->Encrypt(ec_point));
  encrypted_sketch.insert(encrypted_sketch.end(), ciphertext.first.begin(),
                          ciphertext.first.end());
  encrypted_sketch.insert(encrypted_sketch.end(), ciphertext.second.begin(),
                          ciphertext.second.end());
  return true;
}

StatusOr<std::string> SketchEncrypterImpl::GetECPointForInteger(
    const uint64_t n) {
  if (auto ec_point = integer_to_ec_point_map_.find(n);
      ec_point != integer_to_ec_point_map_.end()) {
    return ec_point->second;
  }
  if (n > max_counter_value_) {
    return GetECPointForInteger(max_counter_value_);
  }
  std::string ec_point_string;
  if (n == 0) {
    // There is no string representation for 0 (ECPoint At Infinity).
    return InternalError("n shouldn't be 0 for GetECPointForInteger().");
  } else if (n == 1) {
    ASSIGN_OR_RETURN(ec_point_string, MapToCurve(KUnitECPointSeed));
  } else {
    ASSIGN_OR_RETURN(ECPoint ec_1, ec_group_->GetPointByHashingToCurveSha256(
                                       KUnitECPointSeed));
    ASSIGN_OR_RETURN(ECPoint ec_n, ec_1.Mul(ctx_->CreateBigNum(n)));
    ASSIGN_OR_RETURN(ec_point_string, ec_n.ToBytesCompressed());
  }
  // Update the map for future access.
  integer_to_ec_point_map_[n] = ec_point_string;
  return {std::move(ec_point_string)};
}

StatusOr<std::string> SketchEncrypterImpl::MapToCurve(
    const std::string& plaintext) {
  ASSIGN_OR_RETURN(ECPoint ec_point,
                   ec_group_->GetPointByHashingToCurveSha256(plaintext));
  return ec_point.ToBytesCompressed();
}

}  // namespace

StatusOr<std::unique_ptr<SketchEncrypter>> CreateWithPublicKey(
    int curve_id, size_t max_counter_value,
    const CiphertextString& public_key_bytes) {
  auto ctx = absl::make_unique<Context>();
  ASSIGN_OR_RETURN(ECGroup temp_ec_group, ECGroup::Create(curve_id, ctx.get()));
  auto ec_group = absl::make_unique<ECGroup>(std::move(temp_ec_group));
  ASSIGN_OR_RETURN(
      auto el_gamal_cipher,
      CommutativeElGamal::CreateFromPublicKey(
          curve_id, std::make_pair(public_key_bytes.u, public_key_bytes.e)));
  std::unique_ptr<SketchEncrypter> result =
      absl::make_unique<SketchEncrypterImpl>(
          std::move(el_gamal_cipher), std::move(ctx), std::move(ec_group),
          max_counter_value);
  return {std::move(result)};
}

}  // namespace wfa::any_sketch
