const vscode = require("vscode");
const congas = require("./out/congas.js");

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
  congas.activate(context);
}

function deactivate() {
  congas.deactivate();
}

module.exports = {
  activate,
  deactivate,
};
