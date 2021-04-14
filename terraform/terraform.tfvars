project_name = "{PROJECT_NAME}"
region = "us-east-1"

vpc_name = "{VPC_NAME}"
vpc_cidr = "172.16.192.0/25"
vpc_azs = [ "us-east-1a","us-east-1c"]
vpc_private_subnets = ["172.16.192.64/27","172.16.192.96/27",]
vpc_public_subnets = ["172.16.192.0/27","172.16.192.32/27",]
vpc_enable_nat_gateway = true
vpc_enable_dns_hostname = true
vpc_single_nat_gateway = true
vpc_tags = {}

wfa_bucket_name = "wfa-example-bucket"
wfa_bucket_acl = "private"
wfa_s3_with_versioning = true
wfa_s3_tags = {}

eks_name = "wfa-example-eks"
eks_version = "1.18"
eks_worker_group_name = "worker-group-example"
eks_worker_group_instance_type = "m5.large"
eks_worker_group_capacity = 3
eks_worker_default_root_volume_type = "gp2"

eks_tags = {
    date  = "Sun Mar 28 17:49:32 PDT 2021"
    purpose = "wfa example eks"
}