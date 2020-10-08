// Copyright 2020 The AnySketch Authors
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

#include "src/main/cc/any_sketch/distributions.h"

#include "absl/types/span.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "src/test/cc/wfa/testutil/matchers.h"

namespace wfa::any_sketch {
namespace {
class FakeFingerprinter : public HashFunction {
 public:
  uint64_t Fingerprint(absl::Span<const unsigned char> item) const override {
    return fingerprint_;
  }

  void SetFingerprint(uint64_t fingerprint) { fingerprint_ = fingerprint; }

 private:
  uint64_t fingerprint_ = 0;
};

TEST(DistributionsTest, OracleDistribution) {
  std::unique_ptr<Distribution> distribution =
      GetOracleDistribution("foo", 3, 10);

  ASSERT_FALSE(distribution == nullptr);

  EXPECT_EQ(distribution->min_value(), 3);
  EXPECT_EQ(distribution->max_value(), 10);

  EXPECT_THAT(distribution->Apply("irrelevant", {{"foo", 5}}), IsOkAndHolds(5));
  EXPECT_THAT(distribution->Apply("irrelevant", {{"foo", 5}, {"bar", 123}}),
              IsOkAndHolds(5));
  EXPECT_THAT(distribution->Apply("irrelevant", {{"foo", 2}}), IsNotOk());
  EXPECT_THAT(distribution->Apply("irrelevant", {{"foo", 11}}), IsNotOk());

  EXPECT_THAT(distribution->Apply("irrelevant", {{"wrong-key", 5}}), IsNotOk());
}

TEST(DistributionsTest, UniformDistribution) {
  FakeFingerprinter fingerprinter;
  std::unique_ptr<Distribution> distribution =
      GetUniformDistribution(&fingerprinter, 3, 10);

  ASSERT_FALSE(distribution == nullptr);

  EXPECT_EQ(distribution->min_value(), 3);
  EXPECT_EQ(distribution->max_value(), 10);

  fingerprinter.SetFingerprint(0);
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(3));

  fingerprinter.SetFingerprint(3);
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(6));

  fingerprinter.SetFingerprint(8);
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(3));

  fingerprinter.SetFingerprint(16);
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(3));
}

TEST(DistributionsTest, ExponentialDistribution) {
  FakeFingerprinter fingerprinter;
  std::unique_ptr<Distribution> distribution =
      GetExponentialDistribution(&fingerprinter, 2, 10);

  ASSERT_FALSE(distribution == nullptr);

  EXPECT_EQ(distribution->min_value(), 0);
  EXPECT_EQ(distribution->max_value(), 9);

  fingerprinter.SetFingerprint(50);
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(0));

  distribution = GetExponentialDistribution(&fingerprinter, 2, 10000);
  fingerprinter.SetFingerprint(5 * std::pow(10, 18));
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(1335));
}

TEST(DistributionsTest, GeometricDistribution) {
  FakeFingerprinter fingerprinter;
  std::unique_ptr<Distribution> distribution =
      GetGeometricDistribution(&fingerprinter, 10, 74);

  ASSERT_FALSE(distribution == nullptr);

  EXPECT_EQ(distribution->min_value(), 10);
  EXPECT_EQ(distribution->max_value(), 74);

  fingerprinter.SetFingerprint(0);
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(74));

  fingerprinter.SetFingerprint(0b1);
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(10));

  fingerprinter.SetFingerprint(0b11110000);
  EXPECT_THAT(distribution->Apply("irrelevant", {}), IsOkAndHolds(14));
}
}  // namespace
}  // namespace wfa::any_sketch
