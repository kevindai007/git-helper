#!/bin/sh
# put on root of the project folder

set -e

SNIPPET_ID=1
BASE_URL="https://gitlab-ultimate.nationalcloud.ae/-/snippets/4/raw/main"

mkdir -p maven-build

curl -fsSL "${BASE_URL}/mvn_pom_build.py"    -o ./maven-build/mvn_pom_build.py
#curl -fsSL "${BASE_URL}/master.json"      -o ./maven-build/master.json
#curl -fsSL "${BASE_URL}/develop.json"    -o ./maven-build/develop.json
curl -fsSL "${BASE_URL}/default.json"        -o ./maven-build/default.json

python3 ./maven-build/mvn_pom_build.py
