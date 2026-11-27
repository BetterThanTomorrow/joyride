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

// An editor that isn't the downloaded VS Code gets extensions and user data directories
// of its own, named by its executable, so it doesn't share them with the VS Code runs.
function profileArgsForEditor(vscodeExecutablePath, args) {
  const editorName = path
    .basename(vscodeExecutablePath)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-");
  return args.map((arg) =>
    arg.replace(/^(--(?:extensions-dir|user-data-dir)=.*)$/, `$1-${editorName}`)
  );
}

async function main(joyrideVSIXPathOrLabel, testWorkspace, vscodeExecutable) {
  try {
    const extensionTestsPath = path.resolve(__dirname, "runTests");
    const vscodeExecutablePath = vscodeExecutable
      ? vscodeExecutable
      : resolveDarwinVSCodeExecutable(await downloadAndUnzipVSCode("insiders"));
    const [cliPath, ...defaultArgs] =
      resolveCliArgsFromVSCodeExecutablePath(vscodeExecutablePath);
    const args = vscodeExecutable
      ? profileArgsForEditor(vscodeExecutablePath, defaultArgs)
      : defaultArgs;

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
      // With an editor of our own choosing, its directories go to the test run too
      launchArgs: [testWorkspace, ...(vscodeExecutable ? args : [])],
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

const args = require("minimist")(process.argv.slice(2), {
  boolean: ["local"],
});
// The editor to run the tests in, such as Cursor, in place of the VS Code Insiders that is downloaded
const vscodeExecutable = args["vscode-executable"]
  ? path.resolve(args["vscode-executable"])
  : undefined;
if (vscodeExecutable && !args.local) {
  console.error(
    "--vscode-executable needs --local: the container runs the VS Code it downloads"
  );
  process.exit(1);
}
// Without --local the tests run in a container, see run-containerized.sh
if (!args.local) {
  const containerRunner = path.resolve(__dirname, "run-containerized.sh");
  const { status } = cp.spawnSync(containerRunner, process.argv.slice(2), {
    stdio: "inherit",
  });
  process.exit(status ?? 1);
}
const joyrideVSIX = args["joyride-vsix"]
  ? args["joyride-vsix"]
  : "extension-development";
const testWorkspace = args["test-workspace"]
  ? path.resolve(args["test-workspace"])
  : path.resolve(__dirname, "..", "seatbelt-e2e", "workspace-1");
console.info(
  `Using:\n  Joyride: ${joyrideVSIX}\n  Test workspace: ${testWorkspace}\n  Editor: ${
    vscodeExecutable || "VS Code Insiders, downloaded"
  }`
);

void init()
  .then(() => main(joyrideVSIX, testWorkspace, vscodeExecutable))
  .catch((error) => {
    console.error("Failed to initialize test running environment:", error);
    process.exit(1);
  });
