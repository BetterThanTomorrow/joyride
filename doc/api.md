# API

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

- `let`, `do!`

## Possibly coming additions

We want to add `clojure.test` and `clojure.pprint` as well in the near future. How near/if depends on things like how much time we can spend on it, and how easy/hard it will be to port this over from [nbb](https://github.com/babashka/nbb).