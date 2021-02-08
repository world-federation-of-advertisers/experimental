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

# Outputs the files changed since the specified revision, one per line.
#
# Arguments:
#   Base revision
#   Revision
get_changed_files() {
  local -r base_revision="$1"
  local -r revision="${2:-HEAD}"

  local files_quoted
  files_quoted="$(
    git -c 'core.quotePath=true' diff \
      --relative \
      --name-only \
      --diff-filter=AMRC \
      "${base_revision}" "${revision}" --
  )" || return

  # Interpret quotes and escape sequences.
  xargs -n 1 <<<"${files_quoted}"
}

# Outputs the file paths from stdin whose basename matches the specified regex
# pattern.
filter() {
  local -r pattern="$1"

  local item
  local base
  while read -r item; do
    base="$(basename "${item}")"
    # shellcheck disable=SC2053
    if [[ "${base}" =~ $pattern ]]; then
      echo "${item}"
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

  local files
  files="$(echo "${changed_files}" | filter "${pattern}")"
  [[ -z "${files}" ]] && return

  mapfile -t files <<<"${files}"
  $command "${files[@]}"
}

# Run linters on changed files.
#
# Arguments:
#   Base revision
#   Revision
main() {
  local changed_files
  changed_files="$(get_changed_files "$@")"

  if [[ -z "${changed_files}" ]]; then
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
