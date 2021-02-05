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

#ifndef SRC_MAIN_CC_WFA_EXAMPLES_STREAMING_HELLO_STREAMING_CLIENT_H_
#define SRC_MAIN_CC_WFA_EXAMPLES_STREAMING_HELLO_STREAMING_CLIENT_H_

#include <grpcpp/channel.h>

#include <memory>
#include <string>
#include <vector>

#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"
#include "absl/types/span.h"
#include "src/main/proto/wfa/examples/streaming/hello_streaming_service.grpc.pb.h"

namespace wfa_examples {
namespace streaming {

class HelloStreamingClient {
 public:
  explicit HelloStreamingClient(std::shared_ptr<grpc::Channel> channel);

  // HelloStreamingClient is neither copyable nor movable.
  HelloStreamingClient(const HelloStreamingClient&) = delete;
  HelloStreamingClient& operator=(const HelloStreamingClient&) = delete;

  absl::StatusOr<std::vector<std::string>> SayHello(
      absl::Span<const absl::string_view> names);

 private:
  std::unique_ptr<HelloStreaming::Stub> stub_;
};

}  // namespace streaming
}  // namespace wfa_examples

#endif  // SRC_MAIN_CC_WFA_EXAMPLES_STREAMING_HELLO_STREAMING_CLIENT_H_
