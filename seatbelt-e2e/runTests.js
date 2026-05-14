const vscode = require("vscode");

exports.run = () => {
  return vscode.commands.executeCommand("joyride.runCode", "(require '[integration-test.runner :as runner]) (runner/run-all-tests)");
}
