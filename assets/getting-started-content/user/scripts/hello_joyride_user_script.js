const vscode = require("vscode");

// JavaScript scripts work, but is quite experimental and not encouraged

const hello = () => {
  return "Hello World!";
};

const showHelloMessage = async () => {
  const button = await vscode.window.showInformationMessage("Hello World!", "Cancel", "OK");
  if (button === "OK") {
    vscode.window.showInformationMessage("You clicked OK! Try clicking Cancel too?.");
  } else {
    const name = await vscode.window.showInputBox({
      title: "CIA wants to know",
      prompt: "What is your name?",
    });
    vscode.window.showInformationMessage(`Hello ${name}!`);
  }
};

hello();
showHelloMessage();

exports.hello = hello;
exports.showHelloMessage = showHelloMessage;
