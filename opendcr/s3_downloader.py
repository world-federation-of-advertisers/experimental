def stream_files_from_s3(
        self,
        remote_files_list,
        internal_request_id,
        data_content,
        bucket_number=None
  ):
  # ... files_to_stream is a queue containing all the s3 keys to download (potentially thousands of 0.5GB each)
  while files_to_stream:
      file = files_to_stream.pop(0)
      bucket_name, item_key, data_store_id = file
      if item_key.endswith(".parquet"):
        s3_response = self.s3_client.get_object(
          Bucket=bucket_name, Key=item_key
        )
        self.event_bus.publish(
            SendDataAsyncEvent(
                # Publish event to notify Vsock Handler that a new S3 file can be streamed into the Enclave
                # The publisher emit events on the same thread of this function
            )
        )
