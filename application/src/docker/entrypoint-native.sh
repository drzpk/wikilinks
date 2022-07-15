#!/bin/bash

# shellcheck disable=SC2155
export FRONTEND_RESOURCES_DIRECTORY="$(pwd)/frontend"

chmod +x backend.kexe
./backend.kexe http
