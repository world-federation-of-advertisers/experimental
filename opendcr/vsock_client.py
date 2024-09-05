class VsockClient:

    def open_connection(self, remote_cid, remote_port):
        self.sock = socket.socket(socket.AF_VSOCK, socket.SOCK_STREAM)
        self.sock.settimeout(self.conn_tmo)
        self.sock.connect((remote_cid, remote_port))

    def close_connection(self):
        self.sock.close()

    def send_metadata(self, vsock_data):
        # Send only metadata info (which is a pydantic object).
        # Make a copy not to delete the field `data` from the original vsock_data.
        if vsock_data.data:
            vsock_data = VsockData(
                metadata=vsock_data.metadata,
            )
        vsock_data.data = None
        metadata_json = vsock_data.json()
        metadata_encoded = metadata_json.encode("utf-8")
        message_length = struct.pack("!I", len(metadata_encoded))
        self.sock.sendall(message_length + metadata_encoded)

    def send_data_store_file(
        self, vsock_data, send_data_store_part=None
    ):

        chunk_size = 20 * 1024 * 1024
        # vsock_data.data is not s3_response got at line 13 in s3_downloader
        total_size = vsock_data.data["ContentLength"]
        for chunk in vsock_data.data["Body"].iter_chunks(
            chunk_size=chunk_size
        ):
            self.sock.sendall(chunk)

    def send_data_store_part(self, vsock_enclave_request):
        """Stream a data store part bytes array to the server"""

        try:
            self.send_request(vsock_enclave_request)
            local_file_path = vsock_enclave_request.data_stores[0].local_file_path
            total_size = 0
            if os.path.isfile(local_file_path):
                size = os.stat(local_file_path).st_size
                total_size += size

            chunk_size = 20 * 1024 * 1024

            internal_request_id = vsock_enclave_request.internal_request_id

            # This buffer mechanism is used for managing the upload of multiple files in parallel.
            # There is a buffer per internal request id.
            # Internal request id identifies an upload operation of a single file.
            with VsockClient.request_buffers_lock:
                if internal_request_id in VsockClient.request_buffers:
                    request_buffer = VsockClient.request_buffers[internal_request_id]
                else:
                    request_buffer = b""

            data = None
            # In here we just read the file from local mounted memory and we send it to the server
            # which in this case is the EC2 application.
            with open(local_file_path, "rb") as file:
                while True:
                    while len(request_buffer) < chunk_size:
                        data = file.read(
                            chunk_size - len(request_buffer)
                        )
                        if not data:
                            break
                        request_buffer += data

                    while len(request_buffer) >= chunk_size:
                        self.sock.sendall(
                            request_buffer[:chunk_size]
                        )
                        request_buffer = request_buffer[
                            chunk_size:
                        ]

                    if not data:
                        break
                    
            with VsockClient.request_buffers_lock:
                VsockClient.request_buffers[internal_request_id] = request_buffer
            
            # Delete the file from the local storage to free memory
            self.file_system_manager.delete_file(local_file_path)

        except Exception as e:
            Logger.log(
                f"Exception in send_data_store_part: {e}", LoggingType.EXCEPTION
            )
