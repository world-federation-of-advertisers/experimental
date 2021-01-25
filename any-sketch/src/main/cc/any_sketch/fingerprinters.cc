#include "src/main/cc/any_sketch/fingerprinters.h"

#include "absl/base/internal/endian.h"
#include "external/farmhash/src/farmhash.h"
#include "openssl/sha.h"

namespace wfa::any_sketch {
namespace {
class Sha256Fingerprinter : public Fingerprinter {
 public:
  Sha256Fingerprinter() = default;

  ~Sha256Fingerprinter() override = default;

  uint64_t Fingerprint(absl::Span<const unsigned char> item) const override {
    SHA256_CTX context;
    uint8_t digest[32];
    memset(digest, 0, sizeof(digest));

    SHA256_Init(&context);
    SHA256_Update(&context, item.data(), item.size());
    SHA256_Final(digest, &context);

    return absl::little_endian::Load64(digest);
  }
};

class FarmFingerprinter : public Fingerprinter {
 public:
  FarmFingerprinter() = default;
  ~FarmFingerprinter() override = default;

  uint64_t Fingerprint(absl::Span<const unsigned char> item) const override {
    return util::Fingerprint64(reinterpret_cast<const char*>(item.data()),
                               item.size());
  }
};
}  // namespace

const Fingerprinter& GetSha256Fingerprinter() {
  static const auto* const fingerprinter = new Sha256Fingerprinter();
  return *fingerprinter;
}
const Fingerprinter& GetFarmFingerprinter() {
  static const auto* const fingerprinter = new FarmFingerprinter();
  return *fingerprinter;
}
}  // namespace wfa::any_sketch
