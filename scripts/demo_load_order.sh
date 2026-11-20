#!/bin/sh
# Runs the app on every load-order example (stage 4, test repository)
cd "$(dirname "$0")/.." || exit 1
for f in examples/order_*.ini; do
    echo "> $f"
    sh ./run.sh "$f"
    echo
done
