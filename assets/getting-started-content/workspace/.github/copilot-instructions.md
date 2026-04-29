---
description: 'Joyride Workspace project — scripts and source files for this specific workspace'
applyTo: '**/.joyride/**'
---

# Joyride Workspace Project

This is the Joyride Workspace project at `.joyride/` in this workspace.
Scripts here are specific to this workspace and can be shared with the
team via version control.

The Joyride extension bundles skills with comprehensive API documentation
and pattern guidance. These instructions focus on this project's specific
content.

## Project Inventory

- `scripts/workspace_activate.cljs` — Workspace activation script
- `scripts/hello_joyride_workspace_script.cljs` — Example greeting script

## Development

Use the REPL (`joyride_evaluate_code`) as your primary tool. Develop
incrementally, evaluate subexpressions, only update files when asked.
Prefer structural editing tools when editing Clojure files.

As new scripts and source files are added to this project, update the
inventory above.
