# Joyride Examples

Demonstrating some ways to [Joyride VS Code](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.joyride).

<img src="../assets/joyride-logo.header.png" width=500></img>

In the `.joyride/scripts` folder you'll find mostly small examples:

## Activation script

A Workspace [activate.cljs] script that registers a `vscode/workspace.onDidOpenTextDocument` event handler. Demonstrates:

* Using the `joyride.core/extension-context` to push disposables on its `subscriptions` array. Making VS Code dispose of them when Joyride is deactivated.
* A re-runnable recipe to avoid re-registering the event handler. (By disposing it and then re-register.)

## Create a Webview

`webview/example.cljs`

Create a Webview. Uses [scittle](https://babashka.org/scittle/) + [reagent](https://github.com/reagent-project/reagent) for the contents of the webview.

Live demo here: https://twitter.com/borkdude/status/1519607386218053632

## Terminal

`.joyride/scripts/terminal.cljs`

Create a Terminal, send text to it and show it.

![](../assets/joyride-demo-terminal.gif)

## Fontsize

`.joyride/scripts/fontsize.cljs`

Manipulates the editor font size.

Live demo: https://twitter.com/borkdude/status/1519709769157775360

## Structural Editing

`.joyride/scripts/ignore_form.cljs`

Adds a command for (un)ignoring (Clojure-wise) the current enclosing form.
Depends on that the [Calva](calva.io) extension is installed, because it is what
helps us find out of the current list in order to insert, or remove, the ignore
tag (`#_`).

If you want to use this script, you can setup a VSCode key binding for it by
editing VSCode's keybindings JSON and adding the following. Note that this
overrides the default comment-keyboard-shortcut on macOS. The result is that
pressing CMD-/ in a Clojure file will use the (un)ignore script when there is no
selection, and keep using the default comment action (prepend the line with `;;`
when there is a selection). For Windows you probably want to change the key to
the default Windows comment keyboard shortcut.

```json
{
  "key": "cmd+/",
  "command": "joyride.runWorkspaceScript",
  "args": "ignore_form.cljs",
  "when": "!editorHasSelection && editorTextFocus && !editorReadOnly && editorLangId =~ /clojure|scheme|lisp/"
}
```

* Video here: https://www.youtube.com/watch?v=V1oTf-1EchU
* Tweet to like/comment/retweet: https://twitter.com/pappapez/status/1519825664177807363

## Joyride API

Joyride comes with the `joyride.core` namespace, giving you access to things as the extension context, the Joyride output channel, and some info about the evaluation environment.

And, you can also script Joyride with Joyride using its Extension API.

Example script: [`.joyride/scripts/joyride_api.cljs`](.joyride/scripts/joyride_api.cljs)

See also: the [Joyride API docs](../doc/api.md)

## Opening a file

The [open_document.cljs](.joyride/scripts/open_document.cljs) script asks if you want to open one of the examples and then opens a random `.cljs` file from the `scripts` folder.

Joyride API used:

* [`promesa.core`](https://funcool.github.io/promesa/latest/user-guide.html)

[VS Code APIs](https://code.visualstudio.com/api/references/vscode-api) used:

* `vscode/window.showInformationMessage`
* `vscode/workspace.findFiles`
* `vscode/workspace.openTextDocument`
* `vscode/window.showTextDocument`