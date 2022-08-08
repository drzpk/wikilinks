# WikiLinks

[WikiLinks](https://wikilinks.drzepka.dev) is a website that implements the concept
of [six degrees of separation](https://en.wikipedia.org/wiki/Six_degrees_of_separation).
It allows a user to find the shortest possible hyperlink connections between two arbitrary Wikipedia articles in
different languages.

## How it works

WikiLinks consists of two main modules:

* `generator` - analyzes [Wikipedia database dumps](https://dumps.wikimedia.org/backup-index.html), extracts link data
  and stores it in a SQLite database in format that allows searching for connections in a fast manner.
* `application` - a web application that reads the SQLite database and searches for article links.

The application module comes in two variants: *JVM* and *NATIVE*. In the latter, sources are compiled into native binary
code that runs on Linux (x86_64). They work exactly the same, but the native variant works slightly faster and uses less
memory.

More detailed information on how different modules of the project work is located in their respective directories.

## Usage

This project can be launched using standalone Docker images or within AWS. The standalone option doesn't include reverse
proxy and HTTP configuration, users of this project must set it up on their own.

The application module runs continuously, whereas the generator module processes a Wikipedia dump and exists.
Generator compares the most recent Wikipedia dump available for given language with the current version and exits if
update is not needed. Generator should be run periodically. Wikipedia releases new dumps on 1st and 20th
day of each month, but some files may take a longer time to become available,
so it's better to run generator the next day (`0 0 2,21 * *`).

Environment variables:

1. Generator
    * `DATABASES_DIRECTORY` - **required** - SQLite database files location
    * `WORKING_DIRECTORY` - **required** - Directory to download Wikipedia dumps and store temporary files
    * `JAVA_TOOL_OPTIONS` - **recommended** - should contain value `-Xmx6G` to limit max amount of memory generator can
      use.
    * `SKIP_DELETING_DUMPS` - optional - if set, Wikipedia dumps won't be deleted after processing is done, useful for
      debugging purposes.
    * `BATCH_MODE` - optional - disables interactive mode (progress updates are printed periodically, each in new line)
2. Application
    * `DATABASES_DIRECTORY` - **required** - should point to the same directory as in the generator module.

The `DATABASES_DIRECTORY` variable must point to the same host directory.

Generator requires minimum 6GB of heap memory. This is because at some point it needs to store
all pageId-pageTitle mappings in memory. This requirement is for the english version of Wikipedia, which
is [the largest](https://en.wikipedia.org/wiki/List_of_Wikipedias#Details_table). Other languages require proportionally
less memory.

### Docker images

Generator:

```shell
#!/bin/sh
docker run \
  --rm \
  -it \
  --name wikilinks-application \
  -p 8080:8080 \
  -v "$(pwd)/databases:/databases" \
  -v "$(pwd)/dumps:/dumps" \
  -e DATABASES_DIRECTORY=/databases \
  -e WORKING_DIRECTORY=/dumps \
  ghcr.io/drzpk/wikilinks/generator:latest \
  language=pl,en
```

Program arguments:
* `language` (**required**) - a comma-separated list of [ISO 639-1](https://en.wikipedia.org/wiki/ISO_639-1) language codes for which to generate search indexes. Supported languages are listed [here](common/src/commonMain/kotlin/dev/drzepka/wikilinks/common/model/dump/DumpLanguage.kt).
* `version` (*optional*) - forces generator to use specific version of Wikipedia dump.

**Note**: don't forget to define the `SKIP_DELETING_DUMPS` environment variable if you wish to retain Wikipedia dumps
downloaded by Generator. Otherwise, they will be deleted upon successful completion.

Application:

```shell
#!/bin/sh
docker run \
  --rm \
  -it \
  --name wikilinks-application \
  -p 8080:8080 \
  -v "$(pwd)/databases:/databases" \
  -e DATABASES_DIRECTORY=/databases \
  ghcr.io/drzpk/wikilinks/application-jvm:latest
```

### AWS

WikiLinks uses Terraform to define AWS infrastructure. The most notable resources include:

* **EC2 instances**:
    * ECS node, t3.micro, the application module is deployed here
    * Bastion host, t3.micro, disabled by default (used for debugging only)
* **Batch** - defines the generator job
* **EFS** - used to store all databases and temporary files
* **HTTP API** - exposes the application to the Internet
* **EventBridge Rule** - periodically launches the generator job

#### Creating the infrastructure

Required software:

* [AWS CLI](https://aws.amazon.com/cli/)
* [Terraform](https://www.terraform.io/downloads)

Inside the `terraform/src` directory, create a new file named `terraform.tfvars` with the following content:

```
aws_region = "eu-west-1"
```

Value of `aws_region` may be any available AWS region. To see all available variables, refer
to `terraform/src/variables.tf`.

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
jobDefn=$(echo $state | jq -r '.values.root_module.resources | map(select(.address == "aws_batch_job_definition.generator"))[0].values | .name + ":" + (.revision | tostring)')
jobQueue=$(echo $state | jq -r '.values.root_module.resources | map(select(.address == "aws_batch_job_queue.queue"))[0].values.name')
aws batch submit-job --job-definition $jobDefn --job-queue $jobQueue --job-name generator-init
```

## Development guide

JDK 11 or greater is required.

### Project structure

* `application/` - the application module - ties together the backend and frontend modules into a single Docker image.
  Also contains integrations tests for the image.
    * `src/docker/` - base images and entry points for application Docker images
* `backend/` - the backend module - responsible for finding links between articles, can be compiled into two targets:
  JVM and Linux
    * `lib/` - libraries required for compilation when targeting Linux
    * `src/commonMain/sqldelight` - [SQLDelight](https://cashapp.github.io/sqldelight/) database definition files
* `common/` - the common module - contains code shared between the rest of modules
* `frontend/` - the frontend module
* `generator/` - the generator module - analyzes Wikipedia database dumps and converts them to a format suitable for
  fast link searching.
* `terraform/` - AWS resources definition

### Building the project

#### Docker images

```shell
gradlew :generator:jibDockerBuild
gradlew :application:jibDockerBuild -DjibConfiguration=JVM
```

The `jibConfiguration` system property is mandatory and tells Gradle which image variant to build. Supported
values: `JVM` or `NATIVE`.

#### Compilation

The following commands will build different modules of the project into a standalone package or executable.

* `gradlew :generator:jar` - output location: `generator/build/libs`
* `gradlew :backend:jvmJar` - output location: `backend/build/libs`
* `gradlew :backend:linkReleaseExecutableLinuxX64` - output location: `backend/build/bin/linuxX64/releaseExecutable`
* `gradlew :frontend:browserDistribution` - output location: `frontend/build/distributions`
* `gradlew :frontend:zip` - output location: `frontend/build/libs`

### Running the project from IDE

Initial setup:

1. Import the project into an IDE
2. Create the run configurations
    1. Backend (JVM)
        1. Main class: `dev.drzepka.wikilinks.app.JvmMainKt`
        2. Program arguments: `http`
        3. Required environment variables: same as in the usage section
    2. Frontend - Gradle task: `gradle :frontend:browserDevelopmentRun --continuous`
    3. Generator
        1. Main class: `dev.drzepka.wikilinks.generator.MainKt`
        2. VM options: `-Xmx7G`
        3. Required environment variables: same as in the usage section

When the frontend Gradle task is started with the `--continuous` flag, the code changes are automatically detected
and the module is recompiled. Frontend expects the backend module to be up and running unless the `Frontend#USE_MOCKS`
constant is set to `true`.

#### Targeting Linux (Backend module)

Running the backend module with Linux target outside the Docker image requires the following packages to be installed:

* libsqlite3-0 3.22.0
* libcurl4-openssl-dev

The exact specified version of `libsqlite3-0` must be used, because the SQLiter library (WikiLinks -> SQLDelight ->
SQLiter) was built in Ubuntu 18.04, which used the specified version of that library.

[Installation script (Ubuntu)](application/src/docker/Dockerfile):

```shell
wget http://security.ubuntu.com/ubuntu/pool/main/s/sqlite3/libsqlite3-0_3.22.0-1ubuntu0.5_amd64.deb
sudo apt install ./libsqlite3-0_3.22.0-1ubuntu0.5_amd64.deb
sudo apt install libcurl4-openssl-dev
sudo ln -s /usr/lib/x86_64-linux-gnu/libsqlite3.so.0 /usr/lib/x86_64-linux-gnu/libsqlite3.so
```

The last command is a workaround that should be executed if the Kotlin compiler reports the `sqlite3` library missing
even after installing the package.
