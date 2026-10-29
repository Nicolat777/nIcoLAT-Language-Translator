#!/bin/sh
# 不依赖 Maven 的最快跑法：编译到 build/cli 然后执行。
# 用法：
#   ./run.sh selftest
#   ./run.sh run examples/demo.n
#   ./run.sh canon examples/a3_time.n
set -e
cd "$(dirname "$0")"
mkdir -p build/cli
javac -encoding UTF-8 -d build/cli $(find src -name '*.java')
java -Dfile.encoding=UTF-8 -cp build/cli nlang.Main "$@"
