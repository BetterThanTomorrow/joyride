const vscode = require("vscode");

exports.run = () => {
  return vscode.commands.executeCommand("joyride.runCode", "(require '[integration-test.index :as it]) (it/run-all-tests)");
}
