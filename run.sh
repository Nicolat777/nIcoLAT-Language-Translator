#!/bin/sh
set -e
cd "$(dirname "$0")"
mkdir -p build/cli
javac -encoding UTF-8 -d build/cli $(find src/main/java -name '*.java')
java -Dfile.encoding=UTF-8 -cp build/cli nlang.Main "$@"
