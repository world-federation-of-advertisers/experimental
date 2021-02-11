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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class HelloStreamingServer extends HelloStreamingGrpc.HelloStreamingImplBase {
  private static final Logger logger = Logger.getLogger(HelloStreamingServer.class.getName());

  @Override
  public StreamObserver<SayHelloStreamingRequest> sayHelloStreaming(
      StreamObserver<SayHelloStreamingResponse> responseObserver) {
    return new StreamObserver<SayHelloStreamingRequest>() {
      ArrayList<String> messages = new ArrayList<>();

      @Override
      public void onNext(SayHelloStreamingRequest request) {
        logger.info("Received: " + request.getName());
        messages.add("Hello " + request.getName());
      }

      @Override
      public void onError(Throwable t) {
        logger.log(Level.WARNING, "sayHelloStreaming cancelled");
      }

      @Override
      public void onCompleted() {
        responseObserver.onNext(
            SayHelloStreamingResponse.newBuilder().addAllMessage(messages).build());
        responseObserver.onCompleted();
      }
    };
  }

  public static void main(String[] args) throws Exception {
    int port = 50051;
    Server server = ServerBuilder.forPort(port).addService(new HelloStreamingServer()).build();
    logger.info("Server started, listening on " + port);
    server.start();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                System.err.println("*** shutting down gRPC server");
                try {
                  server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                  e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
              }
            });
    server.awaitTermination();
  }
}
