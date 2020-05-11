#include "farm_hash_function.h"

#include "external/farmhash/src/farmhash.h"

namespace wfa_xmedia {

uint64_t FarmHashFunction::Fingerprint(
    absl::Span<const unsigned char> item) const {
  return util::Fingerprint64(reinterpret_cast<const char*>(item.data()),
                                 item.size());
}

}  // namespace wfa_xmedia
