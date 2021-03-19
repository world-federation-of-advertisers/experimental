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

"""Halo repository dependencies."""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

_URL_TEMPLATE = "https://github.com/world-federation-of-advertisers/experimental/archive/{commit}.tar.gz"
_PREFIX_TEMPLATE = "experimental-{commit}/{subtree}"

def halo_dependencies(target_map, commit, sha256):
    """Adds Halo repository targets.

    Args:
        target_map: Dict mapping subtree to target name.
        commit: The Git commit hash.
        sha256: The SHA256 sub of the GitHub source archive.
    """
    urls = [_URL_TEMPLATE.format(commit = commit)]
    prefix_template = _PREFIX_TEMPLATE.format(
        commit = commit,
        subtree = "{subtree}",
    )

    for (subtree, name) in target_map.items():
        http_archive(
            name = name,
            urls = urls,
            sha256 = sha256,
            strip_prefix = prefix_template.format(subtree = subtree),
        )
