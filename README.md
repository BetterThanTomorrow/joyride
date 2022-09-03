# Making VS Code Hackable

Modify your editor by executing ClojureScript code in your REPL and/or run scripts via keyboard shortcuts you choose. The Visual Studio Code API, as well as the APIs of its extensions, are at your command!

https://user-images.githubusercontent.com/30010/165934412-ffeb5729-07be-4aa5-b291-ad991d2f9f6c.mp4

[The video in much better quality on Youtube (CalvaTV)](https://www.youtube.com/watch?v=V1oTf-1EchU)

Joyride is Powered by [SCI](https://github.com/babashka/sci) (Small Clojure Interpreter).

See [doc/api.md](https://github.com/BetterThanTomorrow/joyride/blob/master/doc/api.md) for documentation about the Joyride API.

## WIP

You are entering a construction yard. Things are going to change and break your configs while we are searching for good APIs and UI/Ux.

Your feedback is highly welcome!

## User and Workspace scripts

Joyride supports User and Workspace scripts:

* _User_ scripts: `<user home>/.config/joyride/scripts`
* _Workspace_ scripts: `<workspace root>/.joyride/scripts`

You can run or open the scripts using commands provided (search the command palette for **Joyride**):

* **Joyride Run User Script...**, default keybinding `ctrl+shift+,`
* **Joyride Open User Script...**
* **Joyride Run Workspace Script...**, default keybinding `ctrl+shift+.`
* **Joyride Open Workspace Script...**

**Note, about namespaces**: Joyride effectively has a classpath that is `user:workspace`. A file `<User scripts dir>/foo_bar.cljs`, will establish/use a namespace `foo-bar`. As will a file `<Workspace scripts dir>/foo_bar.cljs`. Any symbols in these files will be shared/overwritten, as the files are loaded and reloaded. There are probably ways to use this as a power. Please treat it as a super power, because you might also hurt yourself with it.

## Quickest Start 1 - Run a User Script

Install Joyride. It will run a sample `activate.cljs` User script. You can use this script as a base for init/activation stuff of your VS Code environment.

Joyride installs a "regular” User script as well. You can run either of these with the commands mentioned above.

## Quickest Start 2 - Run some Code

1. Bring up the VS Code Command Palette (`cmd/ctrl+shift+p`)
2. Execute **Joyride: Run Clojure Code**
3. Type in some code into the prompt, e.g.
    ```clojure
    (require '["vscode" :as vscode]) (vscode/window.showInformationMessage "Hello World!")
    ```
4. Submit


## Quick Start - Start the REPL

While developing Joyride scripts you should of course do it leveraging Interactive Programming (see [this video](https://www.youtube.com/watch?v=d0K1oaFGvuQ) demonstrating it). With Calva it is very quick to start a Joyride REPL and connect Calva to it. This video demonstrates starting from scratch, including installing Joyride.

https://user-images.githubusercontent.com/30010/167246562-24638f12-120b-48e9-893a-7408d5beeb77.mp4

The demo ”project” used here is only a directory with this file `hello_joyride.cljs`. Here's the code, if you want to try it out yourself:

```clojure
(ns hello-joyride
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

(comment
  (+ 1 2 3 4 5 6 7 8 6)
  (-> (vscode/window.showInformationMessage
       "Come on, Join the Joyride!"
       "Be a Joyrider")
      (p/then
       (fn [choice]
         (println "You choose to:" choice)))))

"Hello World"
```

## Quick Start - Start your Scripts Library

Joyride lets you bind keyboard shortcuts to its User and Workspace scripts.

* User Scripts: `<user home>/.config/joyride/scripts`
* Workspace scripts: `<workspace root>/.joyride/scripts`

Let's go with a Workspace script:

Create a script in your workspace, e.g `.joyride/scripts/example.cljs`:

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

See [doc/configuration.md](https://github.com/BetterThanTomorrow/joyride/blob/master/doc/configuration.md) for full configuration options.

## Examples

See the [examples](./examples) for examples including:

* Creating an interactive Webview
* Terminal creation and control
* Fontsize manipulation
* Calva Structural Editing enhancements
* Opening and showing project files
* Workspace activation script
* The Joyride Extension API
* The `joyride.core` namespace

## Support and feedback

You'll find us in the `#joyride` channel on the [Clojurians Slack](http://clojurians.net)

## News

* Show HN: https://news.ycombinator.com/item?id=31203024#31206003

### Twitter

Follow the [#vsjoyride](https://twitter.com/search?q=%23vsjoyride&src=typed_query&f=live) hashtag on Twitter!
