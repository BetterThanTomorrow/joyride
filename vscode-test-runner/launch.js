const path = require("path");
const process = require("process");
const os = require("os");
const fs = require("fs");
const { runTests } = require('@vscode/test-electron');

function init() {
  return new Promise((resolve, reject) => {
    try {
      const USER_CONFIG_PATH_KEY = "VSCODE_JOYRIDE_USER_CONFIG_PATH";
      if (!process.env[USER_CONFIG_PATH_KEY]) {
        const tmpConfigPath = path.join(
          os.tmpdir(),
          "vscode-test-runner-joyride",
          "user-config"
        );
        if (fs.existsSync(tmpConfigPath)) {
          fs.rmSync(tmpConfigPath, { recursive: true });
        }
        fs.mkdirSync(tmpConfigPath, { recursive: true });
        process.env[USER_CONFIG_PATH_KEY] = tmpConfigPath;
        console.info(`USER_CONFIG_PATH: ${process.env[USER_CONFIG_PATH_KEY]}`);
      }
      resolve();
    } catch (error) {
      reject(error);
    }
  });
}

async function main() {
  try {
    // The folder containing the Extension Manifest package.json
    // Passed to `--extensionDevelopmentPath`
    const extensionDevelopmentPath = path.resolve(__dirname, '..');

    // The path to the extension test runner script
    // Passed to --extensionTestsPath
    const extensionTestsPath = path.resolve(__dirname, 'runTests');
    const testWorkspace = path.resolve(__dirname, 'workspace-1');

    const launchArgs = [testWorkspace, '--disable-extensions', '--disable-workspace-trust'];

    // Download VS Code, unzip it and run the integration test
    await runTests({
      version: 'insiders',
      extensionDevelopmentPath,
      extensionTestsPath,
      launchArgs,
    }).then((_result) => {
      console.info('Tests finished');
    }).catch((err) => {
      console.error('Tests finished:', err);
      process.exit(1);
    });  
  } catch (err) {
    console.error('Failed to run tests:', err);
    process.exit(1);
  }
}

void init().then(() => main())
  .catch((error) => {
    console.error('Failed to initialize test running environment:', error);
    process.exit(1);
  });
