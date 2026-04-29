---
description: >-
  Joyride — ClojureScript scripting for VS Code. Load the joyride skill
  when using Joyride tools. Load joyride-user-scripting for User scope
  (~/.config/joyride/). Load joyride-workspace-scripting for Workspace
  scope (.joyride/).
---

# Joyride — VS Code Scripting with ClojureScript

Joyride makes VS Code hackable in user space.

## Load the Skills

- Always load the `joyride` skill when using Joyride tools or working
  with Joyride scripts.
- Load `joyride-user-scripting` when working with User scope
  (`~/.config/joyride/`).
- Load `joyride-workspace-scripting` when working with Workspace scope
  (`<workspace>/.joyride/`).

## LLM Context Migration

The user's existing user config Joyride project may need to be updated with the latest llm context / instructsions files.

If `~/.config/joyride/.github/llm-contexts-0.0.73.txt` does not exist, load
the `joyride-update-llm-contexts` skill and offer to modernize the
user's copilot-instructions.md.
