# Flow

*s3_downloader (ec2)* -> *vsock_handler (ec2)* -> *vsock_client (ec2)* -> *vsock_server (enclave)* -> *decryption (enclave)*

The *s3_downloader* pulls files descriptors from s3 using boto3 and for each publish an event that is capture from *vsock_handler*.
Files are in Parquet format.

*vsock_handler* uses a thread pool executor to download up to 10 files in parallel in chunks and every chunk, once received, is immediately send into the enclave using *vsock_client*.

*vsock_server* inside the enclave uses other 10 threads to receive and process 10 files in parallel. *vsock_server* uses a class (*Decryption*) to decrypt all the chunks as they arrive and write them into a file stored in a mounted path in the RAM.

Once the files are made available to the application inside the enclave these are processed (merged with another dataframe of ~200MB that is downloaded as first file and kept in memory the whole time) and the resulting Parquet file is sent back to S3 using multipart upload.
So there are files being downloaded and files being uploaded in parallel for pretty much the whole time.

## Performances

All files are ~0.5GB each.
Joining parquet files takes between 5-7 seconds using Polars, constantly through the whole processing time.
Initial 10 files are downloaded and decrypted in about 5 seconds. This deteriorates to ~30 / 45 seconds at the end of the process.
