load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "com_google_absl",
    strip_prefix = "abseil-cpp-b56cbdd23834a65682c0b46f367f8679e83bc894",
    urls = ["https://github.com/abseil/abseil-cpp/archive/b56cbdd23834a65682c0b46f367f8679e83bc894.zip"],
)

http_archive(
    name = "googletest",
    sha256 = "94c634d499558a76fa649edb13721dce6e98fb1e7018dfaeba3cd7a083945e91",
    strip_prefix = "googletest-release-1.10.0",
    urls = ["https://github.com/google/googletest/archive/release-1.10.0.zip"],
)

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository", "new_git_repository")

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
    sha256 = "4cbce7f708917b6e58b631c24c59fe720acc8fef5f959df9a58cdf9558d0a79b",
    strip_prefix = "grpc-1.28.1",
    urls = [
        "https://github.com/grpc/grpc/archive/v1.28.1.tar.gz",
    ],
)

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", "grpc_deps")

# Includes boringssl, com_google_absl, and other dependencies.
grpc_deps()

load("@com_github_grpc_grpc//bazel:grpc_extra_deps.bzl", "grpc_extra_deps")

# Loads transitive dependencies of GRPC.
grpc_extra_deps()

http_archive(
    name = "com_google_private_join_and_compute",
    sha256 = "dd39c10723f5471d5727c12adb34d2225d1aeb591106fe457f0a88320ee0329c",
    strip_prefix = "private-join-and-compute-master",
    urls = [
        "https://github.com/google/private-join-and-compute/archive/master.zip",
    ],
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
    commit = "4799cbfa2d0e335208d790729ed4b49d34968245",
    remote = "sso://team/ads-xmedia-open-measurement-team/rules_swig",
    shallow_since = "1595012448 -0700",
)

# Measurement APIs.
git_repository(
    name = "wfa_measurement_proto",
    commit = "c55656ca7d8c86f139fa3bb3b0d22a1cf5b74f77",
    remote = "sso://team/ads-xmedia-open-measurement-team/wfa-measurement-proto",
    shallow_since = "1590709328 +0000",
)
