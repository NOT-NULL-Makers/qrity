#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
exec clojure -M:interop-jvm "$@"
