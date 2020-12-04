// Copyright 2020 The Any Sketch Authors
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

#include "sketch_encrypter.h"

// TODO(wangyaopw): use "external/*" path for external dependencies
#include "absl/container/flat_hash_map.h"
#include "crypto/commutative_elgamal.h"
#include "crypto/context.h"
#include "crypto/ec_group.h"
#include "src/main/cc/any_sketch/util/macros.h"
#include "wfa/any_sketch/crypto/sketch_encryption_methods.pb.h"

namespace wfa::any_sketch::crypto {

namespace {
using ::private_join_and_compute::BigNum;
using ::private_join_and_compute::CommutativeElGamal;
using ::private_join_and_compute::Context;
using ::private_join_and_compute::ECGroup;
using ::private_join_and_compute::ECPoint;
using ::wfa::measurement::api::v1alpha::Sketch;
using ::wfa::measurement::api::v1alpha::SketchConfig;
using DestroyedRegisterStrategy =
    ::wfa::any_sketch::crypto::EncryptSketchRequest::DestroyedRegisterStrategy;
using BlindersCiphertext = std::pair<std::string, std::string>;
// Each Compression of ECPoint has size 33-bytes (32 bytes for x, 1 byte for the
// sign of y). An ElGamal ciphertext contains two ECPoints, i.e., u and e.
constexpr int kBytesPerCipherText = 66;
constexpr absl::string_view KUnitECPointSeed = "unit_ec_point";
constexpr absl::string_view KDestroyedRegisterKey = "destroyed_register_key";

// Check if the sketch is valid or not.
// A Sketch is valid if and only if all its registers contain the same number
// of indexes and same number of values as the SketchConfig specifies.
bool ValidateSketch(const Sketch& sketch) {
  const int values_size = sketch.config().values_size();
  for (int i = 0; i < sketch.registers_size(); i++) {
    const Sketch::Register& reg = sketch.registers(i);
    if (values_size != reg.values_size()) {
      return false;
    }
  }
  return true;
}

// Check if a register is destroyed, i.e., if any UNIQUE value is equal to -1
bool IsRegisterDestroyed(const Sketch::Register& reg,
                         const SketchConfig& sketch_config) {
  for (int i = 0; i < reg.values_size(); i++) {
    if (sketch_config.values(i).aggregator() ==
            SketchConfig::ValueSpec::UNIQUE &&
        reg.values(i) <= 0)
      return true;
  }
  return false;
}

// Add ElGamal Encryption to plaintext sketch word by word using the same public
// key.
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

  absl::StatusOr<std::string> Encrypt(
      const Sketch& sketch,
      DestroyedRegisterStrategy destroyed_register_strategy) override;

 private:
  // ElGamal cipher used to do the encryption
  std::unique_ptr<CommutativeElGamal> el_gamal_cipher_;
  // Context used for storing temporary values to be reused across openssl
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
  // The cached ECPoint representation of constant "KDestroyedRegisterKey"
  std::string destroyed_register_key_ec_;

  // Since the underlying private-join-and-computer::CommutativeElGamal is NOT
  // thread safe, we use mutex to enforce thread safety in this class.
  absl::Mutex mutex_;

  // Append an encrypted register with all values equal to a provided number
  // to the sketch.
  absl::Status AppendEncryptedRegisterWithSameValue(
      absl::string_view index_ec, size_t num_of_values, int n,
      std::string& encrypted_sketch);
  // Append an encrypted destroyed register with all values equal to the
  // encryption of the KDestroyedRegisterKey constant.
  absl::Status AppendFlaggedDestroyedRegister(absl::string_view index_ec,
                                              size_t num_of_values,
                                              std::string& encrypted_sketch);
  // Encrypt a destroyed register by inserting a pair of registers with the
  // same actual index but different values.
  absl::Status EncryptDestroyedRegister(
      const Sketch::Register& reg,
      DestroyedRegisterStrategy destroyed_register_strategy,
      std::string& encrypted_sketch);
  // Encrypt a non-destroyed register according to the exact values.
  absl::Status EncryptNonDestroyedRegister(const Sketch::Register& reg,
                                           const SketchConfig& sketch_config,
                                           std::string& encrypted_sketch);
  // Encrypt a Register and append the result to the encrypted_sketch.
  absl::Status EncryptAdditionalRegister(
      const Sketch::Register& reg, const SketchConfig& sketch_config,
      DestroyedRegisterStrategy destroyed_register_strategy,
      std::string& encrypted_sketch);
  // Encrypt an ECPoint and append the result to the encrypted_sketch.
  absl::Status EncryptAdditionalECPoint(absl::string_view ec_point,
                                        std::string& encrypted_sketch) const;
  // Lookup the corresponding ECPoint of the input integer in the map.
  // If the ECPoint doesn't exist in the map, calculate it and insert the result
  // to the map. n can not be 0 since there is no string representation of the
  // identity element (Point At Infinity) in the ECGroup.
  absl::StatusOr<std::string> GetECPointForInteger(uint64_t n);
  // Hash a plaintext string to the elliptical curve and return the compressed
  // bytes of the corresponding ECPoint.
  absl::StatusOr<std::string> MapToCurve(absl::string_view plaintext);
  // Hash a plaintext integer to the elliptical curve and return the compressed
  // bytes of the corresponding ECPoint.
  absl::StatusOr<std::string> MapToCurve(int64_t plaintext);
};

SketchEncrypterImpl::SketchEncrypterImpl(
    std::unique_ptr<CommutativeElGamal> el_gamal_cipher,
    std::unique_ptr<Context> ctx, std::unique_ptr<ECGroup> ec_group,
    size_t max_counter_value)
    : el_gamal_cipher_(std::move(el_gamal_cipher)),
      ctx_(std::move(ctx)),
      ec_group_(std::move(ec_group)),
      max_counter_value_(max_counter_value) {}

absl::StatusOr<std::string> SketchEncrypterImpl::Encrypt(
    const Sketch& sketch,
    DestroyedRegisterStrategy destroyed_register_strategy) {
  // Lock the mutex since most of the crypto computations here are NOT
  // thread-safe.
  absl::WriterMutexLock l(&mutex_);
  if (!ValidateSketch(sketch)) {
    return absl::InternalError("Sketch data doesn't match the config.");
  }
  const int num_registers = sketch.registers_size();
  // number of plaintext(ciphertext) indexes and values in a
  // plaintext(ciphertext) register.
  const int register_size =
      sketch.config().indexes_size() + sketch.config().values_size();
  const int total_cipher_sketch_bytes =
      num_registers * register_size * kBytesPerCipherText;
  std::string encrypted_sketch;
  encrypted_sketch.reserve(total_cipher_sketch_bytes);
  for (auto& reg : sketch.registers()) {
    RETURN_IF_ERROR(EncryptAdditionalRegister(
        reg, sketch.config(), destroyed_register_strategy, encrypted_sketch));
  }
  return encrypted_sketch;
}

absl::Status SketchEncrypterImpl::AppendEncryptedRegisterWithSameValue(
    absl::string_view index_ec, size_t num_of_values, int n,
    std::string& encrypted_sketch) {
  RETURN_IF_ERROR(EncryptAdditionalECPoint(index_ec, encrypted_sketch));
  ASSIGN_OR_RETURN(std::string value_ec, GetECPointForInteger(n));
  for (size_t i = 0; i < num_of_values; ++i) {
    RETURN_IF_ERROR(EncryptAdditionalECPoint(value_ec, encrypted_sketch));
  }
  return absl::OkStatus();
}

absl::Status SketchEncrypterImpl::AppendFlaggedDestroyedRegister(
    absl::string_view index_ec, size_t num_of_values,
    std::string& encrypted_sketch) {
  RETURN_IF_ERROR(EncryptAdditionalECPoint(index_ec, encrypted_sketch));
  if (destroyed_register_key_ec_.empty()) {
    // Do the mapping if there is no cached value.
    ASSIGN_OR_RETURN(destroyed_register_key_ec_,
                     MapToCurve(KDestroyedRegisterKey));
  }
  for (size_t i = 0; i < num_of_values; ++i) {
    RETURN_IF_ERROR(
        EncryptAdditionalECPoint(destroyed_register_key_ec_, encrypted_sketch));
  }
  return absl::OkStatus();
}

absl::Status SketchEncrypterImpl::EncryptDestroyedRegister(
    const Sketch::Register& reg,
    DestroyedRegisterStrategy destroyed_register_strategy,
    std::string& encrypted_sketch) {
  ASSIGN_OR_RETURN(std::string index_ec, MapToCurve(reg.index()));
  switch (destroyed_register_strategy) {
    case EncryptSketchRequest::CONFLICTING_KEYS:
      // Add two registers with the same index for a destroyed register but
      // different values.
      RETURN_IF_ERROR(AppendEncryptedRegisterWithSameValue(
          index_ec, reg.values_size(), 1, encrypted_sketch));
      RETURN_IF_ERROR(AppendEncryptedRegisterWithSameValue(
          index_ec, reg.values_size(), 2, encrypted_sketch));
      break;
    case EncryptSketchRequest::FLAGGED_KEY:
      // ADD one register with all values being the encryption of the
      // KDestroyedRegisterKey constant.
      RETURN_IF_ERROR(AppendFlaggedDestroyedRegister(
          index_ec, reg.values_size(), encrypted_sketch));
      break;
    default:
      return absl::InvalidArgumentError("Invalid DestroyedRegisterStrategy.");
  }
  return absl::OkStatus();
}

absl::Status SketchEncrypterImpl::EncryptNonDestroyedRegister(
    const Sketch::Register& reg, const SketchConfig& sketch_config,
    std::string& encrypted_sketch) {
  // We encrypt the index as a string, since we don't need to do
  // addition on it.
  ASSIGN_OR_RETURN(std::string index_ec, MapToCurve(reg.index()));
  RETURN_IF_ERROR(EncryptAdditionalECPoint(index_ec, encrypted_sketch));

  for (int i = 0; i < reg.values_size(); ++i) {
    // For values, we encrypt the value as a string if it uses UNIQUE
    // aggregator, or as an integer if it uses SUM aggregator.
    switch (sketch_config.values(i).aggregator()) {
      case SketchConfig::ValueSpec::UNIQUE: {
        ASSIGN_OR_RETURN(std::string ec_point_string,
                         MapToCurve(reg.values(i)));
        RETURN_IF_ERROR(
            EncryptAdditionalECPoint(ec_point_string, encrypted_sketch));
        break;
      }
      case SketchConfig::ValueSpec::SUM: {
        if (reg.values(i) <= 0) {
          return absl::InvalidArgumentError("A SUM value should be positive.");
        } else {
          // All other values are calculated based on the unit ECPoint P, which
          // is defined by KUnitECPointSeed. Integer n is mapping to nP.
          ASSIGN_OR_RETURN(std::string ec_point_string,
                           GetECPointForInteger(reg.values(i)));
          RETURN_IF_ERROR(
              EncryptAdditionalECPoint(ec_point_string, encrypted_sketch));
        }
        break;
      }
      case SketchConfig::ValueSpec::AGGREGATOR_UNSPECIFIED:
      default:
        return absl::InvalidArgumentError("Invalid Aggregator type.");
    }
  }
  return absl::OkStatus();
}

absl::Status SketchEncrypterImpl::EncryptAdditionalRegister(
    const Sketch::Register& reg, const SketchConfig& sketch_config,
    DestroyedRegisterStrategy destroyed_register_strategy,
    std::string& encrypted_sketch) {
  if (IsRegisterDestroyed(reg, sketch_config)) {
    return EncryptDestroyedRegister(reg, destroyed_register_strategy,
                                    encrypted_sketch);
  } else {
    return EncryptNonDestroyedRegister(reg, sketch_config, encrypted_sketch);
  }
}

absl::Status SketchEncrypterImpl::EncryptAdditionalECPoint(
    absl::string_view ec_point, std::string& encrypted_sketch) const {
  ASSIGN_OR_RETURN(BlindersCiphertext ciphertext,
                   el_gamal_cipher_->Encrypt(ec_point));
  encrypted_sketch.append(ciphertext.first);
  encrypted_sketch.append(ciphertext.second);
  return absl::OkStatus();
}

absl::StatusOr<std::string> SketchEncrypterImpl::GetECPointForInteger(
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
    return absl::InternalError("n shouldn't be 0 for GetECPointForInteger().");
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

absl::StatusOr<std::string> SketchEncrypterImpl::MapToCurve(
    absl::string_view plaintext) {
  ASSIGN_OR_RETURN(ECPoint ec_point,
                   ec_group_->GetPointByHashingToCurveSha256(plaintext));
  return ec_point.ToBytesCompressed();
}

absl::StatusOr<std::string> SketchEncrypterImpl::MapToCurve(int64_t plaintext) {
  return MapToCurve(std::to_string(plaintext));
}

}  // namespace

absl::StatusOr<std::unique_ptr<SketchEncrypter>> CreateWithPublicKey(
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

absl::StatusOr<ElGamalPublicKeys> CombineElGamalPublicKeys(
    int curve_id, const std::vector<ElGamalPublicKeys>& keys) {
  if (keys.empty()) {
    return absl::InvalidArgumentError("Keys cannot be empty");
  }
  if (keys.size() == 1) {
    return keys[0];
  }

  ElGamalPublicKeys result;
  result.set_el_gamal_g(keys[0].el_gamal_g());

  Context ctx;
  ASSIGN_OR_RETURN_ERROR(ECGroup ec_group, ECGroup::Create(curve_id, &ctx),
                         absl::StrCat("Invalid Curve_id: ", curve_id));
  ASSIGN_OR_RETURN_ERROR(
      ECPoint combined_element_ec, ec_group.CreateECPoint(keys[0].el_gamal_y()),
      absl::StrCat("Invalid ECPoint: ", keys[0].el_gamal_y()));

  for (size_t i = 1; i < keys.size(); i++) {
    if (keys[i].el_gamal_g() != result.el_gamal_g()) {
      return absl::InvalidArgumentError(
          absl::StrCat("Generators don't match", keys[i].el_gamal_g(), " vs ",
                       result.el_gamal_g()));
    }
    ASSIGN_OR_RETURN_ERROR(
        ECPoint element_ec, ec_group.CreateECPoint(keys[i].el_gamal_y()),
        absl::StrCat("Invalid ECPoint: ", keys[i].el_gamal_y()));
    ASSIGN_OR_RETURN(combined_element_ec, combined_element_ec.Add(element_ec));
  }
  ASSIGN_OR_RETURN(*result.mutable_el_gamal_y(),
                   combined_element_ec.ToBytesCompressed());
  return std::move(result);
}

}  // namespace wfa::any_sketch::crypto
