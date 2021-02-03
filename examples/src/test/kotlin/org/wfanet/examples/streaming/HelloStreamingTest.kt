package org.wfanet.examples.streaming

import com.google.common.truth.Truth.assertThat
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HelloStreamingTest {
  @Rule
  @JvmField
  val grpcCleanup = GrpcCleanupRule()

  lateinit var client: HelloStreamingClient

  @Before
  fun initClient() {
    val serverName = InProcessServerBuilder.generateName()
    grpcCleanup.register(
      InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(HelloStreamingServer.HelloStreamingService())
        .build()
        .start()
    )
    val channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
    client = HelloStreamingClient(grpcCleanup.register(channel))
  }

  @Test
  fun `helloStreaming receives list of greetings from server`() = runBlocking {
    val response = client.sayHelloStreaming("Alice", "Bob", "Carol")
    assertThat(response)
      .containsExactlyElementsIn(listOf("Hello Alice", "Hello Bob", "Hello Carol"))
      .inOrder()
  }
}
