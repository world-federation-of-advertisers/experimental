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

#include "math/noise_parameters_computation.h"

namespace wfa::math {

namespace {

int ComputateMuPolya(double epsilon, double delta, int n) {
  ABSL_ASSERT(epsilon > 0);
  ABSL_ASSERT(delta > 0);
  ABSL_ASSERT(n > 0);
  return std::ceil(std::log(2.0 * n / (delta * (1 - std::exp(-epsilon)))) /
                   epsilon);
}

int ComputateMuDlap(double epsilon, double delta) {
  ABSL_ASSERT(epsilon > 0);
  ABSL_ASSERT(delta > 0);
  return std::ceil(std::log(2.0 / delta) / epsilon);
}

}  // namespace

math::DistributedGeometricRandomComponentOptions GetBlindHistogramNoiseOptions(
    const wfa::common::DifferentialPrivacyParams& params, int publisher_count,
    int uncorrupted_party_count) {
  ABSL_ASSERT(publisher_count > 0);
  ABSL_ASSERT(uncorrupted_party_count > 0);
  double success_ratio = std::exp(-params.epsilon() / 2);
  int offset = ComputateMuPolya(params.epsilon() / 2, params.delta(),
                                uncorrupted_party_count * publisher_count);
  return {
      .num = uncorrupted_party_count,
      .p = success_ratio,
      .truncate_threshold = offset,
      .shift_offset = offset,
  };
}

math::DistributedGeometricRandomComponentOptions
GetNoiseForPublisherNoiseOptions(
    const wfa::common::DifferentialPrivacyParams& params, int publisher_count,
    int uncorrupted_party_count) {
  ABSL_ASSERT(publisher_count > 0);
  ABSL_ASSERT(uncorrupted_party_count > 0);
  double success_ratio = std::exp(-params.epsilon() / publisher_count);
  int offset = ComputateMuPolya(params.epsilon() / publisher_count,
                                params.delta(), uncorrupted_party_count);
  return {
      .num = uncorrupted_party_count,
      .p = success_ratio,
      .truncate_threshold = offset,
      .shift_offset = offset,
  };
}

math::DistributedGeometricRandomComponentOptions GetGlobalReachDpNoiseOptions(
    const wfa::common::DifferentialPrivacyParams& params,
    int uncorrupted_party_count) {
  ABSL_ASSERT(uncorrupted_party_count > 0);
  double success_ratio = std::exp(-params.epsilon());
  int offset = ComputateMuPolya(params.epsilon(), params.delta(),
                                uncorrupted_party_count);
  return {
      .num = uncorrupted_party_count,
      .p = success_ratio,
      .truncate_threshold = offset,
      .shift_offset = offset,
  };
}

math::DistributedGeometricRandomComponentOptions GetFrequencyNoiseOptions(
    const wfa::common::DifferentialPrivacyParams& params, int max_frequency,
    int uncorrupted_party_count) {
  ABSL_ASSERT(max_frequency > 0);
  ABSL_ASSERT(uncorrupted_party_count > 0);
  double success_ratio = std::exp(-params.epsilon() / 2);
  int offset = ComputateMuPolya(params.epsilon() / 2, params.delta(),
                                2 * uncorrupted_party_count * max_frequency);
  return {
      .num = uncorrupted_party_count,
      .p = success_ratio,
      .truncate_threshold = offset,
      .shift_offset = offset,
  };
}

math::TruncatedDiscreteLaplaceDistributedOptions GetPublisherNoiseOptions(
    const wfa::common::DifferentialPrivacyParams& params, int publisher_count) {
  ABSL_ASSERT(publisher_count > 0);
  return {
      .mu = ComputateMuDlap(params.epsilon() / publisher_count,
                            params.delta() / publisher_count),
      .s = params.epsilon() / publisher_count,
  };
}

}  // namespace wfa::math
