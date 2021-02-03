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

package org.wfanet.examples.streaming

import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.wfanet.examples.streaming.HelloStreamingGrpcKt.HelloStreamingCoroutineImplBase

class HelloStreamingServer(private val port: Int) {
  val server: Server = ServerBuilder
    .forPort(port)
    .addService(HelloStreamingService())
    .build()

  fun start() {
    server.start()
    println("Server started, listening on $port")
    Runtime.getRuntime().addShutdownHook(
      Thread {
        println("*** shutting down gRPC server since JVM is shutting down")
        this@HelloStreamingServer.stop()
        println("*** server shut down")
      }
    )
  }

  private fun stop() {
    server.shutdown()
  }

  fun blockUntilShutdown() {
    server.awaitTermination()
  }

  private class HelloStreamingService : HelloStreamingCoroutineImplBase() {
    override suspend fun helloStreaming(
      requests: Flow<HelloStreamingRequest>
    ): HelloStreamingResponse {
      val responseBuilder = HelloStreamingResponse.newBuilder()
      requests.collect { request ->
        println("Received: ${request.name}")
        responseBuilder
          .addMessage("Hello ${request.name}")
      }
      val response = responseBuilder.build()
      response.messageList.forEach { message ->
        println("Sending: $message")
      }
      return response
    }
  }
}

fun main() {
  val port = System.getenv("PORT")?.toInt() ?: 50051
  val server = HelloStreamingServer(port)
  server.start()
  server.blockUntilShutdown()
}
