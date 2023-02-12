// load ws conditionally for shadow
ws_file = require.resolve("ws");
if (ws_file != null) {
  globalThis.WebSocket = require(ws_file);
}

globalThis.joyride_vscode = require("vscode");

function activate(context) {
  return import("./dist/out/js/joyride.js").then((joyride) => {
    var ret = joyride.activate(context);
    return ret;
  });
}

// deactivate = joyride.deactivate;

exports.activate = activate;
// exports.deactivate = deactivate;

