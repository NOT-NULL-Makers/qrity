#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
exec clojure -M:generalized-interop-jvm "$@"
