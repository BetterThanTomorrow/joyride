---
name: joyride-testing
description: 'Testing the Joyride under development, in the dev Extension Host. Use when: testing Joyride, smoke testing after a change or a dependency bump, validating joyride_evaluate_code, awaitResult and who-tracking, testing the nREPL server through Calva, testing Flares and their messages, testing a Scittle REPL in a Flare, testing workspace scripts, or checking that the MCP server publishes its skills.'
---

# Joyride testing

This window is the dev Extension Host. Its Joyride is the one under development, built from the `joyride` repo's `out/`, and the test project is `test-projects/example` in that repo. Each section below stands on its own: run one, a few, or all of them.

## Before you start

1. Check that your Joyride tool reaches this window's Joyride. Evaluate with `joyride_evaluate_code`:
   ```clojure
   (.. (js/require "vscode") -extensions (getExtension "betterthantomorrow.joyride") -extensionPath)
   ```
   Expect the path of the `joyride` repo, such as `"/Users/pez/Projects/joyride"`. A path under `~/.vscode/extensions/` means the tool reaches an installed Joyride in another window. Stop and tell the user.
2. Call `clojure_list_sessions`. The test project starts two sessions when the window opens (`.joyride/src/test_project.cljs`):

   | Session | REPL |
   |---------|------|
   | `joyride` | Calva connected to this window's Joyride nREPL server |
   | `tic-tac-toe` | Scittle in the tic-tac-toe Flare, connected to Calva's WebSocket server on port 13400 |

   If one is missing, run the VS Code task **test project: start**, or evaluate `(test-project/start!)` with `awaitResult: true`, and list the sessions again. It starts only what is missing and resolves to what each step found or did, such as `{:nrepl-server :was-running, :websocket-server :was-listening, :joyride :was-connected, :tic-tac-toe :was-connected}`, or `:started`, `:connected` and `:opened` for what it brought up. If a session is still missing, tell the user before running the sections that need it.

## Test conventions

Use `"joyride-tester"` as the `who` for your evaluations, and other slugs only where a section asks for them.

This window uses the user's real Joyride directory, `~/.config/joyride`. Don't create, change or delete anything there. Test files go in the test project, under `.joyride/`, and are deleted at the end of the run, as are Flares you opened. Leave the tic-tac-toe Flare open and start a new game at the end.

## Evaluation through MCP

`joyride_evaluate_code`, in the `user` namespace unless a step says otherwise.

1. `(+ 1 2)` gives `"type":"success"` and `"result":"3"`.
2. With `awaitResult: true`, `(js/Promise. (fn [r] (js/setTimeout #(r :done) 100)))` gives `"#<Promise :done>"`.
3. The same code without `awaitResult` gives a result map with `:type "promise"` and the message `Promise returned but not awaited (fire-and-forget mode)`.
4. With `awaitResult: true`, `(js/Promise.reject (js/Error. "nope"))` gives `"type":"error"` and `"error":"nope"`.
5. `(throw (ex-info "boom" {:a 1}))` gives `"type":"error"` and `"error":"boom"`.
6. As `who` `"joyride-tester-b"`, evaluate `(ns joyride-tester.scratch) (def x 42)`. Then as `"joyride-tester"`, evaluate `x` with `namespace` `"joyride-tester.scratch"`. Expect `"result":"42"` and `"otherWhosSinceLast":["joyride-tester-b"]`.

## nREPL through Calva

`clojure_evaluate_code` with `replSessionKey` `"joyride"`. This is the same SCI as the MCP tool.

1. `(+ 1 2)` in `user` gives `3`.
2. `joyride-tester.scratch/x`, defined in the MCP section, gives `42`. Run that section first, or def it here.
3. `(some? (find-ns 'test-project))` gives `true`: the activation script loaded the test project's library.

## Flares

Through `joyride_evaluate_code`, with `(require '[joyride.flare :as flare])` first.

1. Open a panel Flare that echoes what it gets, with a handler that keeps what comes back:
   ```clojure
   (def !msgs (atom []))
   (flare/flare!+ {:html [:div [:h1 "Test flare"]
                          [:script "const vscode = acquireVsCodeApi();
                                    window.addEventListener('message', e => vscode.postMessage({echo: e.data}));"]]
                   :title "Test flare"
                   :key :joyride-test
                   :message-handler (fn [m] (swap! !msgs conj (js->clj m :keywordize-keys true)))})
   ```
   With `awaitResult: true` it resolves to a map with the key `:joyride-test`.
2. `(keys (flare/ls))` has `:joyride-test` and `:tic-tac-toe`.
3. `(flare/post-message!+ :joyride-test {:hello "flare"})` with `awaitResult: true` gives `"#<Promise true>"`. Half a second later, `@!msgs` is `[{:echo {:hello "flare"}}]`.
4. `(flare/flare!+ {:html [:p "sidebar"] :key :sidebar-2 :title "Sidebar test"})` opens a sidebar Flare, and its key is in `(flare/ls)`.
5. `(flare/close! :joyride-test)` and `(flare/close! :sidebar-2)` each give `true`, and the keys are gone from `(flare/ls)`.

## A Scittle REPL in a Flare

`clojure_evaluate_code` with `replSessionKey` `"tic-tac-toe"`, in `user`. The game is `tic-tac-toe/replicant_tictactoe/` in the test project, and its state is the atom `replicant-tictactoe.core/!store`.

1. `(some? (js/document.querySelector "#app"))` gives `true`: the evaluation runs in the Flare's page.
2. `(replicant-tictactoe.core/event-handler! {} [:tic 1 1])`, then `(:tics @replicant-tictactoe.core/!store)`, gives `{[1 1] :x}`. A second move, `[:tic 0 0]`, is an `:o`.
3. Reload the Flare with `(test-project/open-tic-tac-toe!+)` through `joyride_evaluate_code`. Calva keeps its WebSocket server and takes the page back when it reconnects: within a few seconds, the session answers again, and `(:tics @replicant-tictactoe.core/!store)` is `nil`, since the page started a new game.
4. End with `(replicant-tictactoe.core/event-handler! {} [:reset])`.

## Workspace scripts

1. Create `.joyride/scripts/joyride_tester_script.cljs` in the test project, with `(ns joyride-tester-script)` and `(+ 40 2)`.
2. Through `joyride_evaluate_code`, with `awaitResult: true`: `(vscode/commands.executeCommand "joyride.runWorkspaceScript" "joyride_tester_script.cljs")` gives `"#<Promise 42>"`.
3. Delete the script.

## Bundled skills

Joyride's MCP server publishes its skills. Clients find and load them in different ways, so this section doesn't prescribe one.

1. The skills `joyride`, `joyride-user-scripting`, `joyride-workspace-scripting` and `joyride-update-llm-contexts` can be found through the surface your client has for this server's skills or resources.
2. Each one loads with its frontmatter and body intact.

Note which surface you used. The test passes when the skills are found and load without a workaround.

## Delegation to `joyride-tester`

The `joyride-tester` agent loads this skill, runs one test task, and reports. After **Before you start**, the sections without a dependency on each other can run in parallel tester agents: Evaluation through MCP, Flares, A Scittle REPL in a Flare, Workspace scripts, and Bundled skills. nREPL through Calva reads what the MCP section defined. Pass each agent its section, the session keys you found, and its own `who`.

## Reporting

Report only the sections that ran. For each one: passed, failed, or something unexpected, with the tool responses as evidence. For a failure, what was expected, what came back, and the code that was evaluated. End with what was cleaned up.
