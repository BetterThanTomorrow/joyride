# Language Model Tool Contributions Research

Research findings on VS Code's Language Model (LM) tool contribution capabilities.

## Date
October 13, 2025

## Research Question
Can Joyride enable users to dynamically contribute Language Model tools at runtime?

## Answer
**No** - not in a useful way. While technically feasible using a slot reservation pattern, the constraint that metadata (displayName, modelDescription) must be static makes this approach impractical. Generic slot names like "User Tool 1" would not provide meaningful information to users or AI agents about what each tool does.

## Findings

### Language Model Tools

**Status**: ❌ Not viable for dynamic user contributions

#### Architecture
VS Code's LM tools require **both** components:

1. **Static Declaration** (`package.json`)
   - Provides metadata: name, displayName, modelDescription, inputSchema, when clause, icon, etc.
   - Read once at extension activation
   - **Cannot be modified at runtime** - property descriptors are read-only getters with no setters and `configurable: false`

2. **Dynamic Registration** (`vscode.lm.registerTool()`)
   - Provides implementation: `prepareInvocation` and `invoke` functions
   - Can be registered/unregistered at runtime
   - Returns disposable for lifecycle management

#### Critical Limitation

**Tool metadata is immutable at runtime.** REPL investigation revealed:

```clojure
;; Tool objects have read-only property descriptors
(js/Object.getOwnPropertyDescriptor tool-obj "description")
;; => {:get #function, :set nil, :enumerable true, :configurable false}
```

This means:
- displayName and modelDescription cannot be changed after extension activation
- Generic slot names like "User Tool 1" would be the only option
- AI agents and users wouldn't know what each tool does without external documentation
- This defeats the purpose of self-describing tool metadata

#### REPL Verification

```clojure
;; Dynamic registration works
(def tool-disposable
  (vscode/lm.registerTool
   "joyride_evaluate_code"
   #js {:prepareInvocation (fn [options token] ...)
        :invoke (fn [options token] ...)}))

;; Tool appears in vscode.lm.tools
(filter #(= "joyride_evaluate_code" (.-name %)) vscode/lm.tools)
;; => matches found

;; Tool can be invoked
(vscode/lm.invokeTool "joyride_evaluate_code" #js {:input ...} nil)
;; => works successfully

;; Tool registration without package.json declaration
(vscode/lm.registerTool "test_dynamic_tool" #js {...})
;; => returns disposable (appears successful)

(vscode/lm.invokeTool "test_dynamic_tool" #js {:input ...} nil)
;; => ERROR: "Tool test_dynamic_tool was not contributed"
```

**Conclusion**: Tools require package.json declaration for metadata. Runtime registration only provides implementation. **Since metadata cannot be updated dynamically, there is no practical way to enable users to contribute meaningful custom tools.**

---

## Why The Slot Pattern Doesn't Work

While technically possible to pre-declare tool slots with generic names, this approach is not useful:

### The Problem

```json
{
  "name": "joyride_user_tool_1",
  "displayName": "User Tool 1",  // ❌ Generic, unhelpful
  "modelDescription": "Custom user tool slot 1"  // ❌ Doesn't describe actual functionality
}
```

### Why This Fails

1. **AI agents can't understand** what "User Tool 1" does without external context
2. **Users can't remember** which slot contains which tool
3. **Tool metadata** should be self-describing - the whole point of displayName and modelDescription
4. **Package.json reloading** required anyway if users want meaningful names (defeats "dynamic" goal)

### Attempted Workarounds

We investigated several approaches to mutate metadata at runtime:

#### 1. Mutate package.json in Memory
```clojure
(set! (.-displayName (first (.-languageModelTools ...))) "New Name")
;; ✅ Mutation succeeds in memory
;; ❌ VS Code has already cached metadata, doesn't re-read
```

#### 2. Mutate Tool Objects Directly
```clojure
(set! (.-description tool-obj) "New Description")
;; ❌ Property is read-only getter, mutation has no effect
```

#### 3. Replace Property Descriptor
```clojure
(js/Object.defineProperty tool-obj "description" ...)
;; ❌ Property is not configurable, cannot be redefined
```

**Result**: VS Code has intentionally sealed tool metadata at the property descriptor level to prevent runtime modifications.

### Implementation Pattern

Similar to Flare sidebar slots:

1. Pre-declare N tool slots in `package.json`
2. Each slot has `when` context for conditional visibility
3. Users call API to register implementation: `(joyride.lm/register-user-tool! slot-number tool-impl)`
4. Implementation manages both:
   - `vscode.lm.registerTool()` call
   - When context activation

### Example Slot Declaration

```json
{
  "languageModelTools": [
    {
      "name": "joyride_user_tool_1",
      "when": "joyride.lm.userTool1.isActive",
      "displayName": "User Tool 1",
      "modelDescription": "Custom tool slot 1",
      "inputSchema": {
        "type": "object",
        "properties": {}
      }
    }
  ]
}
```

### User API Pattern

```clojure
(require '[joyride.lm :as lm])

;; Register a custom tool in slot 1
(lm/register-user-tool!
  1  ; slot number
  {:prepare-invocation (fn [options token] ...)
   :invoke (fn [options token] ...)})

;; Dispose tool from slot 1
(lm/dispose-user-tool! 1)
```

---

## Conclusion

**Dynamic user contribution of Language Model tools is not viable** with the current VS Code API. While the technical mechanism exists (static declaration + dynamic registration), the inability to provide meaningful, dynamic metadata makes this approach impractical for end users.

### What Would Be Needed

For this feature to work well, VS Code would need to either:

1. **Allow runtime metadata updates** - Enable extensions to modify displayName, modelDescription, and inputSchema after registration
2. **Support fully dynamic tools** - Accept all metadata at registration time without requiring package.json declarations
3. **Provide metadata callbacks** - Allow extensions to compute metadata dynamically when tools are queried

Until VS Code provides one of these capabilities, users who want custom tools will need to:
- Create their own VS Code extensions with proper package.json declarations
- Or accept that Joyride can only provide pre-defined, statically-named tools

### Related Issue

See [BetterThanTomorrow/joyride#249](https://github.com/BetterThanTomorrow/joyride/issues/249) - "Contribute Copilot tools dynamically"

---

## Implementation Strategy

~~Follow the same proven pattern as Flare sidebars~~

**Status**: Not recommended for implementation due to metadata limitations described above.

1. **Pre-declare tool slots** in `package.json` with metadata
2. **Use when contexts** to control slot visibility
3. **Provide Clojure API** for users to register tool implementations
4. **Manage lifecycle** through disposables and when-context updates

### Proposed Configuration

Reserve 5 user tool slots (configurable number):

```json
{
  "languageModelTools": [
    {
      "name": "joyride_user_tool_1",
      "when": "joyride.lm.userTool1.isActive",
      "displayName": "User Tool 1",
      "modelDescription": "Custom user-contributed tool slot 1. Use this tool when instructed by user-provided instructions.",
      "userDescription": "Custom tool slot 1",
      "canBeReferencedInPrompt": true,
      "toolReferenceName": "user-tool-1",
      "icon": "$(tools)",
      "inputSchema": {
        "type": "object",
        "properties": {},
        "additionalProperties": true
      }
    }
    // ... slots 2-5
  ]
}
```

### User API Design

```clojure
(ns user
  (:require [joyride.lm.user-tools :as user-tools]))

;; Register a custom tool
(user-tools/register!
  {:slot 1
   :display-name "My Custom Tool"
   :description "Does something useful"
   :input-schema {:type "object"
                  :properties {:text {:type "string"}}}
   :prepare-invocation (fn [options token]
                         {:invocation-message "Running my tool..."})
   :invoke (fn [options token]
             (let [input (js->clj (.-input options) :keywordize-keys true)]
               ;; Process input, return LanguageModelToolResult
               ...))})

;; Check tool status
(user-tools/status 1)
;; => {:slot 1 :active? true :display-name "My Custom Tool"}

;; Dispose tool
(user-tools/dispose! 1)

;; List all user tools
(user-tools/list-all)
;; => [{:slot 1 :active? true ...} {:slot 2 :active? false ...}]
```

### Implementation Components

**New namespace**: `src/joyride/lm/user_tools.cljs`

Core functions:
- `register!` - Register tool implementation in a slot
- `dispose!` - Remove tool from slot
- `status` - Get slot status
- `list-all` - List all slots and their status

Internal functions:
- `ensure-slot-valid!` - Validate slot number
- `register-tool-impl!` - Call vscode.lm.registerTool
- `update-when-context!` - Manage when context for slot
- `store-tool-metadata!` - Track tool state in app-db

### State Management

Store tool registrations in `joyride.db/!app-db`:

```clojure
{:user-tools {1 {:slot 1
                 :active? true
                 :display-name "My Tool"
                 :disposable <disposable-object>}
              2 {:slot 2
                 :active? false}
              ;; ... slots 3-5
              }}
```

### When Contexts

Add to `joyride.when-contexts`:
- `joyride.lm.userTool1.isActive`
- `joyride.lm.userTool2.isActive`
- `joyride.lm.userTool3.isActive`
- `joyride.lm.userTool4.isActive`
- `joyride.lm.userTool5.isActive`

---

## Next Steps

1. Define implementation quality criteria (10/10 rating system)
2. Iterate on API design in REPL
3. Validate approach with maintainer
4. Document implementation plan
5. Implement using structural editing tools
6. Add integration tests
7. Update CHANGELOG

---

## Key Insights

1. **No purely dynamic contributions** - All LM contributions require package.json declarations
2. **Runtime registration supplements, not replaces** - Dynamic APIs provide implementation, not metadata
3. **Slot reservation is the pattern** - Similar to Flare sidebars, pre-declare slots and dynamically populate
4. **When contexts enable conditional visibility** - Can hide/show slots based on user configuration

## Related Prior Art

- **Flare Sidebars**: Pre-declared webview slots with when-context activation
- **MCP Servers**: Dynamic server registration but static capability declaration
- **VS Code Commands**: Static declaration with dynamic implementation registration

## References

### VS Code Documentation
- [VS Code 1.105 Release Notes: Language Model Tools](https://code.visualstudio.com/updates/v1_105#_language-model-tools)
- [VS Code API Reference](https://code.visualstudio.com/api/references/vscode-api)
- [VS Code Language Model API](https://code.visualstudio.com/api/references/vscode-api#lm)
- [VS Code Extension API - Language Model Tools](https://code.visualstudio.com/api/extension-guides/language-model-tools)
- [Contribution Points Reference](https://code.visualstudio.com/api/references/contribution-points) (Note: `languageModelTools` not yet documented there)

### Joyride Implementation
- Existing tool implementation: `src/joyride/lm/evaluation.cljs`
- Flare sidebar pattern (prior art): `src/joyride/flare/sidebar.cljs`
- When contexts management: `src/joyride/when_contexts.cljs`

### Related Research
- Chat instructions and prompts: [`dev/docs/lm-instructions-prompts-research.md`](./lm-instructions-prompts-research.md)
