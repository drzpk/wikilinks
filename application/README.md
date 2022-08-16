# WikiLinks - Application

## Usage

The application module is available in two variants: *JVM* and *NATIVE*.
In the latter, sources are compiled into native binary code that runs on Linux (x86_64). They work exactly the same, but
the native variant works slightly faster and uses less memory.

### Environment variables

* `DATABASES_DIRECTORY` - **required** - should point to the same directory as in the generator module.
* `FRONTEND_RESOURCES_DIRECTORY` - *development-only* - the location of frontend resources. Configured automatically
  when building application's Docker image.

### Docker example

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

Docker image variants:

* ghcr.io/drzpk/wikilinks/application-jvm:latest
* ghcr.io/drzpk/wikilinks/application-native:latest

### Logging configuration

In JVM variant, logging is handled by Log4j2.
The [default configuration file](./../backend/src/jvmMain/resources/log4j2.xml) can be overridden by adding the
following option to a `Docker run` command:
```
-v "$(pwd)/updated-log4j2.xml:/app/resources/log4j2.xml"
```
