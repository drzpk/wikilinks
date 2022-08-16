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

In order to run this project locally, first use [the generator](./generator/README.md) to create indexes for languages
you want to use. After the generation is complete, [the application](./application/README.md) can be started. The Docker
examples available in the respective module readme files are configured to use a shared database directory - generator
moves complete indexes into that directory, from where they are automatically picked up by the application (scan
interval is 1 minute).

Wikipedia releases new dumps two times a month (on 1st and 20th), so the generator should be launched the next day (some
files may not be available immediately).

### AWS

AWS usage is described in [the Terraform directory](./terraform/README.md).

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
* `terraform/` - AWS resources definitions

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
