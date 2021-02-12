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

#include <glog/logging.h>

#include <iostream>

#include "absl/flags/flag.h"
#include "absl/flags/parse.h"
#include "absl/strings/escaping.h"
#include "src/main/cc/any_sketch/crypto/sketch_encrypter.h"

ABSL_FLAG(int, curve_id, 0, "The Elliptic curve id.");

ABSL_FLAG(std::vector<std::string>, element_list, {},
          "The list of ElGamal public key elements to combine.");

using ::wfa::common::ElGamalPublicKey;

int main(int argc, char** argv) {
  absl::ParseCommandLine(argc, argv);

  const int curveId = absl::GetFlag(FLAGS_curve_id);
  CHECK(curveId > 0) << "curveId should be greater than 0";
  std::cerr << "curveId: " << curveId << "\n";

  std::vector<ElGamalPublicKey> keys;

  std::cerr << "Keys to combine:\n";
  for (const std::string& y : absl::GetFlag(FLAGS_element_list)) {
    std::cerr << y << "\n";
    keys.emplace_back();
    // The generator g doesn't matter, only set the y component.
    keys.back().set_element(absl::HexStringToBytes(y));
  }

  auto result =
      wfa::any_sketch::crypto::CombineElGamalPublicKeys(curveId, keys).value();
  std::cerr << "\nResult:\n" + absl::BytesToHexString(result.element())
            << std::endl;

  std::cout << absl::BytesToHexString(result.element());
  return 0;
}
