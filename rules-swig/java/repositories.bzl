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

"""Repository dependencies for SWIG Java."""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def rules_swig_dependencies():
    if not native.existing_rule("bazel_skylib"):
        _bazel_skylib()

    if not native.existing_rule("rules_cc"):
        _rules_cc()

    if not native.existing_rule("rules_java"):
        _rules_java()

def _bazel_skylib():
    version = "1.0.3"
    sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c"

    http_archive(
        name = "bazel_skylib",
        urls = [
            "https://github.com/bazelbuild/bazel-skylib/releases/download/{version}/bazel-skylib-{version}.tar.gz".format(version = version),
            "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/{version}/bazel-skylib-{version}.tar.gz".format(version = version),
        ],
        sha256 = sha256,
    )

def _rules_cc():
    revision = "40548a2974f1aea06215272d9c2b47a14a24e556"
    sha256 = "56ac9633c13d74cb71e0546f103ce1c58810e4a76aa8325da593ca4277908d72"

    http_archive(
        name = "rules_cc",
        urls = ["https://github.com/bazelbuild/rules_cc/archive/{revision}.zip".format(revision = revision)],
        sha256 = sha256,
        strip_prefix = "rules_cc-" + revision,
    )

def _rules_java():
    version = "4.0.0"
    sha256 = "34b41ec683e67253043ab1a3d1e8b7c61e4e8edefbcad485381328c934d072fe"

    http_archive(
        name = "rules_java",
        url = "https://github.com/bazelbuild/rules_java/releases/download/4.0.0/rules_java-4.0.0.tar.gz",
        sha256 = "34b41ec683e67253043ab1a3d1e8b7c61e4e8edefbcad485381328c934d072fe",
    )
