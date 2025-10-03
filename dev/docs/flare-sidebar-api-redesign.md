# Flare Sidebar API Redesign - Multiple Sidebar Slots

## Overview

Redesigned the flare sidebar API from a single `:sidebar?` boolean flag to support 5 independent sidebar slots using special keys `:sidebar-1` through `:sidebar-5`.

## Implementation Strategy

### API Design Evolution
**Old API:**
```clojure
(flare/flare!+ {:html [:h1 "Hello"] :sidebar? true})
```

**New API:**
```clojure
(flare/flare!+ {:html [:h1 "Hello"] :key :sidebar-1})
(flare/flare!+ {:html [:h2 "Another"] :key :sidebar-2})
```

### Key Benefits
- **Multiple concurrent sidebars** - Up to 5 sidebar flares can coexist
- **Explicit slot control** - Users choose exactly which sidebar slot to use
- **Clean UI** - Empty slots don't clutter the sidebar (controlled via `when` contexts)
- **Elegant API** - No new parameters needed, just special key values

### Implementation Components

#### 1. Core API Changes (`flare.cljs`)
- Added `sidebar-keys` set: `#{:sidebar-1 :sidebar-2 :sidebar-3 :sidebar-4 :sidebar-5}`
- New helper functions: `sidebar-key?`, `key->sidebar-slot`
- Updated `normalize-flare-options` to detect sidebar keys and set `:sidebar-slot`
- Modified routing logic in `flare!` to use slot-specific sidebar providers

#### 2. Sidebar Provider Updates (`sidebar-provider.cljs`)
- Changed from single provider to 5 separate providers (`register-flare-providers!`)
- Each provider manages its own slot: `joyride.flare-1` through `joyride.flare-5`
- Updated data structure: `:flare-sidebar-views {slot {:webview-view ... :pending-flare ...}}`
- Added slot parameter to `ensure-sidebar-view!`

#### 3. VS Code Configuration (`package.json`)
```json
"views": {
  "joyride": [
    {"id": "joyride.flare-1", "name": "Flare 1", "when": "joyride.flare.sidebar-1.isActive"},
    {"id": "joyride.flare-2", "name": "Flare 2", "when": "joyride.flare.sidebar-2.isActive"},
    {"id": "joyride.flare-3", "name": "Flare 3", "when": "joyride.flare.sidebar-3.isActive"},
    {"id": "joyride.flare-4", "name": "Flare 4", "when": "joyride.flare.sidebar-4.isActive"},
    {"id": "joyride.flare-5", "name": "Flare 5", "when": "joyride.flare.sidebar-5.isActive"}
  ]
}
```

#### 4. When Contexts (`when-contexts.cljs`)
- Added flare content contexts: `joyride.flare.sidebar-N.isActive` (N=1-5)
- **All sidebar slots** use when contexts - only appear when they have content
- Slot 1 gets default content at startup (preserves original UX but allows closing)
- Context management functions: `set-flare-content-context!`, `initialize-flare-contexts!`

#### 5. Startup Initialization (NEW)
- Create default "getting started" flare content for slot 1 at Joyride activation
- Set `joyride.flare.sidebar-1.isActive` context to make slot 1 visible by default
- Preserves original user experience while enabling consistent slot behavior

## Current Status

### ✅ Completed
- [x] Core API implementation with special key detection
- [x] Multiple sidebar provider registration
- [x] VS Code view configuration with `when` contexts
- [x] When context management for slot visibility
- [x] Updated examples and documentation
- [x] When context bug fix (double-namespacing resolved)

### 🔄 In Progress: Consistent When Context Behavior
- [ ] All slots (including slot 1) use when contexts consistently
- [ ] Startup initialization creates default content for slot 1
- [ ] All slots can be closed by users (including slot 1)
- [ ] Same initial UX preserved (slot 1 visible by default)

### 🧪 Testing Progress
- [x] Slot 1: Working (both when context and content)
- [x] Slot 2: Working (both when context and content)
- [x] Slot 3: When context works, content regression confirmed
- [ ] Slot 4: Not tested yet
- [ ] Slot 5: Not tested yet
- [x] Multiple concurrent slots: Working (slots 1, 2, 3 coexist)
- [x] When context visibility: Working (slots appear/disappear correctly)

### 🐛 Known Issues

#### REGRESSION: First-Try Content Update Failure
**Problem**: Sidebar flares require two evaluations to display content:
1. First evaluation returns `{:sidebar #object [...]}` - creates view but no content appears
2. Second evaluation returns `{:sidebar #object [...]}` - content finally populates

**Confirmed Status**:
- ✅ When context updates work correctly (slots appear immediately)
- ❌ Content updates fail on first try (consistent across slots 2 and 3)
- ✅ Content updates work on second try (consistent pattern)

**Expected Behavior**: Should populate content on first try (worked in previous implementation)

**Root Cause Hypothesis**:
- Timing/lifecycle issue between webview creation and content population
- Possible race condition in `update-view-with-options!` when view is newly created
- May be related to `resolveWebviewView` callback timing changes

**Debugging Strategy**:
1. Check if `resolveWebviewView` callback timing has changed
2. Verify pending flare processing logic in sidebar provider
3. Test if `update-view-with-options!` is being called correctly on first try
4. Add logging to track webview readiness state during first content update

## Testing Protocol

### Testing Environment Setup
1. **Use Dev Extension Host**: Test new API via Backseat Driver CLJS REPL
2. **Restart Required**: Package.json changes need Extension Development Host restart
3. **Visual Verification**: Always confirm UI results with human, don't rely only on REPL responses

### Test Cases to Validate
```clojure
;; Test 1: Single sidebar slot
(jsci/eval-string "(flare/flare!+ {:html [:h1 \"Test 1\"] :key :sidebar-1})")

;; Test 2: Multiple concurrent sidebar slots
(jsci/eval-string "(flare/flare!+ {:html [:h1 \"Slot 2\"] :key :sidebar-2})")
(jsci/eval-string "(flare/flare!+ {:html [:h1 \"Slot 3\"] :key :sidebar-3})")

;; Test 3: Slot visibility (should show/hide based on content)
;; Test 4: Closing specific sidebar slots
;; Test 5: Regular panels still work (non-sidebar keys)
```

## Next Steps

### Immediate Priority
1. **Implement consistent when-context behavior** - Make all slots (including slot 1) use when contexts
2. **Debug regression** - Fix first-try content update issue (confirmed consistent pattern)
3. **Verify when context wiring** - Ensure flare creation properly sets `joyride.flare.sidebar-N.isActive` contexts

### Remaining Testing
3. **Complete slot testing** - Test slots 4 and 5 (expect same regression pattern)
4. **Test closing behavior** - Verify `close!` works with new data structures and updates when contexts
5. **Test regular panels** - Ensure non-sidebar keys still work (regression check)

### Quality Assurance
6. **Performance check** - Ensure no memory leaks with multiple providers
7. **Edge case testing** - Test rapid flare creation/destruction, large content, etc.

## Architecture Notes

The design is extensible - adding more slots just requires updating the `sidebar-keys` set and package.json configuration.

Key insights:
- **Special key routing**: Using special key values for routing is more elegant than adding new API parameters, keeping the interface clean while adding powerful functionality
- **Consistent when-context behavior**: All slots behave the same way (appear when content, disappear when closed), giving users full control over sidebar real estate
- **Preserved UX**: Startup initialization ensures slot 1 appears by default, maintaining the same initial experience while enabling new capabilities