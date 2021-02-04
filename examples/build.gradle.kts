// Copyright 2021 The Cross-Media Measurement Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import com.google.protobuf.gradle.*

val protocRuntime by configurations.creating
val runtimeDir by extra("$buildDir/resources/protoc")

group = "org.wfanet"
version = "1.0-SNAPSHOT"

repositories {
  mavenLocal()
  google()
  jcenter()
  mavenCentral()
  ivy {
    url = uri("https://packages.grpc.io/")
    patternLayout {
      artifact("/archive/2021/02/[organization]/[module]/grpc-protoc_linux_x64-[revision]-dev(.[ext])")
    }
    metadataSources {
      artifact()
    }
  }
}

buildscript {
  repositories {
    google()
    jcenter()
    mavenCentral()
    mavenLocal()
  }
  dependencies {
    classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.12")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
  }
}

plugins {
  kotlin("jvm") version "1.4.21"
  id("com.google.protobuf") version "0.8.12"
  idea
  application
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
  implementation("io.grpc:grpc-kotlin-stub:0.1.1")
  implementation("com.google.protobuf:protobuf-java:3.11.1")
  implementation("com.google.protobuf:protobuf-java-util:3.11.1")
  implementation("io.grpc:grpc-netty-shaded:1.28.1")
  implementation("io.grpc:grpc-protobuf:1.28.1")
  implementation("io.grpc:grpc-stub:1.28.1")
  compileOnly("javax.annotation:javax.annotation-api:1.2")
  api("junit:junit:4.13")
  implementation("junit:junit:4.13")
  testImplementation("junit:junit:4.13")
  testImplementation("com.google.truth:truth:1.1.2")
  testImplementation("io.grpc:grpc-testing:1.28.1")
  protocRuntime("4dc84aea46396cde21d13813efcf8ca3b2fda692-0cfa9cb0-c8c8-45f0-b055-48770e1c386c:protoc:1.36.0@tar.gz")
}

protobuf {
  protoc { artifact = "com.google.protobuf:protoc:3.6.1" }
  plugins {
    id("cpp")
    id("grpc_cpp") { path = "$runtimeDir/grpc_cpp_plugin" }
    id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.15.1" }
    id("grpckt") { artifact = "io.grpc:protoc-gen-grpc-kotlin:0.1.1" }
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.plugins {
        id("cpp")
        id("grpc_cpp")
        id("grpc")
        id("grpckt")
      }
      it.dependsOn("unpackFiles")
    }
  }
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "11"
  }
  compileTestKotlin {
    kotlinOptions.jvmTarget = "11"
  }
}

task("run-hello-streaming-server-kt", JavaExec::class) {
  main = "org.wfanet.examples.streaming.HelloStreamingServerKt"
  classpath = sourceSets["main"].runtimeClasspath
}

task("run-hello-streaming-client-kt", JavaExec::class) {
  main = "org.wfanet.examples.streaming.HelloStreamingClientKt"
  classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<Copy>("unpackFiles") {
  protocRuntime.asFileTree.forEach {
    from(tarTree(resources.gzip(it)))
    into(runtimeDir)
  }
}
