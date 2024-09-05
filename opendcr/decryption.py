class Decryption:

    def handle_stdout(self):
        """Continuously read and accumulate data from stdout."""
        with open(self.file_path, "wb") as memfile:
            chunk_size = 10 * 1024 * 1024
            while not self.stop_event.is_set():
                data = self.process.stdout.read(chunk_size)
                if not data:
                    break
                memfile.write(data)
                memfile.flush()

    def handle_stderr(self):
        """Continuously read from stderr and log errors."""
        while not self.stop_event.is_set():
            line = self.process.stderr.readline()
            if not line:
                break
            decoded_line = line.decode().strip()

    def init_subprocess(self, dek):
        try:
            dek = dek.strip()
            self.process = subprocess.Popen(
                [
                    "openssl",
                    "enc",
                    "-d",
                    "-aes-256-cbc",
                    "-pass",
                    f"pass:{dek}",
                ],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                bufsize=100 * 1024 * 1024,
            )

            self.setup_threads()
            self.is_subprocess_ready = True
        except Exception:
            # Handle exception

    def setup_threads(self):
        self.stdout_thread = threading.Thread(target=self.handle_stdout)
        self.stderr_thread = threading.Thread(target=self.handle_stderr)
        self.stdout_thread.start()
        self.stderr_thread.start()

    def cleanup(self):
        if self.process:
            self.process.stdin.close()

        self.stdout_thread.join(timeout=20)
        self.stderr_thread.join(timeout=20)

        if self.process:
            self.process.stdout.close()
            self.process.stderr.close()
            self.process.wait(timeout=20)
            self.process.kill()
            self.process = None

        gc.collect()

        self.is_subprocess_ready = False

    def decrypt_chunk(self, chunk, dek, vsock_data):
        try:
            if not self.is_subprocess_ready:
                self.file_path = # Local path poiting to a mounted memory path in RAM
                self.init_subprocess(dek)

            if chunk:
                self.process.stdin.write(chunk)
                self.process.stdin.flush()
            else:
                vsock_data.metadata.data_store.data_store_file_path = self.file_path
                self.cleanup()
                self.ec2_even_bus.publish(
                    DataStoreFileAvailableEvent(
                        vsock_data
                    )
                )

        except Exception as e:
            self.force_cleanup()
            return None

    def force_cleanup(self):
        """Forcibly terminate all threads and processes."""
        self.stop_event.set()

        Logger.log(f"Invoking force_cleanup!", LoggingType.INFO)

        if self.process:
            try:
                self.process.kill()
                self.process.wait()
                self.process = None
            except Exception as e:
                Logger.log(
                    f"Error forcefully killing process: {e}",
                    LoggingType.EXCEPTION,
                )

        if self.stdout_thread and self.stdout_thread.is_alive():
            try:
                Logger.log(
                    f"Forcefully terminating stdout_thread",
                    LoggingType.EXCEPTION,
                )
            except Exception as e:
                Logger.log(
                    f"Error forcefully terminating stdout_thread: {e}",
                    LoggingType.EXCEPTION,
                )

        if self.stderr_thread and self.stderr_thread.is_alive():
            try:
                Logger.log(
                    f"Forcefully terminating stderr_thread",
                    LoggingType.EXCEPTION,
                )
            except Exception as e:
                Logger.log(
                    f"Error forcefully terminating stderr_thread: {e}",
                    LoggingType.EXCEPTION,
                )

        gc.collect()
        self.is_subprocess_ready = False

    def decrypt_dek(self, aws_region, aws_credentials, dek):
        gc.collect()
        proc = subprocess.Popen(
            [
                "/app/enclave/kmstool_enclave_cli",
                "decrypt",
                "--region",
                aws_region,
                "--proxy-port",
                "8000",
                "--aws-access-key-id",
                aws_credentials["accessKey"],
                "--aws-secret-access-key",
                aws_credentials["secretKey"],
                "--aws-session-token",
                aws_credentials["sessionKey"],
                "--ciphertext",
                dek,
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        stdout, stderr = proc.communicate()
        stdout_decoded = stdout.decode()
        stderr_decoded = stderr.decode()
        
        clear_dek = stdout_decoded.split(": ")[1]
        return clear_dek
