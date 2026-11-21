#!/bin/sh
# Draws and opens a picture for every image example (stage 5, needs d2 in PATH)
cd "$(dirname "$0")/.." || exit 1
for f in examples/image_*.ini; do
    echo "> $f"
    sh ./run.sh "$f"
    echo
done
