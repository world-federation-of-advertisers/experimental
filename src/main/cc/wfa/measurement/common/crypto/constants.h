// Copyright 2020 The Measurement System Authors
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

#ifndef WFA_MEASUREMENT_COMMON_CRYPTO_CONSTANTS_H_
#define WFA_MEASUREMENT_COMMON_CRYPTO_CONSTANTS_H_

namespace wfa::measurement::common::crypto {

inline constexpr int kBytesPerEcPoint = 33;
// A ciphertext contains 2 EcPoints.
inline constexpr int kBytesPerCipherText = kBytesPerEcPoint * 2;
// A register contains 3 ciphertexts, i.e., (index, key, count)
inline constexpr int kBytesPerCipherRegister = kBytesPerCipherText * 3;
// The seed for the IsNotDestoryed flag.
inline constexpr char kIsNotDestroyed[] = "IsNotDestroyed";
inline constexpr char KUnitECPointSeed[] = "unit_ec_point";

}  // namespace wfa::measurement::common::crypto

#endif  // WFA_MEASUREMENT_COMMON_CRYPTO_CONSTANTS_H_
