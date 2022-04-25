const vscode = require("vscode");
const joyride = require("./out/joyride.js");

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
  joyride.activate(context);
}

function deactivate() {
  joyride.deactivate();
}

module.exports = {
  activate,
  deactivate,
};
