const fs = require("fs");
const path = require("path");
const vscode = require("vscode");

function info(...xs) {
  vscode.window.showInformationMessage(xs.join(" "));
}

const rootPath = vscode.workspace.workspaceFolders[0].uri.fsPath;
info("The root path of this workspace:", rootPath);
fs.writeFileSync(
  path.resolve(rootPath, "test-from-js-script.txt"),
  "Written from a Workspace JavaScript Script!"
);
