load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "absl",
    sha256 = "f342aac71a62861ac784cadb8127d5a42c6c61ab1cd07f00aef05f2cc4988c42",
    strip_prefix = "abseil-cpp-20200225.2",
    urls = ["https://github.com/abseil/abseil-cpp/archive/20200225.2.zip"],
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
    name = "io_grpc_grpc_java",
    sha256 = "ca30194aa4ff175f910bbf212911f1b35c17307833da0afcfba07c525f28fff7",
    strip_prefix = "grpc-java-1.28.0",
    url = "https://github.com/grpc/grpc-java/archive/v1.28.0.tar.gz",
)

http_archive(
    name = "com_google_protobuf",
    strip_prefix = "protobuf-fe1790ca0df67173702f70d5646b82f48f412b99",  # this is 3.11.2
    urls = ["https://github.com/protocolbuffers/protobuf/archive/fe1790ca0df67173702f70d5646b82f48f412b99.zip"],
)

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()

git_repository(
    name = "wfa_measurement_proto",
    commit = "7e8bf95e6a3f89c8e3ca7fb451652989eb986810",
    remote = "sso://team/ads-xmedia-open-measurement-team/wfa-measurement-proto",
)
