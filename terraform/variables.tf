variable "project_name" {
    type = string
}

variable "region" {
    type = string 
}

variable "vpc_name" {
    description = "vpc name"
    type = string
}

variable "vpc_cidr" {
    description = "vpc cidr"
    type = string
}

variable "vpc_azs" {
    description = "Availability zones for VPC"
    type = list
}

variable "vpc_private_subnets" {
    description = "Private subnets for VPC"
    type = list(string)
}

variable "vpc_public_subnets" {
    description = "Public subnets for VPC"
    type = list(string)
}

variable "vpc_enable_nat_gateway" {
    description = "Enable NAT gateway for VPC"
    type = bool
}

variable "vpc_enable_dns_hostname" {
    description = "Enable DNS hostnames for VPC"
    type = bool
}

variable "vpc_single_nat_gateway" {
    description = "single nat gateway"
    type = bool
}

variable "vpc_tags" {
    type = map
}

variable "wfa_bucket_name" {
    description = "name for wfa bucket"
    type = string
}

variable "wfa_bucket_acl" {
    description = "bucket acl"
    type = string
}

variable "wfa_s3_with_versioning" {
    description = "enable bucket versioning"
    type = bool
}

variable "wfa_s3_tags" {
    description = "s3 tags"
    type = map  
}
variable "eks_name" {
    description = "eks cluster name"
    type = string
}

variable "eks_version" {
    description = "eks cluster version"
    type = string
}

variable "eks_worker_group_name" {
    description = "name for eks worker group"
    type = string
}

variable "eks_worker_group_instance_type" {
    description = "instance type for worker group"
    type = string
}

variable "eks_worker_group_capacity" {
    description = "Capacity of worker group"
    type = number
}

variable "eks_worker_default_root_volume_type" {
    description = "Default worker root vol type. Valid volume types are standard, io1, gp2, st1 and sc1."
    type = string
    default = "gp2"
}

variable "eks_tags" {
    description = "eks tags"
    type = map
}


