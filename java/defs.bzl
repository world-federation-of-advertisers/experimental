# Copyright 2020 The SWIG Rules Authors
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

def _create_src_jar(ctx, java_runtime_info, input_dir, output_jar):
    jar_args = ctx.actions.args()
    jar_args.add("cf", output_jar)
    jar_args.add_all([input_dir])

    ctx.actions.run(
        outputs = [output_jar],
        inputs = [input_dir],
        executable = "%s/bin/jar" % java_runtime_info.java_home,
        tools = java_runtime_info.files,
        arguments = [jar_args],
        mnemonic = "SwigJar",
    )

def _java_wrap_cc_impl(ctx):
    name = ctx.attr.name

    src = ctx.file.src
    inputs = [src]

    # Add headers from deps as swig inputs.
    for target in ctx.attr.deps:
        inputs.extend(target[CcInfo].compilation_context.headers.to_list())

    outfile = ctx.outputs.outfile
    java_files_dir = ctx.actions.declare_directory(
        "java_files",
        sibling = outfile,
    )

    swig_args = ctx.actions.args()
    swig_args.add("-c++")
    swig_args.add("-java")
    swig_args.add("-package", ctx.attr.package)
    swig_args.add("-outdir", java_files_dir.path)
    swig_args.add("-o", outfile)
    if ctx.attr.module:
        swig_args.add("-module", ctx.attr.module)
    if ctx.label.workspace_root:
        swig_args.add("-I" + ctx.label.workspace_root)
    swig_args.add(src.path)

    ctx.actions.run(
        outputs = [outfile, java_files_dir],
        inputs = inputs,
        executable = "swig",
        arguments = [swig_args],
        mnemonic = "SwigCompile",
    )

    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    _create_src_jar(ctx, java_runtime, java_files_dir, ctx.outputs.srcjar)

_java_wrap_cc = rule(
    doc = """
Wraps C++ in Java using Swig.

It's expected that the `swig` binary exists in the host's path.
""",
    implementation = _java_wrap_cc_impl,
    attrs = {
        "src": attr.label(
            doc = "Single swig source file.",
            allow_single_file = True,
            mandatory = True,
        ),
        "deps": attr.label_list(
            doc = "C++ dependencies.",
            providers = [CcInfo],
        ),
        "package": attr.string(
            doc = "Package for generated Java.",
            mandatory = True,
        ),
        "module": attr.string(doc = "Optional Swig module name."),
        "outfile": attr.output(
            doc = "Generated C++ output file.",
            mandatory = True,
        ),
        "srcjar": attr.output(
            doc = "Generated Java source jar.",
            mandatory = True,
        ),
        "_jdk": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
    },
)

def java_wrap_cc(name, src, package, deps = [], module = "", **kwargs):
    """Wraps C++ in Java using Swig.

    It's expected that the `swig` binary exists in the host's path.

    Args:
        name: target name.
        src: single .swig source file.
        package: package of generated Java files.
        deps: C++ deps.
        module: optional name of Swig module.

    Generated targets:
        {name}: java_library
        lib{name}.so: cc_binary
    """

    wrapper_name = name + "_wrapper"
    outfile = name + ".cc"
    srcjar = name + ".srcjar"
    cc_name = name + "_cc"
    so_name = "lib%s.so" % name

    _java_wrap_cc(
        name = wrapper_name,
        src = src,
        package = package,
        outfile = outfile,
        srcjar = srcjar,
        deps = deps,
        module = module,
        **kwargs
    )

    native.cc_library(
        name = cc_name,
        srcs = [outfile],
        deps = deps + ["@bazel_tools//tools/jdk:jni"],
        alwayslink = True,
    )

    native.cc_binary(
        name = "lib%s.so" % name,
        deps = [cc_name],
        linkshared = True,
    )

    native.java_library(
        name = name,
        srcs = [srcjar],
        runtime_deps = [so_name],
        **kwargs
    )
