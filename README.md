# Take VSCode for a Joyride!

<img src="assets/joyride.png" height=300></img>


Modify your editor by executing ClojureScript code in your REPL adn/or run scripts via keyboard shortcuts you choose. The Visual Studio Code API is at your command!

Joyride is Powered by [SCI](https://github.com/babashka/sci) (Small Clojure Interpreter).

## WIP

You are entering a construction yard. Your feedback is so welcome!

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

See [doc/configuration.md](doc/configuration.md) for full configuration options

## Support and feedback

You'll find us in the `#joyride` channel on the [Clojurians Slack](http://clojurians.net)

## News

### Twitter

Follow the [#vsjoyride](https://twitter.com/search?q=%23vsjoyride&src=typed_query&f=live) hashtag on Twitter!
