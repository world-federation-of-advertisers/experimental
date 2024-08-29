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
