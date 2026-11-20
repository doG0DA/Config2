#!/bin/sh
# Build and run: ./run.sh [file.ini]   (or: sh run.sh [file.ini])
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -encoding UTF-8 -d out src/*.java || exit 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp out Main "$@"
