const vscode = require("vscode");
const joy = require("./out/joy.js");

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
  joy.activate(context);
}

function deactivate() {
  joy.deactivate();
}

module.exports = {
  activate,
  deactivate,
};
