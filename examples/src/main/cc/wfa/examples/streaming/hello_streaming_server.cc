/*
 * Copyright 2021 The Cross-Media Measurement Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <grpc/grpc.h>
#include <grpcpp/server.h>
#include <grpcpp/server_builder.h>
#include <grpcpp/server_context.h>
#include <grpcpp/security/server_credentials.h>
#include "src/main/proto/wfa/examples/streaming/hello_streaming_service.grpc.pb.h"

using ::grpc::Server;
using ::grpc::ServerBuilder;
using ::grpc::ServerReader;
using ::grpc::ServerContext;
using ::grpc::Status;
using ::wfa_examples::streaming::HelloStreaming;
using ::wfa_examples::streaming::SayHelloStreamingRequest;
using ::wfa_examples::streaming::SayHelloStreamingResponse;

class HelloStreamingImpl final : public HelloStreaming::Service {
 public:
  Status SayHelloStreaming(ServerContext* context,
                           ServerReader<SayHelloStreamingRequest>* reader,
                           SayHelloStreamingResponse* response) override {
    SayHelloStreamingRequest request;
    while (reader->Read(&request)) {
      std::cout << "Received: " << request.name() << std::endl;
      response->add_message("Hello " + request.name());
    }

    return Status::OK;
  }
};

int main(int argc, char** argv) {
  std::string server_address = "0.0.0.0:50051";

  HelloStreamingImpl service;

  ServerBuilder builder;
  builder.AddListeningPort(server_address, grpc::InsecureServerCredentials());
  builder.RegisterService(&service);
  std::unique_ptr<Server> server(builder.BuildAndStart());
  std::cout << "C++ server listening on " << server_address << std::endl;
  server->Wait();

  return 0;
}
