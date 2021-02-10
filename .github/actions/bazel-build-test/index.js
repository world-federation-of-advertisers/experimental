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

const core = require('@actions/core');
const cache = require('@actions/cache');
const exec = require('@actions/exec');

const workspacePath = core.getInput('workspace-path');
const validTestExitCodes = [
  0,  // Passes.
  4,  // No test targets.
];

async function execBash(commands) {
  const command = commands.join('\n');
  const lines = [];
  await exec.exec('bash', ['-c', command], {
    cwd: workspacePath,
    listeners: {
      stdline: (line) => {
        lines.push(line);
      }
    }
  });
  return lines.join('\n');
}

async function run() {
  const execRoot = await execBash(['bazelisk info execution_root']);
  const execRootHash =
      await execBash([`echo -n '${execRoot}' | git hash-object --stdin`]);
  const treeHash = await execBash(['git rev-parse HEAD:']);
  const outputBase = await execBash(['bazelisk info output_base'])

  const cacheKey = `bazel-${execRootHash}-${treeHash}`;
  const restoreKeys = [`bazel-${execRootHash}-`];
  const cachePaths = [outputBase];
  await cache.restoreCache(cachePaths, cacheKey, restoreKeys);

  await exec.exec(
      'bazelisk', ['build', '--keep_going', '//...'], {cwd: workspacePath});
  const testExitCode = await exec.exec(
      'bazelisk', ['test', '--keep_going', '//...'],
      {cwd: workspacePath, ignoreReturnCode: true});
  if (!validTestExitCodes.includes(testExitCode)) {
    throw new Error('Testing failed');
  }
  await cache.saveCache(cachePaths, cacheKey);
}

async function main() {
  try {
    await run();
  } catch (err) {
    core.setFailed(err);
  }
}

main();