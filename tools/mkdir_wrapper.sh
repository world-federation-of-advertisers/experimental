#!/usr/bin/env bash

# Wrapper which creates the directory specified as $1, then executing the
# command in $2 with the remaining arguments.
#
# This is to work around https://github.com/bazelbuild/bazel/issues/6393

readonly directory="$1"
readonly command="$2"
shift 2

mkdir -p "${directory}"
exec "${command}" "$@"
