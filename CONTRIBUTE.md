# Contribute

## Issue

Before writing any code, please create an issue first that describes the problem
you are trying to solve with alternatives that you have considered. A little bit
of prior communication can save a lot of time on coding. Keep the problem as
small as possible. If there are two problems, make two issues. If the problem
statement isn't clear, it's better to start with a
[Discussion](https://github.com/BetterThanTomorrow/joyride/discussions). When we
collectively agree on the problem and solution direction, it's time to move on
to a PR.

## PR

Follow up with a pull request Post a corresponding PR with the smallest change
possible to address the issue. Then we discuss the PR, make changes as needed
and if we reach an agreement, the PR will be merged.

Please do not use `git push --force` on your PR branch for the following reasons:

- It makes it more difficult for others to contribute to your branch if needed.
- It makes it harder to review incremental commits.
- Links (in e.g. e-mails and notifications) go stale and you're confronted with: this code isn't here anymore, when clicking on them.
- Your PR will be squashed anyway.

## Tests

Each bug fix, change or new feature should be tested well to prevent future
regressions.  Tests can be added
[here](https://github.com/BetterThanTomorrow/joyride/tree/master/vscode-test-runner/workspace-1/.joyride/src/integration_test)

## Development

1. Run the build script: `cmd/ctrl+b`
   - This starts the nREPL server
   - wait for shadow-cljs to signal that building is done.
1. Start the extension in debug mode: `F5`
1. Connect the REPL to the nREPL server and the shadow-cljs build `:extension`
