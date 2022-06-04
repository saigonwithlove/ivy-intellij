#!/bin/bash

# Stop execution on error
set -e
# Stop piping when any command in the pipe has error
set -o pipefail

SCRIPT_DIRECTORY="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIRECTORY="$SCRIPT_DIRECTORY/.."

# Setup Proguard binary, our build depends on it.
bash "$SCRIPT_DIRECTORY/setup-jre8.sh"

# Go to project directory and build
cd "$PROJECT_DIRECTORY"
gradle buildPlugin proGuard

