%module SketchEncrypterAdapter
%include "exception.i"
%{
#include "src/main/cc/any_sketch/sketch_encrypter_java_adapter.h"
%}

// Convert C++ ::private_join_and_compute::StatusOr<std::string> to a Java byte
// array.
%typemap(jni) ::private_join_and_compute::StatusOr<std::string> "jbyteArray"
%typemap(jtype) ::private_join_and_compute::StatusOr<std::string> "byte[]"
%typemap(jstype) ::private_join_and_compute::StatusOr<std::string> "byte[]"
%typemap(out) ::private_join_and_compute::StatusOr<std::string> {
  if ($1.ok()) {
    $result = JCALL1(NewByteArray, jenv, $1.value().length());
    JCALL4(SetByteArrayRegion, jenv, $result, 0,
           $1.value().length(), (const jbyte*) $1.value().data());
  } else {
    SWIG_exception(SWIG_RuntimeError, $1.status().ToString().c_str());
  }
}
%typemap(javaout) ::private_join_and_compute::StatusOr<std::string> {
  return $jnicall;
}

// Convert Java byte array to C++ const std::string& for any const std::string&
// input parameter.
%typemap(jni) const std::string& "jbyteArray"
%typemap(jtype) const std::string& "byte[]"
%typemap(jstype) const std::string& "byte[]"
%typemap(javain) const std::string& "$javainput"
%typemap(in) const std::string& {
  jsize temp_length = JCALL1(GetArrayLength, jenv, $input);
  jbyte* temp_bytes = JCALL2(GetByteArrayElements, jenv, $input, 0);
  $1 = new std::string((const char*) temp_bytes, temp_length);
  JCALL3(ReleaseByteArrayElements, jenv, $input, temp_bytes, JNI_ABORT);
}
// Convert C++ std::string to a Java byte array.
%clear std::string;
%typemap(jni) std::string "jbyteArray"
%typemap(jtype) std::string "byte[]"
%typemap(jstype) std::string "byte[]"
%typemap(out) std::string {
  $result = JCALL1(NewByteArray, jenv, $1.length());
  JCALL4(SetByteArrayRegion, jenv, $result, 0,
         $1.length(), (const jbyte*) $1.data());
}
%typemap(javaout) std::string {
  return $jnicall;
}

// Convert C++ ::private_join_and_compute::StatusOr<std::unique_ptr<
// ::private_join_and_compute::ECCommutativeCipher>> to a Java object.
%typemap(jni) ::private_join_and_compute::StatusOr<std::unique_ptr<::wfa::any_sketch::SketchEncrypterJavaAdapter>> "jlong"
%typemap(jtype) ::private_join_and_compute::StatusOr<std::unique_ptr<::wfa::any_sketch::SketchEncrypterJavaAdapter>> "long"
%typemap(jstype) ::private_join_and_compute::StatusOr<std::unique_ptr<::wfa::any_sketch::SketchEncrypterJavaAdapter>> "SketchEncrypterJavaAdapter"
%typemap(out, optimal="1") ::private_join_and_compute::StatusOr<std::unique_ptr<::wfa::any_sketch::SketchEncrypterJavaAdapter>> {
  // Use optimal="1" to control how the function is invoked. Without it, SWIG
  // creates a temporary StatusOr and then tries to copy the function's result
  // into it. This fails because std::unique_ptr can't be copied. See
  // http://www.swig.org/Doc3.0/Typemaps.html#Typemaps_optimal  for details on
  // the "optimal" flag.
  auto status_or = $1;
  if (status_or.ok()) {
    *(::wfa::any_sketch::SketchEncrypterJavaAdapter**)&$result = status_or.value().release();
  } else {
    SWIG_exception(SWIG_RuntimeError, status_or.status().ToString().c_str());
  }
}
%typemap(javaout) ::private_join_and_compute::StatusOr<std::unique_ptr<::wfa::any_sketch::SketchEncrypterJavaAdapter>> {
  long cPtr = $jnicall;
  return (cPtr == 0)
      ? null
      : new SketchEncrypterJavaAdapter(cPtr, true /* takes ownership */);
}


%define %ignoreall %ignore ""; %enddef
%define %unignore %rename("%s") %enddef
%define %unignoreall %rename("%s") ""; %enddef

%ignoreall
%unignore wfa;
%unignore wfa::any_sketch;
%unignore wfa::any_sketch::SketchEncrypterJavaAdapter;
%unignore wfa::any_sketch::SketchEncrypterJavaAdapter::~SketchEncrypterJavaAdapterWrapper();
%unignore wfa::any_sketch::SketchEncrypterJavaAdapter::CreateFromPublicKeys(int, int, const std::string&, const std::string&);
%unignore wfa::any_sketch::SketchEncrypterJavaAdapter::Encrypt(const std::string&) const;
%include "src/main/cc/any_sketch/sketch_encrypter_java_adapter.h"
%unignoreall