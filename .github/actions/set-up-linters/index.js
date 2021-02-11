/**
 * Copyright 2021 The Cross-Media Measurement Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const fsPromises = require('fs').promises;
const path = require('path');

const core = require('@actions/core');
const exec = require('@actions/exec');
const io = require('@actions/io');
const tc = require('@actions/tool-cache');

const LINTERS_PATH = path.join(process.env.HOME, 'lint');
const EXECUTABLE_MODE = 0o755;

function buildTool(name, version) {
  return {
    name: name, version: version, path: path.join(LINTERS_PATH, name)
  }
}

const tools = Object.freeze({
  addlicense: buildTool('addlicense', '0.0.0-20200906110928-a0294312aa76'),
  buildifier: buildTool('buildifier', '4.0.0'),
  java_format: buildTool('google-java-format', '1.9'),
  ktlint: buildTool('ktlint', '0.40.0'),
});

async function installExecutable(tool, url) {
  const cachedPath = tc.find(tool.name, tool.version);
  if (cachedPath) {
    await io.cp(path.join(cachedPath, tool.name), tool.path);
    core.info(`Restored ${tool.name} ${tool.version} from cache`);
    return;
  }

  await tc.downloadTool(url, tool.path);
  await fsPromises.chmod(tool.path, EXECUTABLE_MODE);
  await tc.cacheFile(tool.path, tool.name, tool.name, tool.version);
}

async function installBuildifier() {
  const tool = tools.buildifier;
  await installExecutable(
      tool,
      `https://github.com/bazelbuild/buildtools/releases/download/${
          tool.version}/buildifier-linux-amd64`)
}

async function installKtlint() {
  const tool = tools.ktlint;
  await installExecutable(
      tool,
      `https://github.com/pinterest/ktlint/releases/download/${
          tool.version}/ktlint`)
}

async function installGoogleJavaFormat() {
  const tool = tools.java_format;
  const jarName = `${tool.name}.jar`;
  const jarPath = path.join(LINTERS_PATH, jarName);
  const cachedPath = tc.find(tool.name, tool.version);
  if (cachedPath) {
    await io.cp(path.join(cachedPath, jarName), jarPath);
    core.info(`Restored ${tool.name} ${tool.version} from cache`);
  } else {
    await tc.downloadTool(
        `https://github.com/google/google-java-format/releases/download/google-java-format-${
            tool.version}/google-java-format-${tool.version}-all-deps.jar`,
        jarPath);
    await tc.cacheFile(jarPath, jarName, tool.name, tool.version);
  }

  const script = `#!/usr/bin/env bash
  java -jar ${jarPath} "$@"`;
  await fsPromises.writeFile(tool.path, script, {mode: EXECUTABLE_MODE});
}

async function installAddlicense() {
  const tool = tools.addlicense;
  const cachedPath = tc.find(tool.name, tool.version);
  if (cachedPath) {
    await io.cp(path.join(cachedPath, tool.name), tool.path);
    core.info(`Restored ${tool.name} ${tool.version} from cache`);
    return;
  }

  const tmpdir =
      await fsPromises.mkdtemp(path.join(process.env.RUNNER_TEMP, tool.name));

  const goPackage = 'github.com/google/addlicense';
  const execOptions = {cwd: tmpdir};
  await exec.exec('go', ['mod', 'init', 'temp'], execOptions);
  await exec.exec(
      'go', ['get', '-u', '-d', `${goPackage}@v${tool.version}`], execOptions);
  await exec.exec('go', ['build', '-ldflags=-s', goPackage], execOptions);
  await io.cp(path.join(tmpdir, tool.name), tool.path);
  await tc.cacheFile(tool.path, tool.name, tool.name, tool.version);
}

async function run() {
  try {
    await fsPromises.mkdir(LINTERS_PATH, {recursive: true});
    await Promise.all([
      installBuildifier(), installKtlint(), installGoogleJavaFormat(),
      installAddlicense()
    ]);

    core.addPath(LINTERS_PATH);
  } catch (err) {
    core.setFailed(err);
  }
}

run();