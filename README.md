# Joyride

Take VSCode for a Joyride!

Execute CLJS scripts using keyboard shortcuts.

Powered by [SCI](https://github.com/babashka/sci) (Small Clojure Interpreter).

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

## News

### Twitter

Follow the [#vsjoyride](https://twitter.com/search?q=%23vsjoyride&src=typed_query&f=live) hashtag on Twitter!
