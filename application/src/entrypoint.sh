#!/bin/bash

mkdir frontend && cd frontend || exit 1
unzip ../frontend*.zip
# shellcheck disable=SC2155
export FRONTEND_RESOURCES_DIRECTORY=$(pwd)
cd ..


chmod +x backend.kexe
./backend.kexe http
