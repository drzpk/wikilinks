# WikiLinks - AWS infrastructure in Terraform

WikiLinks uses Terraform to define AWS infrastructure. It comes in two variants:

1. `full` - defines the complete application and exposes it through API Gateway.
   ![Terraform full infrastructure](./../img/terraform-full.svg)
2. `generator-only` - only creates resources needed for index generation. Generated files are stored in S3.
   ![Terraform generator-only infrastructure](./../img/terraform-generator-only.svg)

#### Creating the infrastructure

Required software:

* [AWS CLI](https://aws.amazon.com/cli/)
* [Terraform](https://www.terraform.io/downloads)

Inside the `terraform/src` directory, create a new file named `terraform.tfvars` with the region and variant:

```
aws_region = "eu-west-1"
variant    = "full"
```

To see all available variables, refer to `terraform/src/variables.tf`.

After [configuring AWS credentials](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html),
type the following command to provision the infrastructure:

```
terraform apply -auto-approve
```

**Note**, that after the initial provisioning, it's necessary to manually to submit the generator job. This is a
one-time action. It can be done either from the AWS Console (by using the *"submit new job"* button in the job
definition details) or by using the following command:

```shell
#!/bin/bash
state=$(terraform show -json)
jobDefn=$(echo $state | jq -r '.values.root_module.child_modules[].resources | map(select(.address == "module.batch.aws_batch_job_definition.generator"))[0].values | .name + ":" + (.revision | tostring)')
jobQueue=$(echo $state | jq -r '.values.root_module.child_modules[].resources | map(select(.address == "module.batch.aws_batch_job_queue.queue"))[0].values.name')
aws batch submit-job --job-definition $jobDefn --job-queue $jobQueue --job-name generator-init
```
