globalThis.WebSocket = require("ws");
globalThis.joyride_vscode = require("vscode")
globalThis.joyride_require = require;

function activate(context) {
  return import("./out/js/joyride.js").then((joyride) => {
    var ret = joyride.activate(context);
    return ret;
  });
}

// deactivate = joyride.deactivate;

exports.activate = activate;
// exports.deactivate = deactivate;

