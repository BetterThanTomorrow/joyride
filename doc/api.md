# APIs

The Joyride API consist of:

1. The Joyride *Extension API*
   * `exports`
   * Extension commands
   * `when` clause context
1. The Joyride *Scripting API*
   * Scripting lifecycle management
   * Included clojure library namespaces

Give the [joyride_api.cljs](../examples/.joyride/scripts/joyride_api.cljs) example a spin, why don't ya!

Please note that Joyride's *Extension API* is also available to *Joyride scripts*.

## Extension API

Joyride's Extension API has these parts:

1. The `exports` map/object on the activated [extension](https://code.visualstudio.com/api/references/vscode-api#extensions) instance.
1. The Joyride extension commands.
1. [`when` clauses contexts](https://code.visualstudio.com/api/references/when-clause-contexts)

### `exports`

When the Joyride extension has activated it has the following API:

* `joyride.runCode`
  * Evaluates the the string provided and returns a promise with the result.
* `startNReplServer [project-root-path]`
   * Returns a promise resolving to the port where the nREPL server started
   * `project-root-path` is optional, defaulting to `vscode/workspace.rootPath`
* `getContextValue context-key`
   * Returns the value of a Joyride [`when` clause context](#when-clause-context)
   * Returns undefined for non-existing `context-key`s

You reach the API through the `ezports` field on the Joyride extension:

``` js
const joyrideExtension = vscode.extensions.getExtension("betterthantomorrow.joyride");
const joyride = joyrideExtension.exports;
```

Note that `runCode` will return the ClojureScript results. And if an error occurs it will be a ClojureScript error. This means that if you are consuming the API from JavaScript/TypeScript you will need to convert the results, as well as any error. You can use Joyride's ClojureScript (SCI) interpreter for this:

``` js
const toJS = await joyride.runCode("clj->js");
const exData = await joyride.runCode("ex-data");

const r = joyride.runCode("{:a (some-fn)}")
  .catch(e => vscode.windiow.showErrorMessage(JSON.stringify(toJS(exData(e)))));

if (r) {
  const js_r = await toJS(r);
  vscode.window.showInformationMessage(js_r);
}
```

### Extension commands

Select Joyride from the VS Code Extension pane to see which commands it provides. The commands you'll probably use the most are:

* `joyride.runCode`
* `joyride.evaluatSelection `
* `joyride.runUserScript`
* `joyride.runWorkspaceScript`

The same note about ClojureScript applias for the `joyride.runCode` command as for the corresponding API export, mentioned above. Fetching the `clj->js` function looks more like so in this case:

``` js
const toJS = await vscode.commands.executeCommand('joyride.runCode', "clj->js");
```

### `when` clause context

The following contexts are available for users of Joyride when binding commands to keyboard shortcuts:

* `joyride.isActive`, `boolean` - Whether the joyRide extension is active or not
* `joyride.isNReplServerRunning`, `boolean` - Whether the Joyride nREPL server is running or not

If your script needs access to these contexts, use the `getContextValue` function from the [Joyride `exports`](#exports) API.

## Scripting API

### The Joyride classpath

Joyride has a fixed ”classpath” (the paths where it looks for roots to your Joyride code source files). It searches these directories in this order:

1. `<workspace-root>/.joyride/src`
1. `<workspace-root>/.joyride/scripts`
1. `<user-home>/.config/joyride/src`
1. `<user-home>/.config/joyride/scripts`

The first file found will be used.

### Scripting lifecycle

You can make some code run when Joyride activates, by naming the scripts `activate.cljs`. The activations script will be run in the order:

1. `<User scripts directory>/activate.cljs`
1. `<Workspace scripts directory>/activate.cljs`

Look at the [Activation example](../examples/.joyride/scripts/activate.cljs) script for a way to use this, and for a way to make the script re-runnable.

### NPM modules

To use npm modules these need to be installed in the path somewhere in the path from where the script using it resides, to the root of the filessystem. Consider using `<user-home>/.config/joyride` and `<ws-root>/.joyride`. (`yarn` or `npm i` both work, Joyride doesn't care, it looks for stuff in `node_modules`).

See [examples/.joyride/scripts/html_to_hiccup.cljs](examples/.joyride/scripts/html_to_hiccup.cljs) for an example.

The modules you use need to be in CommonJS format.

### JavaScript files

You can require JavaScript files (CommonJS) from your scripts by using absolute or relative paths to the JS files. Like in JS, relative paths need to start with a `.` to separate them from node module requires. If you provide the `:reload` option to the require form, the code in these files are reloaded when you re-require them from Clojure code. See [Examples Requiring JavaScript Files](../examples/README.md#requiring-javascript-files) for an example.

### VS Code, and Extension ”namespaces”

Joyride exposes its `vscode` module for scripts to consume. You require it like so:

```clojure
(ns joyride-api
  (:require ...
            ["vscode" :as vscode]
            ...))
```

VS Code Extensions that export an API can be required using the `ext://` prefix followed by the extension identifier. For instance, to require [Calva's Extension API](https://calva.io/api/) use `"ext://betterthantomorrow.calva"`. Optionally you can specify any submodules in the exported API by suffixing the namespace with a `$` followed by the dotted path of the submodule. You can also `refer` objects in the API and submodules. Like so:

```clojure
(ns z-joylib.calva-api
  (:require ...
            ["ext://betterthantomorrow.calva$v0" :as calva :refer [repl ranges]])
            ...)

(def current-form-text (second (ranges.currentForm)))
```

### ClojureScript Namespaces

In addition to `clojure.core`, `clojure.set`, `clojure.edn`, `clojure.string`,
`clojure.walk`, `clojure.data`, `cljs.test`, and `clojure.zip`, Joyride exposes
the following namespaces:

Lacking some particular library? Please consider contributing to [babashka/sci.configs](https://github.com/babashka/sci.configs)!

#### `joyride.core`

- `*file*`: dynamic var holding the absolute path of file where the current evaluation is taking place
- `invoked-script`: function returning the absolute path of the invoked script when running as a script. Otherwise returns `nil`. Together with `*file*` this can be used to create a guard that avoids running certain code when you load a file in the REPL:
  ```clojure
  (when (= (joyride/invoked-script) joyride/*file*)
    (main))
  ```
- `extension-context`: function returning the Joyride [ExtensionContext](https://code.visualstudio.com/api/references/vscode-api#ExtensionContext) instance
- `output-channel`: function returning the Joyride [OutputChannel](https://code.visualstudio.com/api/references/vscode-api#OutputChannel) instance

Here's a snippet from the [joyride_api.cljs](../examples/.joyride/scripts/joyride_api.cljs) example.

```clojure
(ns your-awesome-script
  (:require [joyride.core :as joyride]
            ...))

(doto (joyride/output-channel)
  (.show true)
  (.append "Writing to the ")
  (.appendLine "Joyride output channel.")
  (.appendLine (str "Joyride extension path: "
                    (-> (joyride/extension-context)
                        .-extension
                        .-extensionPath)))
  (.appendLine (str "joyride/*file*: " joyride/*file*))
  (.appendLine (str "Invoked script: " (joyride/invoked-script)))
  (.appendLine "🎉"))
```

**NB**: Currently, using bar `*file*` works. But it will probably stop working soon. Always use it from `joyride.core`.

#### promesa.core

See [promesa docs](https://cljdoc.org/d/funcool/promesa/6.0.2/doc/user-guide).

- `p/*loop-run-fn*`
- `p/->`
- `p/->>`
- `p/TimeoutException`
- `p/all`
- `p/any`
- `p/as->`
- `p/bind`
- `p/cancel!`
- `p/cancelled?`
- `p/catch`
- `p/catch'`
- `p/chain`
- `p/chain'`
- `p/create`
- `p/deferred`
- `p/deferred?`
- `p/delay`
- `p/do`
- `p/do!`
- `p/doseq`
- `p/done?`
- `p/err`
- `p/error`
- `p/finally`
- `p/future`
- `p/handle`
- `p/let`
- `p/loop`
- `p/map`
- `p/mapcat`
- `p/pending?`
- `p/plet`
- `p/promise`
- `p/promise?`
- `p/promisify`
- `p/race`
- `p/recur`
- `p/reject!`
- `p/rejected`
- `p/rejected?`
- `p/resolve!`
- `p/resolved`
- `p/resolved?`
- `p/run!`
- `p/then`
- `p/then'`
- `p/thenable?`
- `p/timeout`
- `p/with-redefs`
- `p/wrap`

### Possibly coming additions

We want to add `clojure.test` and `clojure.pprint` as well in the near future. How near/if depends on things like how much time we can spend on it, and how easy/hard it will be to port this over from [nbb](https://github.com/babashka/nbb).


