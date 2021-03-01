workspace(name = "halo_containers")

load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
    "http_file",
)

# TODO(@SanjayVas): Switch to an official release once there's one that contains
# the fixes for https://github.com/bazelbuild/rules_docker/issues/1716.
RULES_DOCKER_REVISION = "e15c9ebf203b7fa708e69ff5f1cdcf427d7edf6f"

http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "893726fd83049cece8bfec873091c43877449f28987adf5c13b17801ecf0a788",
    strip_prefix = "rules_docker-{revision}".format(
        revision = RULES_DOCKER_REVISION,
    ),
    urls = [
        "https://github.com/bazelbuild/rules_docker/archive/{revision}.tar.gz".format(
            revision = RULES_DOCKER_REVISION,
        ),
    ],
)

load(
    "@io_bazel_rules_docker//repositories:repositories.bzl",
    container_repositories = "repositories",
)

container_repositories()

load("@io_bazel_rules_docker//repositories:deps.bzl", container_deps = "deps")

container_deps()

load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_pull",
)

# docker.io/library/ubuntu:18.04
container_pull(
    name = "ubuntu_18_04",
    digest = "sha256:d1bf40f712c466317f5e06d38b3e7e4c98fef1229872bf6e2a8d1e01836c7ec4",
    registry = "docker.io",
    repository = "library/ubuntu",
)

# docker.io/library/debian:bullseye-slim
container_pull(
    name = "debian_bullseye",
    digest = "sha256:8ab4e348f60ebd18b891593a531ded31cbbc3878f6e476116f3b49b15c199110",
    registry = "docker.io",
    repository = "library/debian",
)

# Custom Java base image.
# See //base:push_java_base
container_pull(
    name = "debian_java_base",
    digest = "sha256:c6746729103a1a306a1ed572012562496512a691b3b23c3abacd64ad503cebc2",
    registry = "index.docker.io",
    repository = "wfameasurement/java-base",
)

# bazel
http_file(
    name = "bazel",
    downloaded_file_path = "bazel",
    executable = True,
    sha256 = "b7583eec83cc38302997098a40b8c870c37e0ab971a83cb3364c754a178b74ec",
    urls = ["https://github.com/bazelbuild/bazel/releases/download/3.7.0/bazel-3.7.0-linux-x86_64"],
)
