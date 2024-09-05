# Uploading a file to S3 - STEP 1
def upload_part(self, multipart_upload, file):        
    """Function used from the Enclave app to send a result part to S3"""
    save_data_store_request = DataStoreRequest(
        data_store_id=multipart_upload.data_store_id,
        remote_file_keys=[multipart_upload.remote_file_key],
        local_file_path=local_file_path, # This is the path to the file stored in RAM
        multipart_upload_completed=False
    )

    # Publish an event for the vsocket handler to send the data to the EC2
    self.enclave_even_bus.publish(
        SendEnclaveRequestEvent(
            VsockEnclaveRequest(
                request_type=RequestType.SAVE_DATA_STORE,
                data_stores=[save_data_store_request],
                internal_request_id=multipart_upload.internal_request_id,
            )
        )
    )
