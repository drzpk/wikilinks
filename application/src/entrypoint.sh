#!/bin/bash

if [ ! -d frontend ]; then
  mkdir frontend && cd frontend || exit 1
  unzip ../frontend*.zip
  cd ..
fi

# shellcheck disable=SC2155
export FRONTEND_RESOURCES_DIRECTORY="$(pwd)/frontend"

chmod +x backend.kexe
./backend.kexe http
