#!/bin/sh
# Runs the app on every example config with errors
cd "$(dirname "$0")/.." || exit 1
for f in examples/errors/*.ini nope.ini; do
    echo "> $f"
    ./run.sh "$f"
    echo
done
