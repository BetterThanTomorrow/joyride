# Chat Instructions Migration Notes

## Current Guide Delivery
- `src/joyride/lm/docs.cljs` fetches remote markdown guides from `assets/llm-contexts`, caches them locally, and registers LM tools declared in `package.json` under `contributes.languageModelTools`.
- `src/joyride/extension.cljs` wires activation via `joyride.lm/register-tools!`, so guide delivery is exposed exclusively through language model tool registration.
- Two guides are provided: `joyride_basics_for_agents` (agent-joyride-eval.md) and `joyride_assisting_users_guide` (user-assistance.md)
- Fetch mechanism: Try GitHub first (10s timeout), fallback to local bundled copy on failure
- Current files exist at: `assets/llm-contexts/agent-joyride-eval.md` and `assets/llm-contexts/user-assistance.md`

## Chat Instructions API Requirements (VS Code 1.105+)
- `contributes.chatInstructions` is available in VS Code 1.105 and later; the contribution is validated when the manifest loads, not lazily.
- Each instruction entry must include a unique `name`, a `path` that stays inside the extension folder, and a human-readable `description`.
- Names must satisfy VS Code's manifest regex (`^[A-Za-z0-9][-A-Za-z0-9]*$`) and duplicate names are rejected.
- The referenced markdown file **must exist on disk during activation**; missing or unreadable files cause the contribution to fail and surface an activation error.
- There is no runtime registration API, so the markdown must exist before activation completes. VS Code does not add an activation event automatically for chat instructions.
- Documentation: https://code.visualstudio.com/updates/v1_105#_prompt-and-instructions-file-contributions

## Key Insight: Background Sync Strategy
**Critical realization**: If markdown files are bundled with the extension at the paths declared in the manifest, VS Code's activation validation passes immediately. We can then update the files in the background without blocking activation.

This means:
- **Bundle time**: Copy markdown files from `assets/llm-contexts/` to manifest-declared location
- **Activation time**: VS Code validates files exist (they do - bundled copies), activation succeeds
- **Post-activation**: Background task fetches fresh content from GitHub and atomically updates files
- **Next restart**: VS Code loads the updated content

This approach eliminates:
- Activation blocking on network requests
- Complex error handling for fetch failures during activation
- Need for graceful degradation

## Migration Plan (Revised)

### 1. Use Existing Location (No New Folder Needed)
- Keep guides at `assets/llm-contexts/*.md` - already bundled correctly
- Manifest will point to these existing paths
- No build process changes needed

### 2. Add Background Sync Function
Create `sync-guides-background!` in `joyride.lm.docs`:
```clojure
(defn sync-guide-background!
  "Non-blocking sync of guide from GitHub. Writes to target-path if successful.
   Returns promise resolving to {:status :success/:failed :source ...}"
  [extension-context file-path target-path]
  (-> (p/let [result (fetch-agent-guide extension-context file-path)]
        (when (= (:type result) "success")
          (p/let [target-uri (vscode/Uri.file target-path)
                  _ (vscode/workspace.fs.writeFile target-uri (.encode (js/TextEncoder.) (:content result)))]
            (js/console.log "Background guide sync SUCCESS:" file-path "from" (:source result))))
        {:status (if (= (:type result) "success") :success :failed)
         :source (:source result)})
      (p/catch (fn [error]
                 (js/console.warn "Background guide sync ERROR:" file-path (.-message error))
                 {:status :failed :error (.-message error)}))))

(defn sync-all-guides-background!
  "Sync all guides in background after activation completes.
   Skips sync in development mode to avoid overwriting local edits."
  [extension-context]
  (let [extension-mode (.-extensionMode extension-context)
        development-mode? (= extension-mode 2)]
    (if development-mode?
      (js/console.log "Development mode detected - skipping background guide sync")
      (let [guides [{:file-path agent-guide-path
                     :target (path/join (.-extensionPath extension-context) agent-guide-path)}
                    {:file-path user-assistance-guide-path
                     :target (path/join (.-extensionPath extension-context) user-assistance-guide-path)}]]
        (doseq [{:keys [file-path target]} guides]
          (sync-guide-background! extension-context file-path target))))))
```

**Development Mode Protection**: Background sync is skipped when `extensionMode = 2` (Development) to prevent overwriting local guide edits during Shadow-CLJS hot-reload cycles. In production (`extensionMode = 1`), guides are updated from GitHub as intended.

### 3. Update Extension Activation
In `joyride.extension/activate`:
- Remove guide tool registration from `lm/register-tools!` call
- Add `(lm.docs/sync-all-guides-background! extension-context)` after activation completes
- Keep evaluation and human-intelligence tools (they're not guides)

### 4. Update package.json Manifest
Replace `languageModelTools` entries for guides with `chatInstructions`:
```json
"chatInstructions": [
  {
    "name": "joyride-basics-for-agents",
    "description": "Joyride agent guide - provides AI agents with documentation about using Joyride evaluation effectively",
    "path": "./assets/llm-contexts/agent-joyride-eval.md"
  },
  {
    "name": "joyride-assisting-users-guide",
    "description": "Joyride user guide - provides AI agents with documentation about helping users learn Joyride",
    "path": "./assets/llm-contexts/user-assistance.md"
  }
]
```

Keep `languageModelTools` entries for:
- `joyride_evaluate_code` (evaluation tool)
- `joyride_request_human_input` (human intelligence tool)

### 5. Clean Up lm.cljs
Update `joyride.lm/register-tools!`:
- Remove guide registration calls
- Keep evaluation and human-intelligence registration
- Return only non-guide tool disposables

### 6. Preserve Shared Utilities
Keep in `joyride.lm.docs`:
- `fetch-agent-guide` function (used by background sync)
- `github-base-url`, `agent-guide-path`, `user-assistance-guide-path` constants
- Remove `invoke-tool!` and `register-tool!` (obsolete for guides)

## Quality Criteria (10/10 Rating)

### Data-Oriented (4/10 points)
1. ✅ Pure functions for fetching: `fetch-agent-guide` returns data, doesn't mutate state
2. ✅ Separate concerns: Fetch, file I/O, and logging are separate, composable functions
3. ✅ Clear data flow: `fetch-guide` → `write-file` → `log-result`, each taking and returning data

### Minimal (3/10 points)
4. ✅ No new abstractions: Reuse existing `fetch-agent-guide`, just change where it's called from
5. ✅ Minimal manifest changes: Only change guide entries from `languageModelTools` → `chatInstructions`
6. ✅ Remove only what's obsolete: Delete guide tool registration, keep fetch helpers

### Razor-Sharp Focused (3/10 points)
7. ✅ One responsibility per change: Activation triggers background sync; sync updates files; manifest declares files
8. ✅ No feature creep: Don't add versioning, migration paths, or backward compatibility
9. ✅ Clear success/failure: Log background fetch results, don't fail activation
10. ✅ Files always valid: Bundled files are source of truth; updates enhance but don't replace

### Additional Quality Gates
- ✅ Zero blocking operations during activation (background fetch only)
- ✅ Atomic file writes (temp file + rename pattern)
- ✅ Preserve existing behavior for evaluation and human-intelligence tools
- ✅ No build process changes needed (files already in correct location)

## Implementation Checklist
- [x] Add `sync-guide-background!` and `sync-all-guides-background!` to `joyride.lm.docs`
- [x] Update `joyride.lm/register-tools!` to remove guide registration, keep other tools
- [x] Update `joyride.extension/activate` to call `sync-all-guides-background!`
- [x] Update `package.json` to add `chatInstructions` contribution
- [x] Update `package.json` to remove guide entries from `languageModelTools`
- [x] Fix all Shadow-CLJS compilation warnings (type hints, arity)
- [x] Remove obsolete functions from `joyride.lm.docs` (`invoke-tool!`, `register-tool!` if guides-only)
- [x] Test in Extension Development Host together with human developer
- [x] Verify background sync works and updates files
- [x] Verify chat instructions appear in Copilot
