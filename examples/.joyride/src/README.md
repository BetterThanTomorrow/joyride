# Joyride example src files

Examples/suggestions for things that can be convenient to have accessible from your Joyride scripts. If you load a namespace from your user activate.cljs script you can bind keyboard shortcuts to the functions in them. E.g:

```clojure
(ns activate
  (:require ...
            [calva-api]
            [util.editor]
            ...))
```

Example keyboard shortcut:

```json
    {
        "key": "<some-keyboard-shortcut>",
        "command": "joyride.runCode",
        "args": "(calva-api/restart-clojure-lsp)"
    },
```