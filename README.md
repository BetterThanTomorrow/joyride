# Joyride VS Code with Clojure!

Modify your editor by executing ClojureScript code in your REPL and/or run scripts via keyboard shortcuts you choose. The Visual Studio Code API is at your command!

https://user-images.githubusercontent.com/30010/165934412-ffeb5729-07be-4aa5-b291-ad991d2f9f6c.mp4

[The video in much better quality on Youtube (CalvaTV)](https://www.youtube.com/watch?v=V1oTf-1EchU)

Joyride is Powered by [SCI](https://github.com/babashka/sci) (Small Clojure Interpreter).

## WIP

You are entering a construction yard. Things are going to change and break your configs while we are searching for good APIs and UI/Ux.

Your feedback is highly welcome!

## Quickest Start

1. Bring up the VS Code Command Palette (`cmd/ctrl+shift+p`)
2. Execute **Joyride: Run Clojure Code**
3. Type in some code into the prompt, e.g.
    ```clojure
    (require '["vscode" :as vscode]) (vscode/window.showInformationMessage "Hello World!")`
    ```
4. Submit

## Quickstart

Create a script in your workspace under `.joyride/scripts`, e.g. `example.cljs`:

``` clojure
(ns example
  (:require ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as str]))

(defn info [& xs]
  (vscode/window.showInformationMessage (str/join " " xs)))

(info "The root path of this workspace:" vscode/workspace.rootPath)

(fs/writeFileSync (path/resolve vscode/workspace.rootPath "test.txt") "written!")
```

This script gives one information message and writes to a file `test.txt` in
your workspace.

Then in your keyboard shortcuts, add:

``` json
 {
        "key": "cmd+1",
        "command": "joyride.runWorkspaceScript",
        "args": "example.cljs"
 }
```

Now you can run the `example.cljs` script by just hitting Cmd+1!

See [doc/configuration.md](https://github.com/BetterThanTomorrow/joyride/blob/master/doc/configuration.md) for full configuration options

## Examples

See the [examples](./examples) for examples including:

* Creating an interactive Webview
* Terminal creation and control
* Fontsize manipulation
* Calva Structural Editing enhancements

## There's a REPL server

While developing Joyride scripts you should of course do it leveraging Interactive Programming (see [this video](https://www.youtube.com/watch?v=d0K1oaFGvuQ) demonstrating it). Here are the steps:

1. Issue the command <kbd>**Joyride: Start nREPL**</kbd>. This will start Joyride's nREPL server.
2. Connect your Clojure editor (we suggest [Calva](https://calva.io) for super biased reasons.)
3. Open a script (presumeably in the `.joyride/scripts` folder)
4. Hack away!

## Support and feedback

You'll find us in the `#joyride` channel on the [Clojurians Slack](http://clojurians.net)

## News

### Twitter

Follow the [#vsjoyride](https://twitter.com/search?q=%23vsjoyride&src=typed_query&f=live) hashtag on Twitter!
