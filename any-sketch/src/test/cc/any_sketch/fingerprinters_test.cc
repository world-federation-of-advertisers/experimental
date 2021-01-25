// Copyright 2021 The Cross-Media Measurement Authors
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

#include "src/main/cc/any_sketch/fingerprinters.h"

#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "src/test/cc/testutil/matchers.h"

namespace wfa::any_sketch {
namespace {

TEST(FingerprintersTest, Sha256) {
  const Fingerprinter& fingerprinter = GetSha256Fingerprinter();

  // SHA256 of the empty string is:
  // "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855".
  // The first 8 bytes are: "e3 b0 c4 42 98 fc 1c 14".
  // Since we use little endian, we expect to see these bytes in reverse order.
  EXPECT_EQ(fingerprinter.Fingerprint(""), 0x141cfc9842c4b0e3);

  // Verify that different strings give different results.
  EXPECT_NE(fingerprinter.Fingerprint("a-different-str"), 0x141cfc9842c4b0e3);
}

TEST(FingerprintersTest, Farm) {
  const Fingerprinter& fingerprinter = GetFarmFingerprinter();
  EXPECT_EQ(fingerprinter.Fingerprint(""), 0x9ae16a3b2f90404f);

  // Verify that different strings give different results.
  EXPECT_NE(fingerprinter.Fingerprint("not-an-empty-str"), 0x9ae16a3b2f90404f);
}

}  // namespace
}  // namespace wfa::any_sketch
