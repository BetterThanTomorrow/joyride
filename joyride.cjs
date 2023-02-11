globalThis.WebSocket = require("ws");
globalThis.joyride_vscode = require("vscode")
globalThis.joyride_require = require;

function activate(context) {
  import("./out/js/joyride.js").then((joyride) => {
    return joyride.activate(context);
  });
}

// deactivate = joyride.deactivate;

exports.activate = activate;
// exports.deactivate = deactivate;

