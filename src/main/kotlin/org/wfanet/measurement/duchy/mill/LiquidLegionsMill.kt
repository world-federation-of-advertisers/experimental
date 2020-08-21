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

package org.wfanet.measurement.duchy.mill

import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusException
import java.nio.charset.Charset
import java.util.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.wfanet.measurement.api.v1alpha.ConfirmGlobalComputationRequest
import org.wfanet.measurement.api.v1alpha.GlobalComputationsGrpcKt.GlobalComputationsCoroutineStub
import org.wfanet.measurement.api.v1alpha.MetricRequisition
import org.wfanet.measurement.common.MinimumIntervalThrottler
import org.wfanet.measurement.common.logAndSuppressExceptionSuspend
import org.wfanet.measurement.db.duchy.computation.LiquidLegionsSketchAggregationComputationStorageClients
import org.wfanet.measurement.db.duchy.computation.singleOutputBlobMetadata
import org.wfanet.measurement.internal.LiquidLegionsSketchAggregationStage as LiquidLegionsStage
import org.wfanet.measurement.internal.duchy.BlindLastLayerIndexThenJoinRegistersRequest
import org.wfanet.measurement.internal.duchy.BlindOneLayerRegisterIndexRequest
import org.wfanet.measurement.internal.duchy.ClaimWorkRequest
import org.wfanet.measurement.internal.duchy.ClaimWorkResponse
import org.wfanet.measurement.internal.duchy.ComputationControlServiceGrpcKt.ComputationControlServiceCoroutineStub
import org.wfanet.measurement.internal.duchy.ComputationDetails.CompletedReason
import org.wfanet.measurement.internal.duchy.ComputationDetails.RoleInComputation
import org.wfanet.measurement.internal.duchy.ComputationStage
import org.wfanet.measurement.internal.duchy.ComputationToken
import org.wfanet.measurement.internal.duchy.ComputationTypeEnum.ComputationType
import org.wfanet.measurement.internal.duchy.DecryptOneLayerFlagAndCountRequest
import org.wfanet.measurement.internal.duchy.FinishComputationRequest
import org.wfanet.measurement.internal.duchy.HandleConcatenatedSketchRequest
import org.wfanet.measurement.internal.duchy.HandleEncryptedFlagsAndCountsRequest
import org.wfanet.measurement.internal.duchy.MetricValue.ResourceKey
import org.wfanet.measurement.internal.duchy.MetricValuesGrpcKt.MetricValuesCoroutineStub
import org.wfanet.measurement.internal.duchy.StreamMetricValueRequest
import org.wfanet.measurement.internal.duchy.ToConfirmRequisitionsStageDetails.RequisitionKey
import org.wfanet.measurement.service.internal.duchy.computation.storage.outputPathList

/**
 * Mill works on computations using the LiquidLegionSketchAggregationProtocol.
 *
 * @param[millId] The identifier of this mill, used to claim a work.
 * @param[storageClients] clients that have access to local computation storage, i.e., spanner
 *    table and blob store.
 * @param[metricValuesClient] client of the own duchy's MetricValuesService.
 * @param[globalComputationsClient] client of the kingdom's GlobalComputationsService.
 * @param[workerStubs] A map from other duchies' Ids to their corresponding
 *    computationControlClients, used for passing computation to other duchies.
 * @param[cryptoKeySet] The set of crypto keys used in the computation.
 * @param[cryptoWorker] The cryptoWorker that performs the actual computation.
 * @param[throttler] A throttler used to rate limit the frequency of the mill polling from the
 *    computation table.
 * @param[chunkSize] The size of data chunk when sending result to other duchies.
 */
class LiquidLegionsMill(
  private val millId: String,
  private val storageClients: LiquidLegionsSketchAggregationComputationStorageClients,
  private val metricValuesClient: MetricValuesCoroutineStub,
  private val globalComputationsClient: GlobalComputationsCoroutineStub,
  private val workerStubs: Map<String, ComputationControlServiceCoroutineStub>,
  private val cryptoKeySet: CryptoKeySet,
  private val cryptoWorker: LiquidLegionsCryptoWorker,
  private val throttler: MinimumIntervalThrottler,
  private val chunkSize: Int = 2_000_000
) {
  companion object {
    val logger: Logger = Logger.getLogger(this::class.java.name)
  }

  suspend fun continuallyProcessComputationQueue() {
    logger.info("Starting...")
    logAndSuppressExceptionSuspend { throttler.onReady { pollAndProcessNextComputation() } }
  }

  suspend fun pollAndProcessNextComputation() {
    val claimWorkRequest = ClaimWorkRequest.newBuilder()
      .setComputationType(ComputationType.LIQUID_LEGIONS_SKETCH_AGGREGATION_V1)
      .setOwner(millId)
      .build()
    val claimWorkResponse: ClaimWorkResponse =
      storageClients.computationStorageClient.claimWork(claimWorkRequest)
    if (claimWorkResponse.hasToken()) {
      val token: ComputationToken = claimWorkResponse.token
      when (token.computationStage.liquidLegionsSketchAggregation) {
        LiquidLegionsStage.TO_CONFIRM_REQUISITIONS ->
          confirmRequisitions(token)
        LiquidLegionsStage.TO_ADD_NOISE ->
          addNoise()
        LiquidLegionsStage.TO_APPEND_SKETCHES_AND_ADD_NOISE ->
          appendSketchesAndAddNoise()
        LiquidLegionsStage.TO_BLIND_POSITIONS ->
          blindPositions(token)
        LiquidLegionsStage.TO_BLIND_POSITIONS_AND_JOIN_REGISTERS ->
          blindPositionsAndJoinRegisters(token)
        LiquidLegionsStage.TO_DECRYPT_FLAG_COUNTS_AND_COMPUTE_METRICS ->
          decryptFlagCountsAndComputeMetrics()
        LiquidLegionsStage.TO_DECRYPT_FLAG_COUNTS ->
          decryptFlagCounts(token)
        else -> error("Unexpected stage for mill to process: $token")
      }
    }
  }

  /** Process computation in the TO_CONFIRM_REQUISITIONS stage */
  private suspend fun confirmRequisitions(token: ComputationToken): ComputationToken {
    val requisitionsToConfirm =
      token.stageSpecificDetails.toConfirmRequisitionsStageDetails.keysList
    val availableRequisitions = mutableSetOf<RequisitionKey>()
    val bytesLists = mutableListOf<ByteString>()
    requisitionsToConfirm.forEach {
      try {
        // Call the MetricValuesService to get the requisition
        metricValuesClient.streamMetricValue(it.toStreamMetricValueRequest())
          .filter { response -> response.hasChunk() }
          .map { response -> response.chunk.data }
          .toList(bytesLists)
        availableRequisitions.add(it)
      } catch (e: StatusException) {
        // Do nothing for NOT_FOUND and DATA_LOSS errors.
        if (e.status.code !in listOf(Status.Code.NOT_FOUND, Status.Code.DATA_LOSS)) {
          throw e
        }
      }
    }

    globalComputationsClient.confirmGlobalComputation(
      availableRequisitions.toConfirmGlobalComputationRequest()
    )

    return if (availableRequisitions.size != requisitionsToConfirm.size) {
      logger.warning(
        "Computation failed due to missing requisitions: " +
          "$token, $requisitionsToConfirm, $availableRequisitions"
      )
      completeComputation(token, CompletedReason.FAILED)
    } else {
      val bytes =
        bytesLists
          .joinToString(separator = "") { it.toString(Charset.defaultCharset()) }
          .toByteArray(Charset.defaultCharset())
      // cache the combined local requisitions to blob store.
      val nextToken = storageClients.writeSingleOutputBlob(token, bytes)
      storageClients.transitionComputationToStage(
        nextToken,
        inputsToNextStage = nextToken.outputPathList(),
        stage = if (nextToken.role == RoleInComputation.PRIMARY)
          LiquidLegionsStage.WAIT_SKETCHES else LiquidLegionsStage.WAIT_TO_START
      )
    }
  }

  /** Process computation in the TO_ADD_NOISE stage */
  private suspend fun addNoise() {
    TODO("Not yet implemented")
  }

  /** Process computation in the TO_APPEND_SKETCHES_AND_ADD_NOISE stage */
  private suspend fun appendSketchesAndAddNoise() {
    TODO("Not yet implemented")
  }

  /** Process computation in the TO_BLIND_POSITIONS stage */
  private suspend fun blindPositions(token: ComputationToken): ComputationToken {
    // TODO: Catch permanent errors and terminate the Computation

    val (bytes, nextToken) =
      if (token.singleOutputBlobMetadata().path.isNotEmpty()) {
        // Reuse cached result if it exists
        CachedResult(ByteString.copyFrom(storageClients.readSingleOutputBlob(token)), token)
      } else {
        // compute a new result if no cache exists
        val newResult = cryptoWorker.BlindOneLayerRegisterIndex(
          BlindOneLayerRegisterIndexRequest.newBuilder()
            .setCompositeElGamalKeys(cryptoKeySet.clientPublicKey)
            .setCurveId(cryptoKeySet.curveId.toLong())
            .setLocalElGamalKeys(cryptoKeySet.ownPublicAndPrivateKeys)
            .setSketch(readAndCombineAllInputBlobs(token, 1))
            .build()
        ).sketch
        // cache the newly computed results
        val newToken = storageClients.writeSingleOutputBlob(token, newResult.toByteArray())
        CachedResult(newResult, newToken)
      }

    // Pass the computation to the next duchy.
    (workerStubs[nextToken.nextDuchy] ?: error("Cannot find the target of the next duchy"))
      .handleConcatenatedSketch(
        bytes.chunkedFlow()
          .map {
            HandleConcatenatedSketchRequest.newBuilder()
              .setComputationId(nextToken.globalComputationId)
              .setPartialSketch(it)
              .build()
          }
      )
    return storageClients.transitionComputationToStage(
      nextToken,
      inputsToNextStage = nextToken.outputPathList(),
      stage = LiquidLegionsStage.WAIT_FLAG_COUNTS
    )
  }

  /** Process computation in the TO_BLIND_POSITIONS_AND_JOIN_REGISTERS stage */
  private suspend fun blindPositionsAndJoinRegisters(token: ComputationToken): ComputationToken {
    val (bytes, nextToken) =
      if (token.singleOutputBlobMetadata().path.isNotEmpty()) {
        // Reuse cached result if it exists
        CachedResult(ByteString.copyFrom(storageClients.readSingleOutputBlob(token)), token)
      } else {
        // compute a new result if no cache exists
        val newResult = cryptoWorker.BlindLastLayerIndexThenJoinRegisters(
          BlindLastLayerIndexThenJoinRegistersRequest.newBuilder()
            .setCompositeElGamalKeys(cryptoKeySet.clientPublicKey)
            .setCurveId(cryptoKeySet.curveId.toLong())
            .setLocalElGamalKeys(cryptoKeySet.ownPublicAndPrivateKeys)
            .setSketch(readAndCombineAllInputBlobs(token, 1))
            .build()
        ).flagCounts
        // cache the newly computed results
        val newToken = storageClients.writeSingleOutputBlob(token, newResult.toByteArray())
        CachedResult(newResult, newToken)
      }

    // Pass the computation to the next duchy.
    (workerStubs[nextToken.nextDuchy] ?: error("Cannot find the target of the next duchy"))
      .handleEncryptedFlagsAndCounts(
        bytes.chunkedFlow()
          .map {
            HandleEncryptedFlagsAndCountsRequest.newBuilder()
              .setComputationId(nextToken.globalComputationId)
              .setPartialData(it)
              .build()
          }
      )
    return storageClients.transitionComputationToStage(
      nextToken,
      inputsToNextStage = nextToken.outputPathList(),
      stage = LiquidLegionsStage.WAIT_FLAG_COUNTS
    )
  }

  /** Process computation in the TO_DECRYPT_FLAG_COUNTS stage */
  private suspend fun decryptFlagCounts(token: ComputationToken): ComputationToken {
    val (bytes, nextToken) =
      if (token.singleOutputBlobMetadata().path.isNotEmpty()) {
        // Reuse cached result if it exists
        CachedResult(ByteString.copyFrom(storageClients.readSingleOutputBlob(token)), token)
      } else {
        // compute a new result if no cache exists
        val newResult = cryptoWorker.DecryptOneLayerFlagAndCount(
          DecryptOneLayerFlagAndCountRequest.newBuilder()
            .setCurveId(cryptoKeySet.curveId.toLong())
            .setLocalElGamalKeys(cryptoKeySet.ownPublicAndPrivateKeys)
            .setFlagCounts(readAndCombineAllInputBlobs(token, 1))
            .build()
        ).flagCounts
        // cache the newly computed results
        val newToken = storageClients.writeSingleOutputBlob(token, newResult.toByteArray())
        CachedResult(newResult, newToken)
      }

    // Pass the computation to the next duchy.
    (workerStubs[nextToken.nextDuchy] ?: error("Cannot find the target of the next duchy"))
      .handleEncryptedFlagsAndCounts(
        bytes.chunkedFlow()
          .map {
            HandleEncryptedFlagsAndCountsRequest.newBuilder()
              .setComputationId(nextToken.globalComputationId)
              .setPartialData(it)
              .build()
          }
      )
    // This duchy's responsibility for the computation is done. Mark it COMPLETED locally.
    return completeComputation(nextToken, CompletedReason.SUCCEEDED)
  }

  /** Process computation in the TO_DECRYPT_FLAG_COUNTS_AND_COMPUTE_METRICS stage */
  private suspend fun decryptFlagCountsAndComputeMetrics() {
    TODO("Not yet implemented")
  }

  private suspend fun readAndCombineAllInputBlobs(token: ComputationToken, count: Int): ByteString {
    val blobMap = storageClients.readInputBlobs(token)
    require(blobMap.size == count) {
      "Unexpected number of input blobs. expected $count, actual ${blobMap.size}."
    }
    return ByteString.copyFrom(blobMap.values.fold(ByteArray(0)) { acc, e -> acc.plus(e) })
  }

  /** Partition a bytestring to a flow of chunks. */
  private fun ByteString.chunkedFlow(): Flow<ByteString> = flow {
    for (begin in 0 until size() step chunkSize) {
      emit(substring(begin, minOf(size(), begin + chunkSize)))
    }
  }

  private suspend fun completeComputation(token: ComputationToken, reason: CompletedReason):
    ComputationToken {
      return storageClients.computationStorageClient.finishComputation(
        FinishComputationRequest.newBuilder()
          .setToken(token)
          .setEndingComputationStage(
            ComputationStage.newBuilder()
              .setLiquidLegionsSketchAggregation(LiquidLegionsStage.COMPLETED)
          )
          .setReason(reason)
          .build()
      )
        .token
    }

  private fun RequisitionKey.toStreamMetricValueRequest(): StreamMetricValueRequest {
    return StreamMetricValueRequest.newBuilder()
      .setResourceKey(
        ResourceKey.newBuilder()
          .setDataProviderResourceId(dataProviderId)
          .setCampaignResourceId(campaignId)
          .setMetricRequisitionResourceId(metricRequisitionId)
      )
      .build()
  }

  private fun MutableSet<RequisitionKey>.toConfirmGlobalComputationRequest():
    ConfirmGlobalComputationRequest {
      return ConfirmGlobalComputationRequest.newBuilder()
        .addAllReadyRequisitions(
          this.map { key ->
            MetricRequisition.Key.newBuilder()
              .setCampaignId(key.campaignId)
              .setDataProviderId(key.dataProviderId)
              .setMetricRequisitionId(key.metricRequisitionId)
              .build()
          }
        ).build()
    }

  private data class CachedResult(val bytes: ByteString, val token: ComputationToken)
}
