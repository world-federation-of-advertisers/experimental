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

#include "wfa/examples/streaming/hello_streaming_service.h"

#include <iostream>

#include "absl/strings/str_cat.h"

namespace wfa_examples {
namespace streaming {

using ::grpc::ServerContext;
using ::grpc::ServerReader;
using ::grpc::Status;

Status HelloStreamingService::SayHelloStreaming(
    ServerContext* context, ServerReader<SayHelloStreamingRequest>* reader,
    SayHelloStreamingResponse* response) {
  SayHelloStreamingRequest request;
  while (reader->Read(&request)) {
    std::cout << "Received " << request.name() << std::endl;
    response->add_message(absl::StrCat("Hello ", request.name()));
  }

  return Status::OK;
}

}  // namespace streaming
}  // namespace wfa_examples
