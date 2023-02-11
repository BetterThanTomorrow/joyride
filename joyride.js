globalThis.WebSocket = require("ws");
joyride = require("./out/js/joyride.js");

function activate(context) {
  if (context != null) {
    joyride.activate(context);
  }
}

deactivate = joyride.deactivate;

exports.activate = activate;
exports.deactivate = deactivate;

