import * as ws from 'ws';
import * as vscode from 'vscode';

globalThis.WebSocket = ws;
globalThis.joyride_vscode = vscode;
// globalThis.joyride_require = require;

export function activate(context) {
  return import("./out/js/joyride.js").then((joyride) => {
    var ret = joyride.activate(context);
    return ret;
  });
}

// deactivate = joyride.deactivate;

// exports.activate = activate;
// exports.deactivate = deactivate;

