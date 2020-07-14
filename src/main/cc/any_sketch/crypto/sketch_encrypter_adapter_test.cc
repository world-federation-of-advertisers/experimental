#include "sketch_encrypter_adapter.h"

#include <openssl/obj_mac.h>

#include "absl/memory/memory.h"
#include "crypto/commutative_elgamal.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "sketch_encrypter.h"
#include "wfa/any_sketch/crypto/sketch_encryption_methods.pb.h"

namespace wfa::any_sketch::crypto {
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
      // Mod(kMaxCounterValue * 2) so we have some but not too many values
      // exceed the max.
      last_register->add_values(rand() %
                                static_cast<int>(kMaxCounterValue * 2));
    }
  }
}

TEST(SketchEncrypterJavaAdapterTest, basicBehavior) {
  // Generate a key for testing.
  auto commutativeElGamal =
      CommutativeElGamal::CreateWithNewKeyPair(kTestCurveId).value();
  std::pair<std::string, std::string> public_key_pair =
      commutativeElGamal->GetPublicKeyBytes().value();

  const int index_cnt = 1;
  const int unique_cnt = 2;
  const int sum_cnt = 3;
  const int register_size = 1000;

  // Build the request
  wfa::any_sketch::crypto::EncryptSketchRequest request;
  request.mutable_el_gamal_keys()->set_el_gamal_g(public_key_pair.first);
  request.mutable_el_gamal_keys()->set_el_gamal_y(public_key_pair.second);
  request.set_curve_id(kTestCurveId);
  request.set_maximum_value(kMaxCounterValue);
  *request.mutable_sketch()->mutable_config() =
      CreateSketchConfig(index_cnt, unique_cnt, sum_cnt);
  AddRandomRegisters(register_size, *request.mutable_sketch());

  wfa::any_sketch::crypto::EncryptSketchResponse response;
  response.ParseFromString(EncryptSketch(request.SerializeAsString()).value());

  EXPECT_THAT(response.encrypted_sketch(),
              SizeIs(register_size * (index_cnt + unique_cnt + sum_cnt) * 66));
}

}  // namespace
}  // namespace wfa::any_sketch::crypto
