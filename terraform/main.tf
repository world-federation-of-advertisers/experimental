provider "aws" {
    profile = var.project_name
    region = var.region
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority.0.data)
  token                  = data.aws_eks_cluster_auth.cluster.token
  exec {
    api_version = "client.authentication.k8s.io/v1alpha1"
    command     = "aws"
    args = [
      "eks",
      "get-token",
      "--cluster-name",
      data.aws_eks_cluster.cluster.name
    ]
  }
}

data "aws_caller_identity" "current" {
}
data "aws_region" "current" {
}

data "aws_eks_cluster" "cluster" {
  name = module.wfa_eks.cluster_id
}

data "aws_eks_cluster_auth" "cluster" {
  name = module.wfa_eks.cluster_id
}

module "wfa_vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "2.77.0"

  name = var.vpc_name
  cidr = var.vpc_cidr

  azs             = var.vpc_azs
  private_subnets = var.vpc_private_subnets
  public_subnets  = var.vpc_public_subnets

  enable_nat_gateway = var.vpc_enable_nat_gateway
  enable_dns_hostnames = var.vpc_enable_dns_hostname

  single_nat_gateway = var.vpc_single_nat_gateway

  tags = var.vpc_tags
}

module "wfa_s3_bucket" {
  source = "terraform-aws-modules/s3-bucket/aws"

  bucket = var.wfa_bucket_name
  acl    = var.wfa_bucket_acl


  versioning = {
    enabled = var.wfa_s3_with_versioning
  }

  tags = var.wfa_s3_tags

}

module "wfa_eks" {
    source = "terraform-aws-modules/eks/aws"
    cluster_name = var.eks_name
    cluster_version = var.eks_version
    subnets = module.wfa_vpc.private_subnets
    vpc_id = module.wfa_vpc.vpc_id
    worker_groups = [
        {
            name = var.eks_worker_group_name
            instance_type = var.eks_worker_group_instance_type
            asg_desired_capacity = var.eks_worker_group_capacity
        }
    ]
    workers_group_defaults = {
  	    root_volume_type = var.eks_worker_default_root_volume_type
    }
    tags = var.eks_tags
}



