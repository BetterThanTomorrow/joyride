# Chat Instructions & Prompt Files Research

Research findings on VS Code's chat instructions and prompt file capabilities.

## Date
October 13, 2025

## Research Question
Can Joyride enable users to dynamically contribute chat instructions and prompt files at runtime?

## Status
🔍 Deferred - VS Code has built-in discovery mechanisms

## VS Code Documentation
- [VS Code 1.105 Release Notes: Prompt and Instructions File Contributions](https://code.visualstudio.com/updates/v1_105#_prompt-and-instructions-file-contributions)
- [VS Code API Reference](https://code.visualstudio.com/api/references/vscode-api)
- [Customizing Chat in VS Code](https://code.visualstudio.com/docs/copilot/customization/overview)
- [GitHub Copilot Documentation](https://docs.github.com/en/copilot)
- [Contribution Points Reference](https://code.visualstudio.com/api/references/contribution-points) (Note: `chatInstructions` and `chatPromptFiles` not yet documented there)

## Initial Findings

- Declared in `package.json` under `contributes.chatInstructions` and `contributes.chatPromptFiles`
- No dynamic registration API found in `vscode.chat` or `vscode.lm` namespaces
- Can mutate package.json array in memory, but has no effect on VS Code behavior
- Instructions/prompts use file paths (e.g., `"./assets/llm-contexts/agent-joyride-eval.md"`)
- Paths are relative to extension root directory

### VS Code Discovery Mechanisms

VS Code already provides mechanisms for discovering instructions and prompts in the filesystem:
- **Nested AGENTS.md files**: Support for workspace-specific instructions
- **Prompt file suggestions**: VS Code can suggest prompt files from the workspace
- **.github/copilot-instructions.md**: Repository-level instructions

Joyriders can write instruction and prompt files to these locations without Joyride involvement.

## Potential Future Work

Helper functions similar to `joyride.flare` namespace could assist with:
- Creating instruction/prompt files in conventional locations
- Managing multiple instruction files
- Validating instruction file format
- Listing available instructions

**Decision**: Hold off on implementation for now. Focus on tool contributions where Joyride adds clear value.

## Critical Questions (Unexplored)

1. **Path Resolution**: Can instructions reference absolute paths or only extension-relative paths?
   - Extension-relative: `"./assets/instructions/foo.md"` ✅ (confirmed working)
   - Absolute paths: `"/Users/.../.config/joyride/instructions/foo.md"` ❓
   - User-relative: `"~/.config/joyride/instructions/foo.md"` ❓

2. **File Watching**: Does VS Code reload instructions when file contents change?
   - Needs testing: Modify instruction file and check if AI sees updated content

3. **Dynamic Content**: Can we use placeholder paths that are populated by users?

## Potential Implementation Approaches (Not Pursued)

### Option A: Symbolic Path References
Pre-declare instruction slots with paths that can be user-populated:

```json
{
  "chatInstructions": [
    {
      "name": "JoyrideUserInstruction1",
      "description": "User custom instruction 1",
      "path": "${userHome}/.config/joyride/lm-instructions/user-instruction-1.md"
    }
  ]
}
```

**Pros**: Truly dynamic - users just write files
**Cons**: May not support variable interpolation in paths

### Option B: Extension-Managed File Proxy
Create proxy files in extension space that load user content.

**Pros**: Guaranteed to work with VS Code's path resolution
**Cons**: More complex, requires file watching and copying

### Option C: Slot Reservation with Static Paths
Pre-create instruction files in extension space, allow users to populate via API:

```clojure
(joyride.lm/set-user-instruction!
  1
  "# My Custom Instruction\n\nContent here...")
```

**Pros**: Simple, guaranteed to work
**Cons**: Less flexible, requires extension involvement

## References

### Related Research
- Language Model tool contributions: [`dev/docs/lm-contributions-research.md`](./lm-contributions-research.md)
