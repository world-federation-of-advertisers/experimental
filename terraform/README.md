# wfa sample stack
This is a repo for sample terraform template of wfa project.

* main.tf: contains the resource and aws module declarations
* variables.tf: contains all the variable declarations
* terraform.tfvars: contains all the actual values to be passed over to create the resources.
* outputs.tf: contains all the return values user wants from Terraform.
  
To apply the template:
* Download the terraform binary from https://www.terraform.io/downloads.html
* move the binary to $PATH
* Run the following commands:
```
terraform init
terraform plan # dry-run and get a preview of the resource state changes to ensure that changes are expected
terraform apply
```

After the whole stack is provisioned, follow [this instruction](https://learn.hashicorp.com/tutorials/terraform/eks#configure-kubectl) to access the eks cluster.