/*
 * Copyright 2021 The Cross-Media Measurement Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wfanet.examples.streaming;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.wfanet.examples.streaming.HelloStreamingGrpc.HelloStreamingStub;

class HelloStreamingClient {
  private static final Logger logger = Logger.getLogger(HelloStreamingClient.class.getName());

  public static void sayHelloStreaming(HelloStreamingStub asyncStub) throws InterruptedException {
    CountDownLatch finishLatch = new CountDownLatch(1);
    StreamObserver<SayHelloStreamingResponse> responseObserver =
      new StreamObserver<SayHelloStreamingResponse>() {
        @Override
        public void onNext(SayHelloStreamingResponse response) {
          for (String message : response.getMessageList()) {
            logger.info("Received: " + message);
          }
        }

        @Override
        public void onError(Throwable t) {
          logger.warning("sayHelloStreaming failed: " + t.getMessage());
          finishLatch.countDown();
        }

        @Override
        public void onCompleted() {
          logger.info("Finished sayHelloStreaming");
          finishLatch.countDown();
        }
      };
    String[] names = new String[]{"Alice", "Bob", "Carol"};
    StreamObserver<SayHelloStreamingRequest> requestObserver =
      asyncStub.sayHelloStreaming(responseObserver);
    try {
      for (String name : names) {
        requestObserver.onNext(SayHelloStreamingRequest.newBuilder().setName(name).build());
      }
    } catch (RuntimeException e) {
      requestObserver.onError(e);
      throw e;
    }
    requestObserver.onCompleted();
    if (!finishLatch.await(1, TimeUnit.MINUTES)) {
      logger.warning("sayHelloStreaming can not finish within 1 minutes");
    }
  }

  public static void main(String[] args) throws Exception {
    String target = "localhost:50051";
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    HelloStreamingStub asyncStub = HelloStreamingGrpc.newStub(channel);
    try {
      sayHelloStreaming(asyncStub);
    } finally {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
