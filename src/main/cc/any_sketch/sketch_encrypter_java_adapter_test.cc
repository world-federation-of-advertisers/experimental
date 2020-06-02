#include "sketch_encrypter_java_adapter.h"

#include <openssl/obj_mac.h>

#include "absl/memory/memory.h"
#include "crypto/commutative_elgamal.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"

namespace wfa::any_sketch {
namespace {

using ::private_join_and_compute::CommutativeElGamal;
using ::private_join_and_compute::Context;
using ::private_join_and_compute::ECGroup;
using ::private_join_and_compute::ECPoint;
using ::private_join_and_compute::InternalError;
using ::testing::SizeIs;
using ::wfa::measurement::api::v1alpha::Sketch;
using ::wfa::measurement::api::v1alpha::SketchConfig;

constexpr int kTestCurveId = NID_X9_62_prime256v1;
constexpr int kMaxCounterValue = 100;

// Helper function to create a SketchConfig.
SketchConfig CreateSketchConfig(const int index_cnt, const int unique_cnt,
                                const int sum_cnt) {
  SketchConfig sketch_config;
  for (int i = 0; i < index_cnt; ++i) {
    sketch_config.add_indexes();
  }
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
    last_register->set_index(rand());
    for (int value_i = 0; value_i < sketch.config().values_size(); ++value_i) {
      // The Aggregate type doesn't matter here.
      // Mod(kMaxCounterValue * 1.2) so we have some but not too many values
      // exceed the max.
      last_register->add_values(rand() %
                                static_cast<int>(kMaxCounterValue * 1.2));
    }
  }
}

TEST(SketchEncrypterJavaAdapterTest, validEncrypterShouldEncryptSuccessfully) {
  auto commutativeElGamal =
      CommutativeElGamal::CreateWithNewKeyPair(kTestCurveId).value();
  auto public_key_pair = commutativeElGamal->GetPublicKeyBytes().value();
  CiphertextString public_key = {
      .u = public_key_pair.first,
      .e = public_key_pair.second,
  };
  auto encrypter = SketchEncrypterJavaAdapter::CreateFromPublicKeys(
      kTestCurveId, kMaxCounterValue, public_key_pair.first,
      public_key_pair.second);
  ASSERT_TRUE(encrypter.ok());

  const int index_cnt = 1;
  const int unique_cnt = 2;
  const int sum_cnt = 3;
  const int register_size = 1000;
  Sketch plain_sketch;
  *plain_sketch.mutable_config() = CreateSketchConfig(
      /* index_cnt = */ 1, /* unique_cnt = */ 2, /* sum_cnt = */ 3);
  AddRandomRegisters(register_size, plain_sketch);

  auto result = encrypter.value()->Encrypt(plain_sketch.SerializeAsString());
  ASSERT_TRUE(result.ok());
  EXPECT_THAT(result.value(),
              SizeIs(register_size * (index_cnt + unique_cnt + sum_cnt) * 66));
}

TEST(SketchEncrypterJavaAdapterTest, invalidSketchProtoDataShouldFail) {
  auto commutativeElGamal =
      CommutativeElGamal::CreateWithNewKeyPair(kTestCurveId).value();
  auto public_key_pair = commutativeElGamal->GetPublicKeyBytes().value();
  CiphertextString public_key = {
      .u = public_key_pair.first,
      .e = public_key_pair.second,
  };
  auto encrypter = SketchEncrypterJavaAdapter::CreateFromPublicKeys(
      kTestCurveId, kMaxCounterValue, public_key_pair.first,
      public_key_pair.second);
  ASSERT_TRUE(encrypter.ok());

  auto result = encrypter.value()->Encrypt("invalid proto string");
  ASSERT_TRUE(!result.ok());
  EXPECT_NE(result.status().message().find("failed to parse the sketch proto"),
            std::string::npos);
}

TEST(SketchEncrypterJavaAdapterTest, invalidPublicKeyShouldFail) {
  auto encrypter = SketchEncrypterJavaAdapter::CreateFromPublicKeys(
      kTestCurveId, kMaxCounterValue, "some invalid key", "some invalid key");
  ASSERT_TRUE(!encrypter.ok());
}

}  // namespace
}  // namespace wfa::any_sketch
