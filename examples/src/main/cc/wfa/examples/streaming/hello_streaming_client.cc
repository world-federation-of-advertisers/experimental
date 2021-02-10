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

#include "src/main/cc/wfa/examples/streaming/hello_streaming_client.h"

#include <grpcpp/create_channel.h>
#include <grpcpp/security/credentials.h>

#include <iostream>

#include "absl/container/fixed_array.h"

namespace wfa_examples {
namespace streaming {
namespace {

using ::grpc::Channel;
using ::grpc::ClientContext;
using ::grpc::ClientWriter;
using ::grpc::CreateChannel;

absl::Status MakeStatus(const grpc::Status& grpc_status) {
  if (grpc_status.ok()) {
    return absl::OkStatus();
  }

  auto status_code = static_cast<absl::StatusCode>(grpc_status.error_code());
  return absl::Status(status_code, grpc_status.error_message());
}

void RunClient(absl::string_view server_address,
               absl::Span<const absl::string_view> names) {
  HelloStreamingClient client(CreateChannel(
      std::string(server_address), grpc::InsecureChannelCredentials()));
  absl::StatusOr<std::vector<std::string>> messages_or = client.SayHello(names);
  if (!messages_or.ok()) {
    std::cerr << "RPC failed " << messages_or.status() << std::endl;
    return;
  }

  for (const std::string& message : messages_or.value()) {
    std::cout << message << std::endl;
  }
}

}  // namespace

HelloStreamingClient::HelloStreamingClient(std::shared_ptr<Channel> channel)
    : stub_(HelloStreaming::NewStub(channel)) {}

absl::StatusOr<std::vector<std::string>> HelloStreamingClient::SayHello(
    absl::Span<const absl::string_view> names) {
  ClientContext context;
  SayHelloStreamingResponse response;
  std::unique_ptr<ClientWriter<SayHelloStreamingRequest>> writer =
      stub_->SayHelloStreaming(&context, &response);

  for (absl::string_view name : names) {
    SayHelloStreamingRequest request;
    request.set_name(std::string(name));
    writer->Write(request);
  }
  writer->WritesDone();

  grpc::Status status = writer->Finish();
  if (!status.ok()) {
    return MakeStatus(status);
  }

  // Copy response messages to return.
  std::vector<std::string> messages;
  for (const std::string& message : response.message()) {
    messages.push_back(message);
  }
  return messages;
}

}  // namespace streaming
}  // namespace wfa_examples

int main(int argc, char** argv) {
  if (argc < 3) {
    std::cerr << "Usage:"
              << " hello_streaming_client <server_address> <name> [<name>...]"
              << std::endl
              << "\tExample: hello_streaming_client localhost:50051"
              << " Alice Bob Carol" << std::endl;
    return 1;
  }

  auto args = absl::MakeConstSpan(argv, argc);
  auto names = args.subspan(2);
  wfa_examples::streaming::RunClient(
      args.at(1),
      absl::FixedArray<absl::string_view>(names.begin(), names.end()));

  return 0;
}
