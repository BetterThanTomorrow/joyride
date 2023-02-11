globalThis.WebSocket = require("ws");
joyride = require("./out/js/joyride.cjs");

function activate(context) {
  if (context != null) {
    return joyride.activate(context);
  }
}

deactivate = joyride.deactivate;

exports.activate = activate;
exports.deactivate = deactivate;

