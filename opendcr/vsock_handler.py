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

    def _send_data(self, vsock_data):
        client = VsockClient(1200)
        client.open_connection(self.remote_cid, self.remote_port)
        client.send_metadata(vsock_data)
        client.send_data_store_file(vsock_data)
        client.close_connection()
