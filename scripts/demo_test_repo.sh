#!/bin/sh
# Runs the app on every test-repository example (stage 3)
cd "$(dirname "$0")/.." || exit 1
for f in examples/test_*.ini; do
    echo "> $f"
    sh ./run.sh "$f"
    echo
done
