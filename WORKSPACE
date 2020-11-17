workspace(name = "any_sketch_java")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository", "new_git_repository")

# Abseil C++ libraries
git_repository(
    name = "com_google_absl",
    remote = "https://github.com/abseil/abseil-cpp.git",
    commit = "0f3bb466b868b523cf1dc9b2aaaed65c77b28862",
    shallow_since = "1603283562 -0400",
)

http_archive(
    name = "googletest",
    sha256 = "94c634d499558a76fa649edb13721dce6e98fb1e7018dfaeba3cd7a083945e91",
    strip_prefix = "googletest-release-1.10.0",
    urls = ["https://github.com/google/googletest/archive/release-1.10.0.zip"],
)


new_git_repository(
    name = "farmhash",
    build_file_content = """
package(default_visibility = ["//visibility:public"])
cc_library(
    name = "farmhash",
    hdrs = ["src/farmhash.h"],
    srcs = ["src/farmhash.cc"],
    deps = [],
)""",
    commit = "2f0e005b81e296fa6963e395626137cf729b710c",
    remote = "https://github.com/google/farmhash.git",
)

http_archive(
    name = "com_github_glog_glog",
    sha256 = "f28359aeba12f30d73d9e4711ef356dc842886968112162bc73002645139c39c",
    strip_prefix = "glog-0.4.0",
    urls = ["https://github.com/google/glog/archive/v0.4.0.tar.gz"],
)

# gflags
# Needed for glog
http_archive(
    name = "com_github_gflags_gflags",
    sha256 = "34af2f15cf7367513b352bdcd2493ab14ce43692d2dcd9dfc499492966c64dcf",
    strip_prefix = "gflags-2.2.2",
    urls = [
        "https://mirror.bazel.build/github.com/gflags/gflags/archive/v2.2.2.tar.gz",
        "https://github.com/gflags/gflags/archive/v2.2.2.tar.gz",
    ],
)

# gRPC
http_archive(
    name = "com_github_grpc_grpc",
    sha256 = "2060769f2d4b0d3535ba594b2ab614d7f68a492f786ab94b4318788d45e3278a",
    strip_prefix = "grpc-1.33.2",
    urls = [
        "https://github.com/grpc/grpc/archive/v1.33.2.tar.gz",
    ],
)

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", "grpc_deps")

# Includes boringssl, com_google_absl, and other dependencies.
grpc_deps()

load("@com_github_grpc_grpc//bazel:grpc_extra_deps.bzl", "grpc_extra_deps")

# Loads transitive dependencies of GRPC.
grpc_extra_deps()

git_repository(
    name = "com_google_private_join_and_compute",
    remote = "https://github.com/google/private-join-and-compute.git",
    commit = "aa2d68e68193547e88a120c667e173662abd7478",
    shallow_since = "1605559721 -0400",
)

http_archive(
    name = "com_google_protobuf",
    strip_prefix = "protobuf-fe1790ca0df67173702f70d5646b82f48f412b99",  # this is 3.11.2
    urls = ["https://github.com/protocolbuffers/protobuf/archive/fe1790ca0df67173702f70d5646b82f48f412b99.zip"],
)

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()

# Support Maven sources

http_archive(
    name = "rules_jvm_external",
    sha256 = "62133c125bf4109dfd9d2af64830208356ce4ef8b165a6ef15bbff7460b35c3a",
    strip_prefix = "rules_jvm_external-3.0",
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/3.0.zip",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

# Maven
maven_install(
    artifacts = [
        "com.google.guava:guava:29.0-jre",
        "com.google.truth.extensions:truth-liteproto-extension:1.0.1",
        "com.google.truth.extensions:truth-proto-extension:1.0.1",
        "com.google.truth:truth:1.0.1",
        "junit:junit:4.13",
    ],
    generate_compat_repositories = True,
    repositories = [
        "https://repo.maven.apache.org/maven2/",
    ],
)

load("@maven//:compat.bzl", "compat_repositories")

compat_repositories()

# Swig rules.
git_repository(
    name = "wfa_rules_swig",
    commit = "ad4f1bcf5747cb26b480b45c582069fb1488f8c1",
    remote = "sso://team/ads-xmedia-open-measurement-team/rules_swig",
    shallow_since = "1603231045 -0700"
)

# Measurement APIs.
git_repository(
    name = "wfa_measurement_proto",
    commit = "c55656ca7d8c86f139fa3bb3b0d22a1cf5b74f77",
    remote = "sso://team/ads-xmedia-open-measurement-team/wfa-measurement-proto",
    shallow_since = "1590709328 +0000",
)

# Core AnySketch.
git_repository(
    name = "any_sketch",
    commit = "5415eec38253c3bd3f250cb12fdc24242743e426",
    remote = "sso://team/ads-xmedia-open-measurement-team/any-sketch",
    shallow_since = "1603139261 +0000",
)
