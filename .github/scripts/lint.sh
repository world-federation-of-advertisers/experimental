#!/usr/bin/env bash
# Copyright 2021 The Cross-Media Measurement Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -eEu -o pipefail

readonly LICENSE_TYPE='apache'
readonly COPYRIGHT_HOLDER='The Cross-Media Measurement Authors'

readonly ALL_FILES_PATTERN='.*'
readonly KOTLIN_PATTERN='\.kts?$'
readonly BAZEL_PATTERN='BUILD(\.bazel)?$|\.bzl$'
readonly CPP_OR_PROTO_PATTERN='\.(cc|h|proto)$'
readonly CPP_PATTERN='\.(cc|h)$'
readonly JAVA_PATTERN='\.java$'

# Reads exclude pathspecs from the .lintignore file into the specified array.
read_exclude_pathspecs() {
  local -n pathspecs="$1"

  if ! [[ -f '.lintignore' ]]; then
    return
  fi

  local line
  while read -r line; do
    if [[ -z "${line}" ]] || [[ "${line:0:1}" == '#' ]]; then
      continue
    fi
    pathspecs+=( ":!${line}" )
  done < '.lintignore'
}

# Outputs a null-terminated list of files that have changed relative to the
# specified base revision.
#
# Arguments:
#   Base revision
#   Revision
get_changed_files() {
  local -r base_revision="$1"
  local -r revision="${2:-HEAD}"

  local -a exclude_pathspecs=()
  read_exclude_pathspecs exclude_pathspecs

  git diff \
    --relative \
    --name-only \
    -z \
    --diff-filter=AMRC \
    "${base_revision}" "${revision}" -- \
    "${exclude_pathspecs[@]}"
}

# Adds the items whose basename matches the specified regex pattern to an array.
#
# Arguments:
#   Array to add filtered items to
#   pattern
#   items
filter() {
  local -n filtered="$1"
  local -r pattern="$2"
  shift 2

  local item
  local base
  for item in "$@"; do
    base="$(basename "${item}")"
    # shellcheck disable=SC2053
    if [[ "${base}" =~ $pattern ]]; then
      filtered+=( "${item}" )
    fi
  done
}

addlicense_cmd() {
  echo 'Checking for missing license headers...' >&2
  addlicense --check -l "${LICENSE_TYPE}" -c "${COPYRIGHT_HOLDER}" "$@" || {
    echo 'The above files are missing license headers' >&2
    return 1
  }
  echo 'Done' >&2
}

ktlint_cmd() {
  ktlint --experimental --relative "$@" 1>&2
}

buildifier_cmd() {
  buildifier --mode=check --lint=warn "$@"
}

clang_format_cmd() {
  clang-format --Werror --style=Google --dry-run "$@"
}

cpplint_cmd() {
  cpplint "$@"
}

google_java_format_cmd() {
  google-java-format --dry-run "$@"
}

# Runs the linter command on changed files with the specified regex pattern.
#
# Scope vars:
#   changed_files
run_linter() {
  local -r command="$1"
  local -r pattern="$2"

  local -a filtered_files=()
  filter filtered_files "${pattern}" "${changed_files[@]}"

  if ! (( ${#filtered_files[@]} )); then
    return
  fi
  $command "${filtered_files[@]}"
}

# Run linters on changed files.
#
# Arguments:
#   Base revision
#   Revision
main() {
  local -a changed_files=()
  readarray -d '' -t changed_files < <(get_changed_files "$@")
  local -r changed_files_pid="$!"
  wait "${changed_files_pid}"

  if ! (( ${#changed_files[@]} )); then
    echo 'No files to lint' >&2
    return
  fi

  local -i has_errors=0

  run_linter addlicense_cmd "${ALL_FILES_PATTERN}" || has_errors=1
  run_linter ktlint_cmd "${KOTLIN_PATTERN}" || has_errors=1
  run_linter buildifier_cmd "${BAZEL_PATTERN}" || has_errors=1
  run_linter clang_format_cmd "${CPP_OR_PROTO_PATTERN}" || has_errors=1
  run_linter cpplint_cmd "${CPP_PATTERN}" || has_errors=1
  run_linter google_java_format_cmd "${JAVA_PATTERN}" || has_errors=1

  ! ((has_errors))
}

main "$@"
