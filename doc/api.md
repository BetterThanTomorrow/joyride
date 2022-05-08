# APIs

The Joyride API consist of:

1. The Joyride *Scripting API*
   * Scripting life-cycle management
   * Included clojure library namespaces
1. The Joyride *Extension API*

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

- `*file*`: dynamic var representing the currently executing file
- `get-extension-context`: function returning the Joyride [extension context](https://code.visualstudio.com/api/references/vscode-api#ExtensionContext) object

NB: While using `*file*` bare works, it will probably stop working soon. Always use it from `joyride.core`, e.g.:

```clojure
(ns your-awesome-script
  (:require [joyride.core :refer [*file*]]
            ...))
```

#### promesa.core

See [promesa docs](https://cljdoc.org/d/funcool/promesa/6.0.2/doc/user-guide).

**``**: `p/->>`, `p/->`, `p/all`, `p/any`, `p/catch`, `p/chain`, `p/create`, `p/deferred`, `p/delay`, `p/do`, `p/do`, `p/done`, `p/finally`, `p/let`, `p/map`, `p/mapcat`, `p/pending`, `p/promise`, `p/promise`, `p/race`, `p/rejected`, `p/rejected`, `p/resolved`, `p/resolved`, `p/run`, `p/then`, `p/thenable`, `p/with`, and `p/wrap`

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
