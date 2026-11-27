---
name: joyride-testing
description: 'Testing the Joyride under development, in the dev Extension Host. Use when: testing Joyride, smoke testing after a change or a dependency bump, validating joyride_evaluate_code, awaitResult and who-tracking, testing the nREPL server through Calva, testing Flares, sidebar Flares and their messages, testing a Scittle REPL in a Flare, testing workspace scripts, or checking that the MCP server publishes its skills.'
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

This window uses the user's real Joyride directory, `~/.config/joyride`. Don't create, change or delete anything there. Test files go in the test project, under `.joyride/`.

Some steps wait for a page to answer. `test-project/wait-for!+` takes a function and a timeout in milliseconds, calls the function every half second until it returns something truthy, and resolves to the last value. Evaluate it with `awaitResult: true`.

The tic-tac-toe Flare keeps its page while other editors cover it, so a section can open panel Flares while a game is going. Once the Joyride sidebar has been shown, `(flare/ls)` also lists `:sidebar-1`, the placeholder view of the first sidebar slot. It isn't something a test left behind.

Each section ends by cleaning up after itself: it closes the Flares it opened, deletes its test files, and removes its namespaces after switching back to `user`. Leave the tic-tac-toe Flare open, with a new game.

## Evaluation through MCP

`joyride_evaluate_code`, in the `user` namespace unless a step says otherwise.

1. `(+ 1 2)` gives `"type":"success"` and `"result":"3"`.
2. With `awaitResult: true`, `(js/Promise. (fn [r] (js/setTimeout #(r :done) 100)))` gives `"#<Promise :done>"`.
3. The same code without `awaitResult` gives a result map with `:type "promise"` and the message `Promise returned but not awaited (fire-and-forget mode)`.
4. With `awaitResult: true`, `(js/Promise.reject (js/Error. "nope"))` gives `"type":"error"` and `"error":"nope"`.
5. `(throw (ex-info "boom" {:a 1}))` gives `"type":"error"` and `"error":"boom"`.
6. As `who` `"joyride-tester-b"`, evaluate `(ns joyride-tester.scratch) (def x 42)`. Then as `"joyride-tester"`, evaluate `x` with `namespace` `"joyride-tester.scratch"`. Expect `"result":"42"` and `"otherWhosSinceLast":["joyride-tester-b"]`.
7. Still as `"joyride-tester"`, evaluate `(str *ns*)` with `namespace` `"user"`. Expect `"result":"\"user\""` and `"ns":"user"`: the evaluation runs in the namespace it names, not in the one step 6 ended in.
8. Clean up with `(in-ns 'user) (remove-ns 'joyride-tester.scratch)`.

## nREPL through Calva

`clojure_evaluate_code` with `replSessionKey` `"joyride"`. This is the same SCI as the MCP tool.

1. `(+ 1 2)` in `user` gives `3`.
2. Evaluate `(ns joyride-tester.nrepl) (def y 43)`. Then, through `joyride_evaluate_code`, `joyride-tester.nrepl/y` gives `"result":"43"`.
3. `(some? (find-ns 'test-project))` gives `true`: the activation script loaded the test project's library.
4. Clean up with `(in-ns 'user) (remove-ns 'joyride-tester.nrepl)`.

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
3. `(flare/post-message!+ :joyride-test {:hello "flare"})` with `awaitResult: true` gives `"#<Promise true>"`. Then `(test-project/wait-for!+ #(not-empty @!msgs) 2000)` resolves to `[{:echo {:hello "flare"}}]`.
4. `(flare/flare!+ {:html [:p "sidebar"] :key :sidebar-2 :title "Sidebar test"})` opens a sidebar Flare, and its key is in `(flare/ls)`.
5. `(flare/close! :joyride-test)` and `(flare/close! :sidebar-2)` each give `true`, and the keys are gone from `(flare/ls)`.

## Sidebar Flare messages

Through `joyride_evaluate_code`, with `(require '[joyride.flare :as flare])` first. The page in a sidebar Flare and Joyride talk both ways: the page says it is ready, the message handler answers it, and then Joyride starts a round trip of its own.

1. Open a Flare in sidebar slot 3, with a handler that keeps every message and answers `ready` with a `welcome`:
   ```clojure
   (def !sidebar-msgs (atom []))
   (flare/flare!+
    {:html [:div [:h3 "Sidebar messages"]
            [:script "const vscode = acquireVsCodeApi();
                      let received = 0;
                      window.addEventListener('message', e => {
                        const m = e.data;
                        received++;
                        if (m.type === 'welcome') vscode.postMessage({type: 'welcomed', text: m.text, received});
                        if (m.type === 'ping') vscode.postMessage({type: 'pong', n: m.n + 1, received});
                      });
                      vscode.postMessage({type: 'ready'});"]]
     :title "Sidebar messages"
     :key :sidebar-3
     :message-handler (fn [m]
                        (let [msg (js->clj m :keywordize-keys true)]
                          (swap! !sidebar-msgs conj msg)
                          (when (= "ready" (:type msg))
                            (flare/post-message!+ :sidebar-3 {:type "welcome" :text "hello sidebar"}))))})
   ```
   With `awaitResult: true` it resolves to a map with the key `:sidebar-3`.
2. `(test-project/wait-for!+ #(when (= 2 (count @!sidebar-msgs)) @!sidebar-msgs) 3000)` resolves to `[{:type "ready"} {:type "welcomed", :text "hello sidebar", :received 1}]`: the page's `ready` reached the handler, the handler's `welcome` reached the page, and the page's answer came back.
3. `(flare/post-message!+ :sidebar-3 {:type "ping" :n 41})` with `awaitResult: true` gives `"#<Promise true>"`. Then `(test-project/wait-for!+ #(when (= 3 (count @!sidebar-msgs)) (last @!sidebar-msgs)) 3000)` resolves to `{:type "pong", :n 42, :received 2}`.
4. `(flare/close! :sidebar-3)` gives `true`, and `:sidebar-3` is gone from `(flare/ls)`.

## A Scittle REPL in a Flare

`clojure_evaluate_code` with `replSessionKey` `"tic-tac-toe"`, in `user`. The game is `tic-tac-toe/replicant_tictactoe/` in the test project, and its state is the atom `replicant-tictactoe.core/!store`.

1. `(some? (js/document.querySelector "#app"))` gives `true`: the evaluation runs in the Flare's page.
2. `(replicant-tictactoe.core/event-handler! {} [:tic 1 1])`, then `(:tics @replicant-tictactoe.core/!store)`, gives `{[1 1] :x}`. A second move, `[:tic 0 0]`, is an `:o`.
3. Cover the Flare. Through `joyride_evaluate_code`, with `(require '[joyride.flare :as flare])` first, open `(flare/flare!+ {:html [:p "cover"] :title "Cover test" :key :joyride-test-cover})` with `awaitResult: true`, then `(flare/close! :joyride-test-cover)`. The game is still `{[1 1] :x, [0 0] :o}`: the page kept its state while it was covered.
4. Through `joyride_evaluate_code`, with `awaitResult: true`, `(test-project/reload-tic-tac-toe!+)` gives `"#<Promise :reloaded>"`. It closes the Flare, opens it again, and resolves when the session answers from the new page. `(:tics @replicant-tictactoe.core/!store)` is now `nil`, since the new page started a new game.
5. End with `(replicant-tictactoe.core/event-handler! {} [:reset])`.

## Workspace scripts

1. Create `.joyride/scripts/joyride_tester_script.cljs` in the test project, with `(ns joyride-tester-script)` and `(+ 40 2)`.
2. Through `joyride_evaluate_code`, with `awaitResult: true`: `(vscode/commands.executeCommand "joyride.runWorkspaceScript" "joyride_tester_script.cljs")` gives `"#<Promise 42>"`.
3. Delete the script, and clean up with `(in-ns 'user) (remove-ns 'joyride-tester-script)`.

## Bundled skills

Joyride's MCP server publishes its skills. Clients find and load them in different ways, so this section doesn't prescribe one.

1. The skills `joyride`, `joyride-user-scripting`, `joyride-workspace-scripting` and `joyride-update-llm-contexts` can be found through the surface your client has for this server's skills or resources.
2. Each one loads with its frontmatter and body intact.

Note which surface you used. The test passes when the skills are found and load without a workaround.

## Delegation to `joyride-tester`

The `joyride-tester` agent loads this skill, runs one test task, and reports. After **Before you start**, every section can run in its own tester agent, in parallel. Pass each agent its section, the session keys you found, and its own `who`. In a parallel run, `otherWhosSinceLast` in MCP step 6 also lists the other agents.

## Reporting

Report only the sections that ran. For each one: passed, failed, or something unexpected, with the tool responses as evidence. For a failure, what was expected, what came back, and the code that was evaluated. End with what was cleaned up.
