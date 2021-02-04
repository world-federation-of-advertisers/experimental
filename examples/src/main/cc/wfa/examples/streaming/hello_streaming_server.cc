// Copyright 2021 The Cross-Media Measurement Authors
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

#include <iostream>

#include "absl/strings/str_cat.h"
#include "include/grpcpp/server_builder.h"
#include "wfa/examples/streaming/hello_streaming_service.h"

namespace wfa_examples {
namespace streaming {
namespace {

using ::grpc::Server;
using ::grpc::ServerBuilder;

void RunServer(int port) {
  HelloStreamingService service;
  ServerBuilder builder;
  builder.RegisterService(&service);

  builder.AddListeningPort(absl::StrCat("[::]:", port),
                           grpc::InsecureServerCredentials());
  std::unique_ptr<Server> server = builder.BuildAndStart();
  std::cerr << "Server listening on port " << port << std::endl;
  server->Wait();
}

}  // namespace
}  // namespace streaming
}  // namespace wfa_examples

namespace {

constexpr int kDefaultPort = 50051;

int GetPort(int argc, char** argv) {
  if (argc > 1) {
    int port;
    if (absl::SimpleAtoi(argv[1], &port)) {
      return port;
    } else {
      std::cerr << "Failed to parse port from " << argv[1] << std::endl;
    }
  }

  return kDefaultPort;
}

}  // namespace

int main(int argc, char** argv) {
  int port = GetPort(argc, argv);
  wfa_examples::streaming::RunServer(port);

  return 0;
}
