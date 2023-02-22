const cp = require("child_process");
const path = require("path");
const process = require("process");
const os = require("os");
const fs = require("fs");
const {
  downloadAndUnzipVSCode,
  resolveCliArgsFromVSCodeExecutablePath,
  runTests,
} = require("@vscode/test-electron");

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

async function main(joyrideVSIXPathOrLabel, testWorkspace) {
  try {
    const extensionTestsPath = path.resolve(__dirname, "runTests");
    const vscodeExecutablePath = await downloadAndUnzipVSCode("insiders");
    const [cliPath, ...args] =
      resolveCliArgsFromVSCodeExecutablePath(vscodeExecutablePath);

    const launchArgs = [
      testWorkspace,
      ...args,
      "--disable-workspace-trust",
      ...(joyrideVSIXPathOrLabel !== "extension-development"
        ? ["--install-extension", joyrideVSIXPathOrLabel, "--force"]
        : [
            // Make this instance exit so that runTests() can launch a new instance
            // https://github.com/microsoft/vscode-test/issues/192
            "--version",
          ]),
      "--verbose",
    ];
    console.log("launchArgs", launchArgs);
    cp.spawnSync(cliPath, launchArgs, {
      encoding: "utf-8",
      stdio: "inherit",
    });

    const runOptions = {
      vscodeExecutablePath,
      extensionTestsPath,
      launchArgs: [testWorkspace],
    };
    if (joyrideVSIXPathOrLabel === "extension-development") {
      runOptions.extensionDevelopmentPath = path.resolve(__dirname, "..");
    }
    await runTests(runOptions)
      .then((_result) => {
        console.info("Tests finished");
      })
      .catch((err) => {
        console.error("Tests finished:", err);
        process.exit(1);
      });
  } catch (err) {
    console.error("Failed to run tests:", err);
    process.exit(1);
  }
}

const args = require("minimist")(process.argv.slice(2));
const joyrideVSIX = args["joyride-vsix"]
  ? args["joyride-vsix"]
  : "extension-development";
const testWorkspace = args["test-workspace"]
  ? path.resolve(args["test-workspace"])
  : path.resolve(__dirname, "..", "vscode-test-runner", "workspace-1");
console.info(
  `Using:\n  Joyride: ${joyrideVSIX}\n  Test workspace: ${testWorkspace}`
);

void init()
  .then(() => main(joyrideVSIX, testWorkspace))
  .catch((error) => {
    console.error("Failed to initialize test running environment:", error);
    process.exit(1);
  });
