#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
exec bb -cp src:test -m qrity.generalized-interop-emit "$@"
