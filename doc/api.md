# APIs

The Joyride API consist of:

1. The Joyride *Scripting API*
   * Scripting life-cycle management
   * Included clojure library namespaces
1. The Joyride *Extension API*

Give the [joyride_api.cljs](../examples/.joyride/scripts/joyride_api.cljs) example a spin, why don't ya!

Please note that Joyride's *Extension API* is also available to *Joyride scripts*.

## Scripting API

### Scripting life-cycle

You can make some code run when Joyride activates, by naming the scripts `activate.cljs`. The activations script will be run in the order:

1. `<User scripts directory>/activate.cljs`
1. `<Workspace scripts directory>/activate.cljs`

Look at the [Activation example](../examples/.joyride/scripts/activate.cljs) script for a way to use this, and for a way to make the script re-runnable.

### Namespaces

In addition to `clojure.core`, `clojure.set`, `clojure.edn`, `clojure.string`,
`clojure.walk`, `clojure.data`, Joyride exposes
the following namespaces:

#### `joyride.core`

- `*file*`: dynamic var holding the absolute path of file where the current evaluation is taking place
- `get-invoked-script`: function returning the absolute path of the invoked script when running as a script. Otherwise returns `nil`. Together with `*file*` this can be used to create a guard that avoids running certain code when you load a file in the REPL:
  ```clojure
  (when (= (joyride/get-invoked-script) joyride/*file*)
    (main))
  ```
- `get-extension-context`: function returning the Joyride [ExtensionContext](https://code.visualstudio.com/api/references/vscode-api#ExtensionContext) instance
- `get-output-channel`: function returning the Joyride [OutputChannel](https://code.visualstudio.com/api/references/vscode-api#OutputChannel) instance

Here's a snippet from the [joyride_api.cljs](../examples/.joyride/scripts/joyride_api.cljs) example.

```clojure
(ns your-awesome-script
  (:require [joyride.core :as joyride]
            ...)

(doto (joyride/get-output-channel)
  (.show true)
  (.append "Writing to the ")
  (.appendLine "Joyride output channel.")
  (.appendLine (str "Joyride extension path: "
                    (-> (joyride/get-extension-context)
                        .-extension
                        .-extensionPath)))
  (.appendLine (str "joyride/*file*: " joyride/*file*))
  (.appendLine (str "Invoked script: " (joyride/get-invoked-script)))
  (.appendLine "🎉"))
```

**NB**: Currently, using bar `*file*` works. But it will probably stop working soon. Always use it from `joyride.core`.

#### promesa.core

See [promesa docs](https://cljdoc.org/d/funcool/promesa/6.0.2/doc/user-guide).

**``**: `p/->>`, `p/->`, `p/all`, `p/any`, `p/catch`, `p/chain`, `p/create`, `p/deferred`, `p/delay`, `p/do`, `p/do!`, `p/done?`, `p/finally`, `p/let`, `p/map`, `p/mapcat`, `p/pending`, `p/promise`, `p/promise?`, `p/race`, `p/rejected`, `p/rejected?`, `p/resolved`, `p/resolved?`, `p/run!`, `p/then`, `p/thenable?`, `p/with`, and `p/wrap`

### Possibly coming additions

We want to add `clojure.test` and `clojure.pprint` as well in the near future. How near/if depends on things like how much time we can spend on it, and how easy/hard it will be to port this over from [nbb](https://github.com/babashka/nbb).

## Extension API

Joyride's Extension API has two parts:

1. [`when` clauses contexts](https://code.visualstudio.com/api/references/when-clause-contexts)
1. The `exports` map/object on the activated [extension](https://code.visualstudio.com/api/references/vscode-api#extensions) instance.

### `when` clause context

The following contexts are available for users of Joyride when binding commands to keyboard shortcuts:

* `joyride.isActive`, `boolean` - Whether the joyRide extension is active or not
* `joyride.isNReplServerRunning`, `boolean` - Whether the Joyride nREPL server is running or not

If your script needs access to these contexts, use the `getContextValue` function from the [Joyride `exports`](#exports) API.

### `exports`

When the Joyride extension has activated it has the following API:

* `startNReplServer [project-root-path]`
   * Returns a promise resolving to the port where the nREPL server started
   * `project-root-path` is optional, defaulting to `vscode/workspace.rootPath`
* `getContextValue context-key`
   * Returns the value of a Joyride [`when` clause context](#when-clause-context)
   * Returns undefined for non-existing `context-key`s
