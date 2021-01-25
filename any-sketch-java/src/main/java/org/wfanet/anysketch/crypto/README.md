# SketchEncrypter Java Library

The SketchEncrypterJavaAdapter Java class is auto-generated from the
sketch_encrypter.swig definition. This wraps a C++ library from the core
AnySketch repository for use in JNI.

## Possible errors when using the JNI java library.

### swig uninstalled

To keep the library updated, each time when the java library is built, it would
run a swig command (provided in the BUILD.bazel rule) to re-generate all the
swig wrapper files using the latest c++ codes. As a result, the swig software is
required to build the java library. Install swig before building the package.

For example, in a system using apt-get, run the following command to get swig:

```shell
sudo apt-get install swig
```
