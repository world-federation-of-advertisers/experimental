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
