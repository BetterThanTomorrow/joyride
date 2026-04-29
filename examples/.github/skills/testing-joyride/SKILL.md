---
name: testing-joyride
description: 'Test Joyride features in the examples workspace. Covers output terminal verification, who-tracking, REPL log queries, and evaluation result validation. Use when: testing Joyride features.'
---

# Testing Joyride

Structured testing patterns for verifying Joyride features in the examples workspace. This skill is a living document — test areas are added as features are developed and verified.

## Principles

### Consistent `who` slugs

Each test area uses a dedicated `who` slug. When querying the REPL log, filter by the slug you used — entries from other slugs (other agents, the human `ui`, previous test runs) will not appear in filtered results. This is by design, but means a query for the wrong slug returns nothing even when entries exist.

### Verify via the log, not just the tool response

The `joyride_evaluate_code` tool returns `result`, `stdout`, `stderr` directly. But the REPL Output Log (`clojure_repl_output_log`) is the authoritative record of what happened — it captures categorized entries (`evaluatedCode`, `evaluationResults`, `evaluationOutput`, `evaluationErrorOutput`) with who attribution and timestamps. Always cross-check against the log when verifying features.

### Use `awaitResult` deliberately

- `awaitResult: true` for expressions where you need the unwrapped result
- `awaitResult: false` for expressions that throw or produce side effects you want to observe without hanging

## Test Areas

### Output Terminal and Who-Tracking

Verifies that evaluations appear in the Joyride Output terminal with correct `who` badges, and that the REPL log captures all output categories with consistent attribution.

#### Basic evaluation and logging

1. Evaluate a simple expression with a dedicated `who` slug
2. Query the log filtered by that slug
3. Confirm `evaluatedCode` and `evaluationResults` entries exist with correct `who`, `ns`, and `repl-session-key: "joyride"`

#### Stdout capture

1. Evaluate `(do (println "test-stdout") 42)` with `awaitResult: true`
2. Confirm `stdout` field in tool response contains `"test-stdout\n"`
3. Query log — confirm `evaluationOutput` entry with the stdout text and correct `who`

#### Stderr capture

1. Evaluate with `awaitResult: false`:
   ```clojure
   (do
     (println "stdout-line")
     (binding [*print-fn* *print-err-fn*]
       (println "stderr-line"))
     (throw (ex-info "thrown-error" {:test true})))
   ```
2. Confirm tool response has `stdout: "stdout-line\n"` and `stderr: "stderr-line\n"`
3. Query log — confirm entries for all three: `evaluationOutput` (stdout), `evaluationErrorOutput` (stderr), `evaluationErrorOutput` (thrown error), all with same `who`

#### Who-tracking across evaluators

1. Evaluate as slug A
2. Evaluate as slug B
3. Evaluate as slug A again
4. Confirm third response includes `otherWhosSinceLast: ["B"]`
5. Evaluate as slug A once more — confirm no `otherWhosSinceLast` (no intervening evaluator)

#### Human UI evaluation tracking

1. Evaluate as your slug
2. Ask the human to evaluate something manually
3. Evaluate as your slug again
4. Confirm `otherWhosSinceLast: ["ui"]`

#### Who consistency (comprehensive)

Use a single dedicated slug for an expression that produces stdout, stderr, and a thrown error simultaneously. Query the log and confirm every entry carries the same `who` value. This catches attribution bugs where output streams lose their evaluator identity.

### Without Calva Connected

All tests above should also pass when Calva is not connected to the REPL. The Joyride REPL and Backseat Driver log operate independently of Calva's nREPL connection.
