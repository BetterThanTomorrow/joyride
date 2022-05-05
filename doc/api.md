# APIs

There are two different APIs here:

1. The Joyride *Scripting API*
1. The Joyride *Extension API*

Please note that Joyride's *Extension API* is available from *Joyride scripts*.

## Scripting API

In addition to `clojure.core`, `clojure.set`, `clojure.edn`, `clojure.string`,
`clojure.walk`, `clojure.data`, Joyride exposes
the following namespaces:

## Joyride

### `joyride.core`

- `*file*`: dynamic var representing the currently executing file.

NB: While using `*file*` bare works, this will probably stop working soon. Always use it from `joyride.core`, e.g.:

```clojure
(ns your-awesome-script
  (:require [joyride.core :refer [*file*]]
            ...))
```

## Promesa

See [promesa docs](https://cljdoc.org/d/funcool/promesa/6.0.2/doc/user-guide).

### `promesa.core`

`p/->>`, `p/->`, `p/all`, `p/any`, `p/catch`, `p/chain`, `p/create`, `p/deferred`, `p/delay`, `p/do`, `p/do`, `p/done`, `p/finally`, `p/let`, `p/map`, `p/mapcat`, `p/pending`, `p/promise`, `p/promise`, `p/race`, `p/rejected`, `p/rejected`, `p/resolved`, `p/resolved`, `p/run`, `p/then`, `p/thenable`, `p/with`, and `p/wrap`

## Possibly coming additions

We want to add `clojure.test` and `clojure.pprint` as well in the near future. How near/if depends on things like how much time we can spend on it, and how easy/hard it will be to port this over from [nbb](https://github.com/babashka/nbb).

## Extension API

When the Joyride extension has activated it has the following API:

* `startNReplServer [project-root-path]`
   * Returns a promise resolving to the port where the nREPL server started
   * `project-root-path` is optional, defaulting to `vscode/workspace.rootPath`

