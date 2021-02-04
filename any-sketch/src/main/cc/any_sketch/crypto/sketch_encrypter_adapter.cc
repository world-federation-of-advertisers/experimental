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

#include "sketch_encrypter_adapter.h"

#include "absl/memory/memory.h"
#include "sketch_encrypter.h"
#include "util/status_macros.h"
#include "wfa/any_sketch/crypto/sketch_encryption_methods.pb.h"

namespace wfa::any_sketch::crypto {

absl::StatusOr<std::string> EncryptSketch(
    const std::string& serialized_request) {
  EncryptSketchRequest request_proto;
  if (!request_proto.ParseFromString(serialized_request)) {
    return absl::InvalidArgumentError(
        "failed to parse the EncryptSketchRequest proto.");
  }
  ASSIGN_OR_RETURN(auto sketch_encrypter,
                   CreateWithPublicKey(
                       request_proto.curve_id(), request_proto.maximum_value(),
                       {.u = request_proto.el_gamal_keys().el_gamal_g(),
                        .e = request_proto.el_gamal_keys().el_gamal_y()}));

  EncryptSketchResponse response;
  ASSIGN_OR_RETURN(
      *response.mutable_encrypted_sketch(),
      sketch_encrypter->Encrypt(request_proto.sketch(),
                                request_proto.destroyed_register_strategy()));

  if (request_proto.has_noise_parameter()) {
    RETURN_IF_ERROR(sketch_encrypter->AppendNoiseRegisters(
        request_proto.noise_parameter(),
        request_proto.sketch().config().values_size(),
        *response.mutable_encrypted_sketch()));
  }
  return response.SerializeAsString();
}

absl::StatusOr<std::string> CombineElGamalPublicKeys(
    const std::string& serialized_request) {
  CombineElGamalPublicKeysRequest request_proto;
  if (!request_proto.ParseFromString(serialized_request)) {
    return absl::InvalidArgumentError(
        "failed to parse the CombineElGamalPublicKeysRequest proto.");
  }
  CombineElGamalPublicKeysResponse response;
  std::vector<ElGamalPublicKeys> keys(request_proto.el_gamal_keys().begin(),
                                      request_proto.el_gamal_keys().end());
  ASSIGN_OR_RETURN(*response.mutable_el_gamal_keys(),
                   CombineElGamalPublicKeys(request_proto.curve_id(), keys));
  return response.SerializeAsString();
}

}  // namespace wfa::any_sketch::crypto
