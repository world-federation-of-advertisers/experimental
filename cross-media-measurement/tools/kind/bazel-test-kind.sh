#!/usr/bin/env bash

# Copyright 2020 The Cross-Media Measurement Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

docker build -t kind-image - < tools/kind/Dockerfile
OUTPUT_ROOT=${HOME}/.cache/bazel-docker
docker run -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $PWD:$PWD \
  -v $OUTPUT_ROOT:$OUTPUT_ROOT \
  --network="host" \
  --workdir=$PWD \
  kind-image sh -c \
  "bazel \
  --output_user_root=$OUTPUT_ROOT \
  test src/test/kotlin/org/wfanet/measurement/e2e:correctness_test \
  --host_platform=//build/platforms:ubuntu_18_04 \
  --test_output=streamed"
