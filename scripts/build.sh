#!/bin/bash

# Stop execution on error
set -e
# Stop piping when any command in the pipe has error
set -o pipefail

SCRIPT_DIRECTORY="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIRECTORY="$SCRIPT_DIRECTORY/.."

# Go to project directory
cd "$PROJECT_DIRECTORY"

# Download JRE to use rt.jar
JRE8_URL="https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u332-b09/OpenJDK8U-jre_x64_linux_hotspot_8u332b09.tar.gz"
JRE8_FILE="$PROJECT_DIRECTORY/.gradle/java/jre8.tar.gz"
JRE8_FILE_SHA512="c62002aa1a28547b0b0cefea3f83af2cf6507002bf296b1b83bd04406f4c362f4c74940209ebbd3803513395bec7503882aac7f8d35dcbf6a6e02e9155a53cdd  $JRE8_FILE"

if ! echo "$JRE8_FILE_SHA512" | shasum -c; then
  mkdir -p "$PROJECT_DIRECTORY/.gradle/java"
  echo "Downloading JRE8 for ProGuard: $JRE8_URL"
  curl -s -L "$JRE8_URL" -o "$JRE8_FILE"
  tar -C "$PROJECT_DIRECTORY/.gradle/java/" -xvzf "$JRE8_FILE"
fi

"$PROJECT_DIRECTORY"/gradlew buildPlugin proGuard

