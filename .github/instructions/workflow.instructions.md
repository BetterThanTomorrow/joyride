---
description: 'Workflows for this project'
applyTo: '**'
---

## Joyride Project Issue Workflow

Adopt a structured, REPL-driven approach for Joyride project issues to deliver high-quality, minimal changes with robust test coverage.

### Pre-Implementation Phase
1. **Explore via REPL**: Use the REPL to interactively understand current behavior and clarify the problem.
2. **Establish criteria**: Define data-oriented, focused changes that address the core issue minimally.
3. **Validate approach**: Confirm understanding and plan with the maintainer.
4. **Branch creation**: Use `<issue-number>-descriptive-name` format (e.g., `247-remove-eval-autobalancing`).

### Implementation Phase
5. **Iterate in REPL**: Develop and test solutions interactively before file changes.
6. **Apply minimal edits**: Implement the smallest change using structural editing tools for Clojure files.
7. **Include tests**: Add integration tests in `vscode-test-runner/workspace-1/.joyride/src/integration_test/`.

### PR Preparation Phase
8. **Update CHANGELOG**: Add to `[Unreleased]` section:
   ```markdown
   - Fix: [Issue title](https://github.com/BetterThanTomorrow/joyride/issues/<number>)
   ```
9. **Verify tests**: Run `npm run integration-test` to ensure all pass.
10. **Prepare PR**: Suggest description with checklist:
    - ✓ Read developer docs (CONTRIBUTE.md)
    - ✓ Addresses clear issue
    - ✓ Includes regression tests
    - ✓ Updated CHANGELOG

### Key Principles
- **REPL-first development**: Test solutions interactively to ensure reliability before file modifications.
- **Minimal changes**: Focus on the smallest diff that resolves the issue effectively.
- **Test coverage**: Require tests for every change to prevent regressions.
- **Preserve history**: Avoid force pushes to maintain clear PR review trails.