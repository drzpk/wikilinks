#!/bin/bash

# shellcheck disable=SC2155
export FRONTEND_RESOURCES_DIRECTORY="$(pwd)/frontend"

java -cp @/app/jib-classpath-file dev.drzepka.wikilinks.app.JvmMainKt http