class ResultWrapper:

    @staticmethod
    def results_uploaded(self, results, results_lock) -> bool:
        results_uploaded = True if len(results) > 0 else False
        with results_lock:
            for internal_request_id, result in results.items():
                if result.status != ResultWrapper.Status.COMPLETED:
                    results_uploaded = False
        return results_uploaded

    class Status(Enum):
        IDLE = "IDLE"
        PROCESSING = "PROCESSING"
        COMPLETING = "COMPLETING"
        COMPLETED = "COMPLETED"

    def __init__(self):
        self.data_queue = queue.Queue()
        self.upload_processes_queue = queue.Queue()
        self.uploaded_part_counter = 1
        self.parts_array = []
        self.status = ResultWrapper.Status.IDLE
        self.upload_id = None
        self._data_queue_lock = Lock()
        self.s3_result_key = None
        self.s3_bucket_name = None
        self.internal_request_id = None

    def push_data_into_queue(self, data):
        with self._data_queue_lock:
            self.data_queue.put(data)


class ResultManager(EventBusObserver):

    def __init__(self) -> None:
        super().__init__()
        self.upload_result_executor = ThreadPoolExecutor(max_workers=8)
        # Results is a dictionary of internal_request_id to ResultWrapper.
        # Once again, there can be multiple files being uploaded in parallel.
        # This `result` dictionary is used to track all of them along with their progresses and statuses.
        self.results = {}
        # ...
        
    def handle_event(self, event):
        if event.topic == EventBusTopic.WORKLOAD_COMPLETED:
            self.results = {}
            self.results_lock = Lock()
        # Handle the event from the vsock server to upload the data to S3
        elif event.topic == EventBusTopic.DATA_STORE_PART_AVAILABLE:
            self.upload_result(event.vsock_data)

    def upload_result(self, vsock_data):
        try:
            internal_request_id = vsock_data.metadata.internal_request_id
            result_file_key = vsock_data.metadata.data_store.data_store_remote_file_key
            with self.results_lock:
                if internal_request_id not in self.results:
                    self.results[internal_request_id] = ResultWrapper()

                    result_bucket_name = (self.config_parser.find_data_store_by_id(vsock_data.metadata.data_store.data_store_id)).bucket_name
                    self.results[internal_request_id].s3_bucket_name = result_bucket_name
                    self.results[internal_request_id].upload_id = self.s3_uploader.start_multipart_upload(result_file_key, result_bucket_name)
                    self.results[internal_request_id].s3_result_key = result_file_key
                    self.results[internal_request_id].internal_request_id = vsock_data.metadata.internal_request_id

            with self.results_lock:
                if vsock_data.data == b"":
                    self.results[internal_request_id].status = ResultWrapper.Status.COMPLETING
                else:
                    self.results[internal_request_id].status = ResultWrapper.Status.PROCESSING
                    self.results[internal_request_id].push_data_into_queue(vsock_data.data)

        except Exception as e:
            Logger.log(
                f"Exception in upload_result: {e}", LoggingType.EXCEPTION
            )

    # Process the self.results dictionary to upload new data to S3
    def process_result_queue(self) -> None:
        while True:
            try:
                with self.results_lock:
                    result_intenal_ids = list(self.results.keys())

                for internal_request_id in result_intenal_ids:
                    self.upload_result_part(
                        self.results[internal_request_id]
                        )

                if ResultWrapper.results_uploaded(self, self.results, self.results_lock):
                    self.ec2_even_bus.publish(
                        WorkloadCompletedEvent()
                    )

                time.sleep(0.2)

            except Exception as e:
                Logger.log(
                    f"Exception in process_result_queue: {e}",
                    LoggingType.EXCEPTION,
                )
                time.sleep(0.5)

    # Function that retrieve chunk of data from a result wrapper queue and upload it to S3.
    def upload_result_part(self, result_wrapper) -> None:
        try:
            if (result_wrapper.data_queue.qsize() >= 14):
                data = []
                # ******************* #
                # ******************* #
                # AS THERE'S A LIMIT OF 10K PARTS IN A MULTIPART UPLOAD, WE HAD TO UPLOAD 14 PARTS AT A TIME,
                # WHICH MEANS 20MB * 14 = ~280MB OF DATA.
                # SO 280MB IS THE CHUNK WE UPLOAD TO S3 PER CALL.
                # ******************* #
                # ******************* #
                for _ in range(14):
                    data.append(result_wrapper.data_queue.get())
                result_wrapper.upload_processes_queue.put(1)
                # WE USE A THREAD POOL EXECUTOR WITH AT MOST 8 THREADS TO UPLOAD THE DATA TO S3
                # 280MB * 8 = 2.24GB OF DATA IN PARALLEL WHILE UPLOADING TO S3.
                # I TRIED WITH MORE AND LESS THREAD, SMALLER AND BIGGER CHUNKS BUT THIS COMBINATION
                # SEEMS TO BE THE FASTEST AND MOST EFFICIENT.
                self.upload_result_executor.submit(
                    self.s3_uploader.upload_result_part,
                    data,
                    result_wrapper.parts_array,
                    result_wrapper.uploaded_part_counter,
                    result_wrapper.upload_processes_queue,
                    result_wrapper.upload_id,
                    result_wrapper.s3_result_key,
                    result_wrapper.s3_bucket_name,
                    result_wrapper.internal_request_id
                )
                result_wrapper.uploaded_part_counter += 1

                del data
                gc.collect()

            elif (
                not result_wrapper.data_queue.empty()
                and result_wrapper.status == ResultWrapper.Status.COMPLETING
            ):
                merged_data = bytearray()
                while not result_wrapper.data_queue.empty():
                    data = result_wrapper.data_queue.get()
                    merged_data.extend(data)
                result_wrapper.upload_processes_queue.put(1)
                self.upload_result_executor.submit(
                    self.s3_uploader.upload_result_part,
                    merged_data,
                    result_wrapper.parts_array,
                    result_wrapper.uploaded_part_counter,
                    result_wrapper.upload_processes_queue,
                    result_wrapper.upload_id,
                    result_wrapper.s3_result_key,
                    result_wrapper.s3_bucket_name,
                    result_wrapper.internal_request_id
                )

                del merged_data
                gc.collect()

                result_wrapper.uploaded_part_counter += 1

            elif (
                result_wrapper.data_queue.empty()
                and result_wrapper.upload_processes_queue.empty()
                and result_wrapper.status == ResultWrapper.Status.COMPLETING
            ):
                response = self.s3_uploader.complete_multipart_upload(
                    result_wrapper.upload_id,
                    result_wrapper.s3_result_key,
                    result_wrapper.s3_bucket_name,
                    result_wrapper.parts_array

                with self.results_lock:
                    result_wrapper.status = ResultWrapper.Status.COMPLETED

        except Exception as e:
            Logger.log(
                f"Exception in upload_result_part: {e}",
                LoggingType.EXCEPTION,
            )
