// load ws conditionally for shadow
try {
  globalThis.WebSocket = require("ws");
}
catch (_) {
  console.info("WebSocket not defined, since 'ws' library is not available.");
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

