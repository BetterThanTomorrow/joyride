# LM Tool: Async Parameter Usage Issues

## Problem Identified

### Agents Overuse `waitForFinalPromise: true`
**Root Cause**: Poor parameter guidance leads agents to default to "safe" waiting behavior.
- Agents use `true` even for obviously synchronous code
- Performance impact: unnecessary promise handling overhead
- User experience: slower responses for simple evaluations

## Current Configuration

From `package.json` (lines 193-196):
```json
"waitForFinalPromise": {
  "type": "boolean",
  "description": "Wether to wait for promises if the evaluation results in a promise. Defaults to `false`. When you specifically want interact with the user, or otherwise need the resolved promise value."
}
```

**Issues with Current Description**:
- Typo: "Wether" should be "Whether"
- Vague: "interact with the user" is unclear
- Incomplete: doesn't explain performance implications
- No guidance distinguishing sync vs async code scenarios

## Solution Strategy

### Phase 1: Improve Agent Guidance (Low Risk, High Impact)

#### A. Rename Parameter
- **From**: `waitForFinalPromise`
- **To**: `awaitResult`
- **Why**: More intuitive, signals purpose clearly
- **Cost**: Potential breakage in existing prompts/instructions that reference old name
- **Mitigation**: Include note in description about previous parameter name
  - e.g., "(formerly waitForFinalPromise)" in the description

#### B. Rewrite Parameter Description
**Current** (problematic):
```
"Wether to wait for promises if the evaluation results in a promise. Defaults to `false`. When you specifically want interact with the user, or otherwise need the resolved promise value."
```

**Proposed**:
```
"Whether to await async results before returning. Defaults to `false` for optimal performance.

Use `true` ONLY when you need the resolved value for your next action:
- Awaiting user input from dialogs (showInputBox, showQuickPick, etc.)
- Waiting for async computations (file operations, API calls, etc.)

Use `false` (default) for:
- Synchronous code (most cases)
- Fire-and-forget async operations
- When you don't need the resolved result"
```

### Phase 2: Smart Auto-Detection (Medium Risk, High Impact)

#### Auto-Override Logic
```clojure
;; Evaluate code first, then decide based on actual result
(let [result (evaluate-code)]
  (if (and (instance? js/Promise result) await-result?)
    ;; Only wait for actual promises when requested
    (await-promise result)
    ;; Return immediately for sync results or unwanted promises
    (return-sync result)))
```

**Benefits**:
- Performance protection: sync code always fast regardless of agent choice
- Agent forgiveness: wrong `awaitResult` choices auto-corrected
- Maintains explicit control for legitimate async cases

### Phase 3: Consider Architectural Changes (High Risk, High Impact)

#### Option A: Separate Tools
- `joyride_run_code` (sync + fire-and-forget)
- `joyride_await_code` (when result needed)
- **Pro**: Forces intentional choice, eliminates parameter confusion
- **Con**: More complex API surface

#### Option B: Specialized User Interaction Tool
- Dedicated tool for VS Code dialogs/prompts
- **Pro**: Optimized for user interaction patterns
- **Con**: May be overengineering

## Recommended Implementation Plan

### Immediate Actions
- [x] **Rename parameter**: `waitForFinalPromise` → `awaitResult`
- [x] **Update tool description** with sync/async guidance
- [ ] **Craft stellar agent guidance** - collaborative session to design clear descriptions
  - Review and improve `code` parameter description for clarity
  - Draft improved `awaitResult` parameter description with precise usage scenarios  
  - Review `namespace` parameter description for completeness
  - Ensure all descriptions work together cohesively to guide proper tool usage
  - Test language with example agent interactions
- [ ] **Update package.json** with all improved parameter descriptions

### Next Phase
- [ ] **Implement auto-detection logic** in `execute-code+`
- [ ] **Monitor usage patterns** to validate improvements
- [ ] **Consider architectural changes** if problems persist

### Success Metrics
- Reduced usage of `awaitResult: true` for sync code
- Faster average response times
- Maintained functionality for legitimate async use cases

## Files to Modify

1. **package.json** (lines 193-196): Update tool parameter spec
2. **src/joyride/lm_tool.cljs**:
   - Rename parameter references
   - Implement auto-detection logic
3. **src/joyride/lm_tool/core.cljs**: Update `extract-input-data` function

## Implementation Notes

- No breaking changes for agents (they discover tools dynamically)
- Maintain backward compatibility during transition
- Consider logging when auto-detection overrides agent choice
- Test with both sync and async code scenarios
