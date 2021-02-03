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

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import org.wfanet.examples.streaming.HelloStreamingGrpcKt.HelloStreamingCoroutineStub

class HelloStreamingClient(private val channel: ManagedChannel) : Closeable {
  private val stub = HelloStreamingCoroutineStub(channel)

  suspend fun sayHelloStreaming(vararg names: String): List<String> {
    val requests = names.asFlow()
      .map { name -> SayHelloStreamingRequest.newBuilder().setName(name).build() }
      .onEach { request -> println("Sending: ${request.name}") }
    val response = stub.sayHelloStreaming(requests)
    return response.messageList
  }

  override fun close() {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }
}

suspend fun main(args: Array<String>) {
  val port = 50051
  val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
  HelloStreamingClient(channel).use { client ->
    val messages = client.sayHelloStreaming("Alice", "Bob", "Carol")
    messages.forEach { message ->
      println("Received: $message")
    }
  }
}
