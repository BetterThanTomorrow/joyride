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

// @vscode/test-electron still returns …/MacOS/Electron on darwin.
// VS Code 1.110+ renamed that binary (Code / Code - Insiders); the Electron
// compat symlink was removed ~2026-07-20. Upstream fix:
// https://github.com/microsoft/vscode-test/pull/350 — not on npm yet.
// Remove this helper once @vscode/test-electron publishes that fix.
function resolveDarwinVSCodeExecutable(vscodeExecutablePath) {
  if (process.platform !== "darwin" || fs.existsSync(vscodeExecutablePath)) {
    return vscodeExecutablePath;
  }
  const macosDir = path.dirname(vscodeExecutablePath);
  const infoPlistPath = path.resolve(macosDir, "..", "Info.plist");
  try {
    const plist = fs.readFileSync(infoPlistPath, "utf8");
    const match = plist.match(
      /<key>CFBundleExecutable<\/key>\s*<string>([^<]+)<\/string>/
    );
    if (match) {
      const resolved = path.resolve(macosDir, match[1]);
      if (fs.existsSync(resolved)) {
        console.info(
          `Resolved VS Code executable via Info.plist: ${resolved}`
        );
        return resolved;
      }
    }
  } catch (err) {
    console.warn(
      "Failed to resolve darwin VS Code executable from Info.plist:",
      err
    );
  }
  return vscodeExecutablePath;
}

function init() {
  return new Promise((resolve, reject) => {
    try {
      const USER_CONFIG_PATH_KEY = "VSCODE_JOYRIDE_USER_CONFIG_PATH";
      if (!process.env[USER_CONFIG_PATH_KEY]) {
        const tmpConfigPath = path.join(
          os.tmpdir(),
          "seatbelt-e2e-joyride",
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
    const vscodeExecutablePath = resolveDarwinVSCodeExecutable(
      await downloadAndUnzipVSCode("insiders")
    );
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
  : path.resolve(__dirname, "..", "seatbelt-e2e", "workspace-1");
console.info(
  `Using:\n  Joyride: ${joyrideVSIX}\n  Test workspace: ${testWorkspace}`
);

void init()
  .then(() => main(joyrideVSIX, testWorkspace))
  .catch((error) => {
    console.error("Failed to initialize test running environment:", error);
    process.exit(1);
  });
