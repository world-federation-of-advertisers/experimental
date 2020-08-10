// Copyright 2020 The Measurement System Authors
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

package org.wfanet.measurement.storage.testing

import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.wfanet.measurement.common.base64UrlEncode
import org.wfanet.measurement.storage.StorageClient
import org.wfanet.measurement.storage.asFlow

/**
 * [StorageClient] implementation that utilizes flat files in the specified
 * directory as blobs.
 */
class FileSystemStorageClient(private val directory: File) :
  StorageClient<FileSystemStorageClient.Blob> {

  init {
    require(directory.isDirectory) { "$directory is not a directory" }
  }

  override suspend fun createBlob(blobKey: String, content: Flow<ByteBuffer>): Blob {
    val file = File(directory, blobKey.base64UrlEncode())
    withContext(Dispatchers.IO) {
      require(file.createNewFile()) { "$blobKey already exists" }

      file.outputStream().channel.use { byteChannel ->
        content.collect { buffer ->
          @Suppress("BlockingMethodInNonBlockingContext") // Flow context preservation.
          byteChannel.write(buffer)
        }
      }
    }

    return Blob(file)
  }

  override fun getBlob(blobKey: String): Blob? {
    val file = File(directory, blobKey.base64UrlEncode())
    return if (file.exists()) Blob(file) else null
  }

  class Blob(private val file: File) : StorageClient.Blob {
    override val size: Long
      get() = file.length()

    override fun read(flowBufferSize: Int): Flow<ByteBuffer> =
      file.inputStream().channel.asFlow(flowBufferSize)

    override fun delete() {
      file.delete()
    }
  }
}

private fun String.base64UrlEncode(): String {
  return toByteArray().base64UrlEncode()
}
