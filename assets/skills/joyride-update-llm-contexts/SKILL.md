---
name: joyride-update-llm-contexts
description: >-
  One-time migration: modernize a Joyride project's copilot-instructions.md
  to use bundled skills instead of duplicated API docs or fetch URLs.
  Use when: the instructions file tells you to check for migration, or
  the user asks about updating their Joyride AI context.
---

# Modernize Joyride LLM Context

One-time migration skill. The Joyride extension now bundles skills with
comprehensive API documentation and pattern guidance. Older installations
may have a `copilot-instructions.md` that duplicates this content or
fetches it from GitHub URLs.

## Step 0 — Check if migration is needed

Look for `~/.config/joyride/.github/llm-contexts-0.0.73.txt`. If this file
exists, migration has already been done — inform the user and stop.

If the file does not exist, proceed with the migration.

## Step 1 — Assess

Read `~/.config/joyride/.github/copilot-instructions.md` and classify:

| Situation | Markers |
|-----------|---------|
| **Old template** | Contains `fetch_webpage` URLs pointing to `llm-contexts/`, title "Joyride User Scripts Project Assistant", duplicated API docs |
| **Already modern** | Short file (~25 lines), references bundled skills, has "Project Inventory" section |
| **Custom content** | User-written content with no old template markers |
| **Mixed** | Old template sections plus user-added custom sections |
| **Missing** | File does not exist |

If the file is **already modern** or **missing**, skip to Step 4.

## Step 2 — Read the target format

The modern copilot-instructions.md is a short project inventory (~25
lines) that defers all API documentation to the extension's bundled
skills. No fetch URLs, no duplicated docs. The format:

```markdown
---
description: 'Joyride User project — scripts and source files available across all VS Code windows'
applyTo: '**'
---

# Joyride User Project

This is your Joyride User project at `~/.config/joyride/`. Scripts and
source files here are available globally across all VS Code windows.

The Joyride extension bundles skills with comprehensive API documentation
and pattern guidance. These instructions focus on this project's specific
content.

## Project Inventory

- `scripts/user_activate.cljs` — Activation script, manages disposables
- (list actual files found in scripts/ and src/)

## Development

Use the REPL (`joyride_evaluate_code`) as your primary tool. Develop
incrementally, evaluate subexpressions, only update files when asked.
Prefer structural editing tools when editing Clojure files.

As new scripts and source files are added to this project, update the
inventory above.
```

## Step 3 — Plan

Build a migration plan based on the assessment:

- **Old template only**: Replace entirely with the modern format. Scan
  the project for scripts and source files to populate the inventory.
- **Custom content only**: Prepend the modern header (frontmatter +
  intro + inventory), preserve all custom content below it.
- **Mixed**: Remove old template sections (fetch URLs, duplicated API
  docs, "Essential Information Sources" section, etc.). Keep user-added
  sections. Add the modern header if missing.

Present the plan to the user showing:
- What will be removed (with brief excerpts)
- What will be preserved
- What will be added

**Wait for explicit user approval before making any changes.**

## Step 4 — Complete

Apply the approved changes (if any), then create the marker file:
`~/.config/joyride/.github/llm-contexts-0.0.73.txt`

Write a single line: `Joyride LLM contexts updated to version 0.0.73 bundled skills.`

This prevents the migration from being offered again.
