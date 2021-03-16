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

FROM ubuntu:18.04
USER root
RUN apt-get update
RUN apt-get install -y \
  curl \
  wget \
  docker.io \
  clang \
  g++ \
  swig \
  unzip \
  zip \
  openjdk-11-jdk
RUN wget -O /usr/bin/kind https://github.com/kubernetes-sigs/kind/releases/download/v0.10.0/kind-linux-amd64
RUN chmod +x /usr/bin/kind
RUN wget https://github.com/bazelbuild/bazel/releases/download/4.0.0/bazel-4.0.0-installer-linux-x86_64.sh
RUN chmod +x bazel-4.0.0-installer-linux-x86_64.sh
RUN ./bazel-4.0.0-installer-linux-x86_64.sh
RUN curl -LO https://dl.k8s.io/release/v1.20.0/bin/linux/amd64/kubectl
RUN install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
