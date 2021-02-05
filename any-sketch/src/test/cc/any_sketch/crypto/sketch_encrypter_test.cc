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

#include "src/main/cc/any_sketch/crypto/sketch_encrypter.h"

#include <openssl/obj_mac.h>

#include "absl/types/span.h"
// TODO(wangyaopw): use "external/*" path for blinders headers
#include "absl/strings/escaping.h"
#include "absl/strings/string_view.h"
#include "crypto/commutative_elgamal.h"
#include "gmock/gmock.h"
#include "google/protobuf/util/message_differencer.h"
#include "gtest/gtest.h"
#include "src/test/cc/testutil/matchers.h"
#include "src/test/cc/testutil/random.h"
#include "src/test/cc/testutil/status_macros.h"

namespace wfa::any_sketch::crypto {
namespace {

using ::private_join_and_compute::CommutativeElGamal;
using ::private_join_and_compute::Context;
using ::private_join_and_compute::ECGroup;
using ::private_join_and_compute::ECPoint;
using ::testing::Not;
using ::testing::SizeIs;
using ::wfa::measurement::api::v1alpha::Sketch;
using ::wfa::measurement::api::v1alpha::SketchConfig;

constexpr int kTestCurveId = NID_X9_62_prime256v1;
constexpr int kMaxCounterValue = 100;

constexpr char kElGamalPublicKeyG[] =
    "036b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296";
constexpr char kElGamalPublicKeyY1[] =
    "02d1432ca007a6c6d739fce2d21feb56d9a2c35cf968265f9093c4b691e11386b3";
constexpr char kElGamalPublicKeyY2[] =
    "039ef370ff4d216225401781d88a03f5a670a5040e6333492cb4e0cd991abbd5a3";
constexpr char kElGamalPublicKeyY3[] =
    "02d0f25ab445fc9c29e7e2509adc93308430f432522ffa93c2ae737ceb480b66d7";
constexpr char kCombinedElGamalPublicKeyY[] =
    "02505d7b3ac4c3c387c74132ab677a3421e883b90d4c83dc766e400fe67acc1f04";

// TODO: use protocol buffer matchers when they are available
// Returns true if the decryption of expected is the same with that of arg.
MATCHER_P2(HasSameDecryption, original_cipher, expected, "") {
  absl::StatusOr<std::string> decrypted_actual =
      original_cipher->Decrypt(std::make_pair(arg.u, arg.e));
  if (!decrypted_actual.ok()) {
    *result_listener << "cannot decrypt ciphertext: " << arg.u << arg.e << ": "
                     << decrypted_actual.status();
    return false;
  }
  absl::StatusOr<std::string> decrypted_expected =
      original_cipher->Decrypt(std::make_pair(expected.u, expected.e));
  if (!decrypted_expected.ok()) {
    *result_listener << "cannot decrypt ciphertext: " << expected.u
                     << expected.e << ": " << decrypted_expected.status();
    return false;
  }
  return ExplainMatchResult(testing::Eq(decrypted_expected.value()),
                            decrypted_actual.value(), result_listener);
}

// Returns true if the arg is an encryption of the provided plaintext.
MATCHER_P2(IsEncryptionOf, original_cipher, plaintext, "") {
  absl::StatusOr<std::string> decrypted =
      original_cipher->Decrypt(std::make_pair(arg.u, arg.e));
  if (!decrypted.ok()) {
    *result_listener << "cannot decrypt ciphertext: " << arg.u << arg.e << ": "
                     << decrypted.status();
    return false;
  }
  Context context;
  std::string plaintext_ec = ECGroup::Create(kTestCurveId, &context)
                                 .value()
                                 .GetPointByHashingToCurveSha256(plaintext)
                                 .value()
                                 .ToBytesCompressed()
                                 .value();
  return ExplainMatchResult(testing::Eq(decrypted.value()), plaintext_ec,
                            result_listener);
}

// Returns true if two proto messages are equal when ignoring the order of
// repeated fields.
MATCHER_P(EqualsProto, expected, "") {
  ::google::protobuf::util::MessageDifferencer differencer;
  differencer.set_repeated_field_comparison(
      ::google::protobuf::util::MessageDifferencer::AS_SET);
  return differencer.Compare(arg, expected);
}

// Helper function to create a SketchConfig.
SketchConfig CreateSketchConfig(const int unique_cnt, const int sum_cnt) {
  SketchConfig sketch_config;
  for (int i = 0; i < unique_cnt; ++i) {
    sketch_config.add_values()->set_aggregator(SketchConfig::ValueSpec::UNIQUE);
  }
  for (int i = 0; i < sum_cnt; ++i) {
    sketch_config.add_values()->set_aggregator(SketchConfig::ValueSpec::SUM);
  }
  return sketch_config;
}

// Helper function to add random registers to a sketch according to its
// sketchConfig.
void AddRandomRegisters(const int register_cnt, Sketch& sketch) {
  for (int i = 0; i < register_cnt; ++i) {
    Sketch::Register* last_register = sketch.add_registers();
    last_register->set_index(RandomInt64());
    for (int value_i = 0; value_i < sketch.config().values_size(); ++value_i) {
      // The Aggregate type doesn't matter here.
      // Mod(kMaxCounterValue * 2) so we have some but not too many values
      // exceed the max.
      last_register->add_values(RandomInt64(kMaxCounterValue * 2) + 1);
    }
  }
}

// Partition the char vector 33 by 33, and convert the results to strings
std::vector<std::string> GetCipherStrings(absl::string_view bytes) {
  EXPECT_EQ(bytes.size() % 66, 0);
  std::vector<std::string> result;
  int word_cnt = bytes.size() / 33;
  result.reserve(word_cnt);
  for (int i = 0; i < word_cnt; ++i) {
    result.emplace_back(bytes.substr(i * 33, 33));
  }
  return result;
}

// Add two ElGamal ciphertexts on the specified ECGroup.
absl::StatusOr<CiphertextString> AddCiphertext(const CiphertextString& a,
                                               const CiphertextString& b,
                                               const ECGroup& ec_group) {
  CiphertextString result;
  ASSIGN_OR_RETURN(ECPoint a_u, ec_group.CreateECPoint(a.u));
  ASSIGN_OR_RETURN(ECPoint b_u, ec_group.CreateECPoint(b.u));
  ASSIGN_OR_RETURN(ECPoint sum_u, a_u.Add(b_u));
  ASSIGN_OR_RETURN(result.u, sum_u.ToBytesCompressed());
  ASSIGN_OR_RETURN(ECPoint a_e, ec_group.CreateECPoint(a.e));
  ASSIGN_OR_RETURN(ECPoint b_e, ec_group.CreateECPoint(b.e));
  ASSIGN_OR_RETURN(ECPoint sum_e, a_e.Add(b_e));
  ASSIGN_OR_RETURN(result.e, sum_e.ToBytesCompressed());

  return std::move(result);
}

class SketchEncrypterTest : public ::testing::Test {
 protected:
  SketchEncrypterTest() {}

  void SetUp() {
    ASSERT_OK_AND_ASSIGN(
        original_cipher_,
        CommutativeElGamal::CreateWithNewKeyPair(kTestCurveId));
    ASSERT_OK_AND_ASSIGN(auto public_key_pair,
                         original_cipher_->GetPublicKeyBytes());
    CiphertextString public_key = {
        .u = public_key_pair.first,
        .e = public_key_pair.second,
    };
    ASSERT_OK_AND_ASSIGN(
        sketch_encrypter_,
        CreateWithPublicKey(kTestCurveId, kMaxCounterValue, public_key));
  }

  absl::StatusOr<std::string> EncryptWithConflictingKeys(const Sketch& sketch) {
    return sketch_encrypter_->Encrypt(sketch,
                                      EncryptSketchRequest::CONFLICTING_KEYS);
  }

  absl::StatusOr<std::string> EncryptWithFlaggedKey(const Sketch& sketch) {
    return sketch_encrypter_->Encrypt(sketch,
                                      EncryptSketchRequest::FLAGGED_KEY);
  }

  // The ElGamal Cipher whose public key is used to create the SketchEncrypter.
  std::unique_ptr<CommutativeElGamal> original_cipher_;
  // The SketchEncrypter used in this test.
  std::unique_ptr<SketchEncrypter> sketch_encrypter_;
};

TEST_F(SketchEncrypterTest, ByteSizeShouldBeCorrect) {
  const int unique_cnt = 2;
  const int sum_cnt = 3;
  const int register_size = 1000;
  Sketch plain_sketch;
  *plain_sketch.mutable_config() = CreateSketchConfig(unique_cnt, sum_cnt);
  AddRandomRegisters(register_size, plain_sketch);

  ASSERT_EQ(plain_sketch.registers_size(), 1000);

  ASSERT_OK_AND_ASSIGN(std::string result,
                       EncryptWithConflictingKeys(plain_sketch));

  // Using SizeIs ends up printing all of "result", which is huge.
  EXPECT_EQ(result.size(), register_size * (1 + unique_cnt + sum_cnt) * 66);
}

TEST_F(SketchEncrypterTest, EncryptionShouldBeNonDeterministic) {
  Sketch plain_sketch;
  *plain_sketch.mutable_config() =
      CreateSketchConfig(/* unique_cnt = */ 0, /* sum_cnt = */ 0);
  plain_sketch.add_registers()->set_index(1);
  plain_sketch.add_registers()->set_index(1);

  ASSERT_OK_AND_ASSIGN(std::string result,
                       EncryptWithConflictingKeys(plain_sketch));
  std::vector<std::string> cipher_words = GetCipherStrings(result);
  ASSERT_THAT(cipher_words, SizeIs(4));  // 2 regs * 1 vals * 2 words

  CiphertextString cipher_index_1_a = {cipher_words[0], cipher_words[1]};
  CiphertextString cipher_index_1_b = {cipher_words[2], cipher_words[3]};
  // Multiple encryption of the same index value should be different.
  EXPECT_NE(cipher_index_1_a.u, cipher_index_1_b.u);
  EXPECT_NE(cipher_index_1_a.e, cipher_index_1_b.e);
  EXPECT_THAT(cipher_index_1_a,
              HasSameDecryption(original_cipher_.get(), cipher_index_1_b));
}

TEST_F(SketchEncrypterTest, EncryptionOfCountValueShouldBeAdditiveHomomorphic) {
  Context ctx;
  ASSERT_OK_AND_ASSIGN(ECGroup ec_group, ECGroup::Create(kTestCurveId, &ctx));

  Sketch plain_sketch;
  *plain_sketch.mutable_config() = CreateSketchConfig(
      /* unique_cnt = */ 0, /* sum_cnt = */ 1);
  plain_sketch.add_registers()->add_values(1);
  plain_sketch.add_registers()->add_values(2);
  plain_sketch.add_registers()->add_values(3);
  plain_sketch.add_registers()->add_values(4);
  plain_sketch.add_registers()->add_values(5);

  ASSERT_OK_AND_ASSIGN(std::string result,
                       EncryptWithConflictingKeys(plain_sketch));
  std::vector<std::string> cipher_words = GetCipherStrings(result);
  ASSERT_THAT(cipher_words, SizeIs(20));  // 5 regs * 2 vals * 2 words

  CiphertextString cipher_1 = {cipher_words[2], cipher_words[3]};
  CiphertextString cipher_2 = {cipher_words[6], cipher_words[7]};
  CiphertextString cipher_3 = {cipher_words[10], cipher_words[11]};
  CiphertextString cipher_4 = {cipher_words[14], cipher_words[15]};
  CiphertextString cipher_5 = {cipher_words[18], cipher_words[19]};

  ASSERT_OK_AND_ASSIGN(CiphertextString cipher_1_add_4,
                       AddCiphertext(cipher_1, cipher_4, ec_group));
  ASSERT_OK_AND_ASSIGN(CiphertextString cipher_2_add_3,
                       AddCiphertext(cipher_2, cipher_3, ec_group));

  EXPECT_THAT(cipher_5,
              HasSameDecryption(original_cipher_.get(), cipher_1_add_4));
  EXPECT_THAT(cipher_5,
              HasSameDecryption(original_cipher_.get(), cipher_2_add_3));
}

TEST_F(SketchEncrypterTest, MaximumCountValueShouldWork) {
  Context ctx;
  ASSERT_OK_AND_ASSIGN(ECGroup ec_group, ECGroup::Create(kTestCurveId, &ctx));

  Sketch plain_sketch;
  *plain_sketch.mutable_config() =
      CreateSketchConfig(/* unique_cnt = */ 0, /* sum_cnt = */ 1);
  plain_sketch.add_registers()->add_values(kMaxCounterValue + 10);
  plain_sketch.add_registers()->add_values(kMaxCounterValue + 1);

  ASSERT_OK_AND_ASSIGN(std::string result,
                       EncryptWithConflictingKeys(plain_sketch));
  std::vector<std::string> cipher_words = GetCipherStrings(result);
  ASSERT_THAT(cipher_words, SizeIs(8));  // 2 regs * 2 vals * 2 words

  CiphertextString count_a = {cipher_words[2], cipher_words[3]};
  CiphertextString count_b = {cipher_words[6], cipher_words[7]};

  // Encryption of kMaxCounterValue +10 and kMaxCounterValue+1 should be the
  // same.
  EXPECT_THAT(count_a, HasSameDecryption(original_cipher_.get(), count_b));
}

TEST_F(SketchEncrypterTest, ZeroCountValueShouldThrow) {
  Sketch plain_sketch;
  *plain_sketch.mutable_config() =
      CreateSketchConfig(/* unique_cnt = */ 0, /* sum_cnt = */ 1);
  plain_sketch.add_registers()->add_values(0);

  absl::StatusOr<std::string> result = EncryptWithConflictingKeys(plain_sketch);

  ASSERT_FALSE(result.status().ok());
  EXPECT_NE(result.status().message().find("should be positive"),
            std::string::npos);
}

TEST_F(SketchEncrypterTest, TestDestroyedRegistersUsingConflictingKeys) {
  Context ctx;
  ASSERT_OK_AND_ASSIGN(ECGroup ec_group, ECGroup::Create(kTestCurveId, &ctx));

  Sketch plain_sketch;
  *plain_sketch.mutable_config() =
      CreateSketchConfig(/* unique_cnt = */ 1, /* sum_cnt = */ 1);
  auto sketch_register = plain_sketch.add_registers();
  sketch_register->set_index(123);
  sketch_register->add_values(-1);  // UNIQUE value -1 means destroyed
  sketch_register->add_values(10);  // SUM value

  ASSERT_OK_AND_ASSIGN(std::string result,
                       EncryptWithConflictingKeys(plain_sketch));
  std::vector<std::string> cipher_words = GetCipherStrings(result);
  ASSERT_THAT(cipher_words, SizeIs(12));  // 2 regs * 3 vals * 2 words

  CiphertextString index_a = {cipher_words[0], cipher_words[1]};
  CiphertextString index_b = {cipher_words[6], cipher_words[7]};
  CiphertextString key_a = {cipher_words[2], cipher_words[3]};
  CiphertextString key_b = {cipher_words[8], cipher_words[9]};

  // Index should be the same.
  EXPECT_THAT(index_a, HasSameDecryption(original_cipher_.get(), index_b));
  // Keys should be different.
  EXPECT_THAT(key_a, Not(HasSameDecryption(original_cipher_.get(), key_b)));
}

TEST_F(SketchEncrypterTest, TestDestroyedRegistersUsingFlaggedKey) {
  Context ctx;
  ASSERT_OK_AND_ASSIGN(ECGroup ec_group, ECGroup::Create(kTestCurveId, &ctx));

  Sketch plain_sketch;
  *plain_sketch.mutable_config() =
      CreateSketchConfig(/* unique_cnt = */ 1, /* sum_cnt = */ 1);
  auto sketch_register = plain_sketch.add_registers();
  sketch_register->set_index(123);
  sketch_register->add_values(-1);  // UNIQUE value -1 means destroyed
  sketch_register->add_values(10);  // SUM value

  ASSERT_OK_AND_ASSIGN(std::string result, EncryptWithFlaggedKey(plain_sketch));
  std::vector<std::string> cipher_words = GetCipherStrings(result);
  ASSERT_THAT(cipher_words, SizeIs(6));  // 1 regs * 3 vals * 2 words

  CiphertextString index = {cipher_words[0], cipher_words[1]};
  CiphertextString key = {cipher_words[2], cipher_words[3]};
  CiphertextString value = {cipher_words[4], cipher_words[5]};

  EXPECT_THAT(index, IsEncryptionOf(original_cipher_.get(), "123"));
  EXPECT_THAT(key,
              IsEncryptionOf(original_cipher_.get(), "destroyed_register_key"));
  EXPECT_THAT(value,
              IsEncryptionOf(original_cipher_.get(), "destroyed_register_key"));
}

TEST_F(SketchEncrypterTest, CombineElGamalPublicKeysNormalCases) {
  ElGamalPublicKeys key1;
  key1.set_el_gamal_g(absl::HexStringToBytes(kElGamalPublicKeyG));
  key1.set_el_gamal_y(absl::HexStringToBytes(kElGamalPublicKeyY1));
  ElGamalPublicKeys key2;
  key2.set_el_gamal_g(absl::HexStringToBytes(kElGamalPublicKeyG));
  key2.set_el_gamal_y(absl::HexStringToBytes(kElGamalPublicKeyY2));
  ElGamalPublicKeys key3;
  key3.set_el_gamal_g(absl::HexStringToBytes(kElGamalPublicKeyG));
  key3.set_el_gamal_y(absl::HexStringToBytes(kElGamalPublicKeyY3));
  std::vector<ElGamalPublicKeys> keys{key1, key2, key3};

  ElGamalPublicKeys combinedKey;
  combinedKey.set_el_gamal_g(absl::HexStringToBytes(kElGamalPublicKeyG));
  combinedKey.set_el_gamal_y(
      absl::HexStringToBytes(kCombinedElGamalPublicKeyY));

  ASSERT_OK_AND_ASSIGN(ElGamalPublicKeys result,
                       CombineElGamalPublicKeys(kTestCurveId, keys));
  EXPECT_THAT(result, EqualsProto(combinedKey));
}

TEST_F(SketchEncrypterTest, CombineElGamalPublicKeysEmptyInputShouldThrow) {
  std::vector<ElGamalPublicKeys> keys;
  auto result = CombineElGamalPublicKeys(kTestCurveId, keys);
  ASSERT_FALSE(result.ok());
  EXPECT_THAT(result.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, "empty"));
}

TEST_F(SketchEncrypterTest, CombineElGamalPublicKeysInvalidInputShouldThrow) {
  ElGamalPublicKeys key1;
  key1.set_el_gamal_g("foo");
  key1.set_el_gamal_y("bar");
  ElGamalPublicKeys key2;
  key2.set_el_gamal_g("foo2");
  key2.set_el_gamal_y("bar2");

  std::vector<ElGamalPublicKeys> keys{key1, key2};
  auto result = CombineElGamalPublicKeys(kTestCurveId, keys);
  ASSERT_FALSE(result.ok());
  EXPECT_THAT(result.status(),
              StatusIs(absl::StatusCode::kInvalidArgument, "Invalid ECPoint"));
}

TEST_F(SketchEncrypterTest, NoisesShouldHaveTheSameIndex) {
  Context ctx;
  ASSERT_OK_AND_ASSIGN(ECGroup ec_group, ECGroup::Create(kTestCurveId, &ctx));

  std::string encrypted_sketch;
  int values_per_register = 5;
  int ciphertexts_per_register = (values_per_register + 1) * 2;

  EncryptSketchRequest::PublisherNoiseParameter noise_parameter;
  noise_parameter.set_epsilon(1);
  noise_parameter.set_delta(0.1);
  noise_parameter.set_publisher_count(3);

  ASSERT_TRUE(sketch_encrypter_
                  ->AppendNoiseRegisters(noise_parameter, values_per_register,
                                         encrypted_sketch)
                  .ok());

  std::vector<std::string> cipher_words = GetCipherStrings(encrypted_sketch);

  ASSERT_EQ(cipher_words.size() % ciphertexts_per_register, 0);
  int noise_register_count = cipher_words.size() / ciphertexts_per_register;

  ASSERT_GT(noise_register_count, 0);
  for (int i = 0; i <= cipher_words.size() - ciphertexts_per_register;
       i += ciphertexts_per_register) {
    CiphertextString index = {cipher_words[i], cipher_words[i + 1]};
    EXPECT_THAT(index, IsEncryptionOf(original_cipher_.get(),
                                      "publisher_noise_register_id"));
  }
}

}  // namespace
}  // namespace wfa::any_sketch::crypto
