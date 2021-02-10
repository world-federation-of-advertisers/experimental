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
#include <grpcpp/channel.h>
#include <grpcpp/client_context.h>
#include <grpcpp/create_channel.h>
#include <grpcpp/security/credentials.h>
#include "src/main/proto/wfa/examples/streaming/hello_streaming_service.grpc.pb.h"

using grpc::Channel;
using grpc::ClientContext;
using grpc::ClientReader;
using grpc::ClientReaderWriter;
using grpc::ClientWriter;
using grpc::Status;
using wfa_examples::streaming::HelloStreaming;
using wfa_examples::streaming::SayHelloStreamingRequest;
using wfa_examples::streaming::SayHelloStreamingResponse;

int main(int argc, char** argv) {
  std::string server_address = "0.0.0.0:50051";

  std::cout << "Server Address: " << server_address << std::endl;

  std::cout << "Starting client..." << std::endl;

  std::shared_ptr<Channel> channel =
      grpc::CreateChannel(server_address, grpc::InsecureChannelCredentials());
  std::unique_ptr<HelloStreaming::Stub> stub = HelloStreaming::NewStub(channel);

  ClientContext context;
  SayHelloStreamingResponse response;
  std::unique_ptr<ClientWriter<SayHelloStreamingRequest> > writer(
      stub->SayHelloStreaming(&context, &response));
  for (const std::string& name : {"Alice", "Bob", "Carol"}) {
    SayHelloStreamingRequest request;
    request.set_name(name);
    writer->Write(request);
  }
  writer->WritesDone();
  Status status = writer->Finish();

  if (status.ok()) {
    std::cout << "Finished streaming RPC" << std::endl;
    for (const std::string& message : response.message()) {
      std::cout << "Received: " << message << std::endl;
    }
  } else {
    std::cout << "Streaming RPC failed" << std::endl;
    std::cout << status.error_code() << std::endl;
    std::exit(EXIT_FAILURE);
  }

  return 0;
}
