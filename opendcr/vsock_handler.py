class VsockHandler:
    def __init__(...):
        # Vsock Handler uses a Thread Pool executor to download up to 10 files in parallel
        self.client_executor = ThreadPoolExecutor(max_workers=10)

    # This method captures the event published from s3_downloader
    def handle_event(self, ...):
        self.client_executor.submit(
            # vsock_data contains metadata useful to identify the file being downloaded inside the enclave
            self._send_data, event.vsock_data
        )
        # Uploading a file to S3 - STEP 2
        # Capture event from vsock handler put them in a queue 
        if event.vsock_enclave_request.request_type == RequestType.SAVE_DATA_STORE:
            with self.request_queues_lock:
                internal_request_id = (
                    event.vsock_enclave_request.internal_request_id
                )
                if internal_request_id not in self.request_queues:
                    self.request_queues[internal_request_id] = queue.Queue()
                self.request_queues[internal_request_id].put(
                    event.vsock_enclave_request
                )

    def _send_data(self, vsock_data):
        client = VsockClient(1200)
        client.open_connection(self.remote_cid, self.remote_port)
        client.send_metadata(vsock_data)
        client.send_data_store_file(vsock_data)
        client.close_connection()

    def requests_queue_listener(self):
        """Check the result queue and process one file at the time."""
        while True:
            with self.request_queues_lock:
                keys = list(self.request_queues.keys())

            for internal_request_id in keys:
                with self.request_queues_lock:
                    if internal_request_id in self.request_queues:
                        request_queue = self.request_queues[internal_request_id]
                        if not request_queue.empty():
                            vsock_enclave_request = request_queue.get()
                            client = VsockClient(1200)
                            client.open_connection(self.remote_cid, self.remote_port)

                            if (
                                vsock_enclave_request.data_stores[
                                    0
                                ].multipart_upload_completed
                                is True
                            ):
                                # Either complete the multipart upload
                                # `multipart_upload_completed` is set to True when the last part is sent.
                                # The last part is marked as such from within the enclave application and its
                                # developer resposibility to notify when the last part is sent.
                                client.complete_data_store_multipart(
                                    vsock_enclave_request
                                )

                            else:
                                # Or send the next part
                                client.send_data_store_part(vsock_enclave_request)

                            client.close_connection()

            time.sleep(0.1)
