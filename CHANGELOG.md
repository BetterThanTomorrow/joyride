# Change Log

Changes to Joyride

## [Unreleased]

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
