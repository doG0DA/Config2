#!/bin/sh
# Prints the D2 text of the graph for every d2 example (stage 5, test repository)
cd "$(dirname "$0")/.." || exit 1
for f in examples/d2_*.ini; do
    echo "> $f"
    sh ./run.sh "$f"
    echo
done
