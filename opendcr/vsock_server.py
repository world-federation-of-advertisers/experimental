class VsockServer():

    def __init__():
        # Vsock Server store deks already decrypted using kms_cli_tool
        self.deks = {}
        self.server_executor = ThreadPoolExecutor(max_workers=10)

    def start_receiving(self, port):
        while True:
            try:
                (from_client, (remote_cid, remote_port)) = self.sock.accept()
                self.server_executor.submit(self.recv_data, from_client)
            except Exception:
                time.sleep(0.1)

    def recv_data(self, from_client):
        decryption = Decryption()
        data_store_id = vsock_payload.metadata.data_store.data_store_id
        chunk_size = 20 * 1024 * 1024
        try:
            while True:
                # Read the entire file in 20MB chunks until it has been completely processed.
                # Uses Decryption class to decrypt it's content.
                # At this execution point the VsockSer
                chunk = from_client.recv(chunk_size)
                if not chunk:
                    decryption.decrypt_chunk(
                        chunk,
                        self.deks.get(data_store_id),
                        vsock_payload
                    )
                    break
                decryption.decrypt_chunk(
                    chunk,
                    self.deks.get(data_store_id),
                    vsock_payload
                )
        except Exception:
            # Handle Exception
        finally:
            # Free resources taken by python subprocess which uses `fork()` under the hood
            decryption.force_cleanup()
            del decryption
            gc.collect()
