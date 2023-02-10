# Changelog

Changes to Joyride

## [Unreleased]

- [Add rewrite-clj as built-in CLJS library](https://github.com/BetterThanTomorrow/joyride/issues/150)

## [0.0.32] - 2023-01-16

- [Make it easier to discover the full API of JavaScript objects/instances](https://github.com/BetterThanTomorrow/joyride/issues/147)

## [0.0.31] - 2023-01-15

- Fix: [Stopping nREPL server resolves without properly awaiting promises](https://github.com/BetterThanTomorrow/joyride/issues/146)

## [0.0.30] - 2023-01-06

- [Enable using JS files as user and workspace scripts](https://github.com/BetterThanTomorrow/joyride/issues/132)
- Fix: [Allow `js/require` to be used in joyride](https://github.com/BetterThanTomorrow/joyride/issues/134)
- Dev internals: [Add basic e2e tests for user scripts](https://github.com/BetterThanTomorrow/joyride/issues/136)
- Dev internals: [Ensure that Joyride's `cljs.test` actually runs tests](https://github.com/BetterThanTomorrow/joyride/issues/138)
- Fix: [Default user content places `my_lib.cljs` in `scripts` folder, should be in `src` folder](https://github.com/BetterThanTomorrow/joyride/issues/139)

## [0.0.29] - 2023-01-02

- Fix [Error `No context found` when evaluating `is` form](https://github.com/BetterThanTomorrow/joyride/issues/124)
- [Add e2e testing](https://github.com/BetterThanTomorrow/joyride/issues/125)
- Fix [Requiring a script that require a js file fails](https://github.com/BetterThanTomorrow/joyride/issues/128)
- Fix [Requiring npm modules by proxy fail](https://github.com/BetterThanTomorrow/joyride/issues/129)

## [0.0.28] - 2022-12-21

- [Document how to provide implementations of vscode/node interfaces](https://github.com/BetterThanTomorrow/joyride/issues/119)
- Add Hover Provider example
- [Upgrade to latest SCI](https://github.com/BetterThanTomorrow/joyride/issues/121)
- [Add `cljs.pprint`](https://github.com/BetterThanTomorrow/joyride/issues/122)

## [0.0.27] - 2022-12-06

- [Add `src` sub User and Workspace directories to the classpath](https://github.com/BetterThanTomorrow/joyride/issues/115)
- [Feature request: Enable requiring JavaScript files](https://github.com/BetterThanTomorrow/joyride/issues/117)

## [0.0.26] - 2022-11-28

- Fix: [v0.0.25 starts an nREPL server that Calva can't connect to](https://github.com/BetterThanTomorrow/joyride/issues/110)

## [0.0.25] - 2022-11-28

- Update SCI for performance improvements

## [0.0.24] - 2022-11-28

- Fix: [The nREPL server adds an extra newline to pretty printed results](https://github.com/BetterThanTomorrow/joyride/issues/108)

## [0.0.23] - 2022-11-22

- [Add ”Evaluate Selection” command](https://github.com/BetterThanTomorrow/joyride/issues/106)

## [0.0.22] - 2022-11-18

- [Make nREPL host address configurable](https://github.com/BetterThanTomorrow/joyride/issues/102)
- New default keyboard shortcut bindings:
  - `joyride.runCode`: `ctrl+alt+j space`
  - `joyride.runUserScript`: `ctrl+alt+j u`
  - `joyride.runWorkspaceScript`: `ctrl+alt+j w`
  - Fixes: [Default key bindings for running scripts are weird on Swedish keyboard layouts](https://github.com/BetterThanTomorrow/joyride/issues/104)
  - Fixes: [Run Workspace Script and Run User Script has the same default keybindings](https://github.com/BetterThanTomorrow/joyride/issues/100)

## [0.0.21] - 2022-10-22

- [Return results from `runCode` command](https://github.com/BetterThanTomorrow/joyride/issues/98)

## [0.0.20] - 2022-10-18

- [Bump SCI dependency](https://github.com/BetterThanTomorrow/joyride/pull/97)

## [0.0.19] - 2022-10-15

- [Add `clojure.zip`](https://github.com/BetterThanTomorrow/joyride/issues/93)
- [Add examples for using npm dependencies](https://github.com/BetterThanTomorrow/joyride/issues/94)
- [Bump promesa and add `promesa.core/doseq`](https://github.com/BetterThanTomorrow/joyride/issues/96)

## [0.0.18] - 2022-10-11

- [Support loading modules from npm](https://github.com/BetterThanTomorrow/joyride/issues/92)
- Fix: [Not disposing of disposables on dev reload](https://github.com/BetterThanTomorrow/joyride/issues/87)

## [0.0.17] - 2022-08-17

- Fix: [v0.0.16 is broken, no script using `promesa.protocols` works](https://github.com/BetterThanTomorrow/joyride/issues/85)

## [0.0.16] - 2022-08-16

- [Include `cljs.test`](https://github.com/BetterThanTomorrow/joyride/issues/76)
- Fix: [Joyride starts Calva even when not in a Clojure(script) workspace](https://github.com/BetterThanTomorrow/joyride/issues/84)

## [0.0.15] - 2022-06-01

- [Use separate namespaces for user and workspace activation scripts](https://github.com/BetterThanTomorrow/joyride/issues/73)

## [0.0.14] - 2022-05-31

- Add some logging when Joyride starts and finishes activating

## [0.0.13] - 2022-05-30

- [Less boilerplate-y require of APIs from VS Code extensions](https://github.com/BetterThanTomorrow/joyride/issues/71)

## [0.0.12] - 2022-05-18

- [Enable access to more `promesa.core` vars](https://github.com/BetterThanTomorrow/joyride/issues/68)

## [0.0.11] - 2022-05-15

- Fix: [`(require)` does not load ns from user script dir](https://github.com/BetterThanTomorrow/joyride/issues/38)
- [Add `my-lib` example to Getting Started user content](https://github.com/BetterThanTomorrow/joyride/issues/63)

## [0.0.10] - 2022-05-15

- [Make it easy to open scripts for editing](https://github.com/BetterThanTomorrow/joyride/issues/56)
- [Add Workspace script menu items for creating getting started content](https://github.com/BetterThanTomorrow/joyride/issues/57)

## [0.0.9] - 2022-05-11

- [Error when dismissing the scripts menu without selecting anything](https://github.com/BetterThanTomorrow/joyride/issues/24)
- Fix: [Better error report when a script isn't found](https://github.com/BetterThanTomorrow/joyride/issues/40)
- [nrepl: support pprinting eval results](https://github.com/BetterThanTomorrow/joyride/issues/49)
- Fix: [Joyride throws an error when activated in a window with no folder](https://github.com/BetterThanTomorrow/joyride/issues/51)
- [Create example user scripts if they are not present](https://github.com/BetterThanTomorrow/joyride/issues/52)
- [Don't unconditionally show the Joyride output channel on start](https://github.com/BetterThanTomorrow/joyride/issues/46)

## [0.0.8] - 2022-05-09

- [Enable guarding things from evaluation when loading file in the repl](https://github.com/BetterThanTomorrow/joyride/issues/4)
- [Don't automatically show the Joyride output channel when scripts run](https://github.com/BetterThanTomorrow/joyride/issues/36)
- [Remove `joyride.core/get-` prefixes](https://github.com/BetterThanTomorrow/joyride/issues/42)
- [Add `*1`, `*2`, `*3`, and `*e` REPL variables](https://github.com/BetterThanTomorrow/joyride/issues/43)

## [0.0.7] - 2022-05-06

- [Activation scripts](https://github.com/BetterThanTomorrow/joyride/issues/8)


## [0.0.6] - 2022-05-06

- [Give the scripts access to the Joyride extension context](https://github.com/BetterThanTomorrow/joyride/issues/33)

## [0.0.5] - 2022-05-06

- Fix [API call `startNReplServer` not honoring the `root-path` argument](https://github.com/BetterThanTomorrow/joyride/issues/32)

## [0.0.4] - 2022-05-05

- [API for starting the nREPL server](https://github.com/BetterThanTomorrow/joyride/issues/28)
- [`when` clause contexts for nREPL server](https://github.com/BetterThanTomorrow/joyride/issues/29)

## [0.0.3] - 2022-05-02

- [Add `joyride.core/*file*`](https://github.com/BetterThanTomorrow/joyride/issues/5)
- [Change name of command *Run Script* -> *Run Clojure Code*]((https://github.com/BetterThanTomorrow/joyride/issues/20))
- [Introduce User scripts](https://github.com/BetterThanTomorrow/joyride/issues/5)

## [0.0.2] - 2022-04-27

- Assorted fixes, e.g. [Joyride alerts a lot of nrepl server diagnostic](https://github.com/BetterThanTomorrow/joyride/issues/3)

## [0.0.1] - 2022-04-27

- [Joyride VS Code using Clojure](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.joyride)
  - Workspace scripts
  - nREPL server
