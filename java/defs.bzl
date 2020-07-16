def _java_wrap_cc_impl(ctx):
    name = ctx.attr.name
    module = ctx.attr.module

    src = ctx.file.src
    inputs = [src] + ctx.files.swig_includes

    outfile = ctx.outputs.outfile
    outputs = [outfile] + ctx.outputs.java_outputs

    swig_args = [
        "-c++",
        "-java",
        "-package",
        ctx.attr.package,
        "-module",
        module,
        "-outdir",
        outfile.dirname,
        "-o",
        outfile.path,
    ]
    if len(ctx.label.workspace_root) > 0:
        swig_args.append("-I" + ctx.label.workspace_root)
    swig_args.append(src.path)

    ctx.actions.run(
        outputs = outputs,
        inputs = inputs,
        executable = "swig",
        arguments = swig_args,
        mnemonic = "SwigCompile",
    )

_java_wrap_cc = rule(
    implementation = _java_wrap_cc_impl,
    attrs = {
        "src": attr.label(allow_single_file = True, mandatory = True),
        "swig_includes": attr.label_list(allow_files = True),
        "module": attr.string(mandatory = True),
        "package": attr.string(doc = "Java package.", mandatory = True),
        "outfile": attr.output(mandatory = True),
        "java_outputs": attr.output_list(mandatory = True),
    },
)

def java_wrap_cc(name, src, module, package, swig_includes = [], classes = []):
    """
    Wraps C++ in Java using Swig.

    It's expected that the `swig` binary exists in the host's path.

    Args:
        name: target name.
        src: single .swig source file.
        module: name of Swig module.
        package: package of generated Java files.
        swig_includes: optional list of files included by Swig source.
        classes: list of class names.

    Outputs:
        {name}.cc
        {module}.java
        {module}JNI.java
        {className}.java for each class
    """
    java_outputs = ([module + ".java", module + "JNI.java"] +
                    [clazz + ".java" for clazz in classes])
    _java_wrap_cc(
        name = name,
        src = src,
        module = module,
        package = package,
        swig_includes = swig_includes,
        outfile = name + ".cc",
        java_outputs = java_outputs,
    )
