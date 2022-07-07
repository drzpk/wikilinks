#!/bin/bash

if [ -z "$SCRIPT_LOCATION" ]; then
  >&2 echo "SCRIPT_LOCATION environment variable wasn't found"
  exit 1
fi

aws s3 cp "$SCRIPT_LOCATION" .
filename=$(basename "$SCRIPT_LOCATION")
chmod +x "$filename"
./"$filename"
