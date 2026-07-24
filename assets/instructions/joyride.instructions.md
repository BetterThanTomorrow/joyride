---
description: >-
  Joyride — ClojureScript scripting for VS Code. Read MCP resource
  skill://joyride/SKILL.md when using Joyride tools. Read
  skill://joyride-user-scripting/SKILL.md for User scope
  (~/.config/joyride/). Read skill://joyride-workspace-scripting/SKILL.md
  for Workspace scope (.joyride/).
---

# Joyride — VS Code Scripting with ClojureScript

Joyride makes VS Code hackable in user space.

## Load the Skills

- Always read MCP resource `skill://joyride/SKILL.md` when using Joyride
  tools or working with Joyride scripts.
- Read MCP resource `skill://joyride-user-scripting/SKILL.md` when working
  with User scope (`~/.config/joyride/`).
- Read MCP resource `skill://joyride-workspace-scripting/SKILL.md` when
  working with Workspace scope (`<workspace>/.joyride/`).

## LLM Context Migration

The user's existing user config Joyride project may need to be updated with the latest llm context / instructsions files.

If `~/.config/joyride/.github/llm-contexts-0.0.73.txt` does not exist, read
MCP resource `skill://joyride-update-llm-contexts/SKILL.md` and offer to
modernize the user's copilot-instructions.md.
