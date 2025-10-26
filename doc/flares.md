# Joyride Flares

## Overview

Flares are Joyride's WebView-based UI system for creating panels and sidebar views. The API provides a unified interface for creating both types of views with support for HTML, Hiccup, file content, and URLs.

## Core Concepts

### Content Types
Flares support four content input methods:
- **`:html`** - HTML string OR Hiccup data structure (vectors)
- **`:url`** - External URL displayed in sandboxed iframe
- **`:file`** - Path to HTML file (absolute, relative to workspace, or URI)
- **Hiccup rendering** - Automatic conversion to HTML using Replicant

### View Types
- **Panels** - Standard webview panels with configurable columns and icons
- **Sidebars** - Fixed sidebar views using `:sidebar-1` through `:sidebar-5` keys
- **Identification** - Flares are identified by `:key` for reuse and management

### State Management
- All active flares stored in `db/!app-db` under `:flares` key
- Each flare entry contains `:view` and optional `:message-handler`
- Sidebar views additionally tracked in `:flare-sidebar-views` with slot metadata

## API Functions

### `flare!+` - Create or Update View
**Primary creation function**. Returns promise resolving to `{key view}` map.

**Required Options**:
- One content option: `:html`, `:url`, or `:file`
- `:key` - Keyword identifier (default: `:anonymous`)

**Panel Options**:
- `:title` - Tab/view title (default: "Flare")
- `:icon` - Tab icon (string path/URL or `{:light "..." :dark "..."}` map)
- `:column` - `vscode.ViewColumn` for panel placement (default: `js/undefined`)
- `:reveal?` - Show panel when created/reused (default: `true`)
- `:preserve-focus?` - Keep focus when revealing (default: `true`)

**Sidebar Options**:
- Use `:key :sidebar-1` through `:sidebar-5` for sidebar placement
- Sidebar keys automatically determine placement
- `:reveal?` and `:preserve-focus?` work for sidebars

**Advanced Options**:
- `:webview-options` - VS Code WebviewPanelOptions & WebviewOptions map
- `:message-handler` - Function receiving messages from webview
- Default webview options: `{:enableScripts true, :localResourceRoots [...]}`

### `close!` - Dispose View
Closes and disposes a flare by key. Returns `true` if closed, `false` if not found.

### `close-all!` - Dispose All Views
Closes all active flare panels and sidebars. Returns count of closed flares.

### `post-message!+` - Send to Webview
Send message from extension to flare webview. Takes `flare-key` and `message` (serialized to JSON).

### `get-flare` - Retrieve Flare
Get flare data by key. Returns `{:view ... :message-handler ...}` or `nil`.

### `ls` - List Active Flares
Returns map of all active flares keyed by flare key.

## Content Processing

### Hiccup Support
- Hiccup vectors automatically rendered to HTML via Replicant
- Special handling for `:script` and `:style` tags (use `:innerHTML` to avoid escaping)
- File paths in Hiccup automatically transformed to webview URIs

### File Path Templates
Paths support template expansion:
- `{joyride/user-dir}` → User's Joyride directory (`~/.config/joyride`)
- `{joyride/extension-dir}` → Extension installation directory
- `{joyride/workspace-dir}` → Current workspace root

### File Path Resolution
- **Absolute paths** - Used as-is
- **Relative paths** - Resolved against workspace root (requires open workspace)
- **URIs** - Passed through unchanged
- Paths in HTML/Hiccup converted to webview-safe URIs

### URL Iframes
URLs wrapped in sandboxed iframe with permissions:
- `allow-scripts`, `allow-same-origin`, `allow-forms`, `allow-modals`
- `allow-orientation-lock`, `allow-pointer-lock`, `allow-presentation`
- `allow-top-navigation-by-user-activation`

## Implementation Details

### Panel Lifecycle
1. Check for existing panel by key
2. If exists and not disposed: reveal and update
3. If new: create panel, register dispose handler
4. Store in `db/!app-db` under `[:flares key]`
5. On dispose: cleanup message handler, remove from state

### Sidebar Lifecycle
1. Enable `when` context for sidebar slot
2. Register webview provider if not already registered
3. Check if view exists for slot
4. If pending: return promise, store options in `:pending-flare`
5. When VS Code resolves view: apply pending options
6. Sidebar-1 shows default "no content" message until first flare

### Message Handling
- Message handlers registered via `onDidReceiveMessage`
- Old handlers disposed when flare updated
- Handlers cleaned up on flare disposal
- Messages passed as-is to handler function

## Common Patterns

### Reusable Panels
```clojure
;; First call creates panel
(flare!+ {:key :my-panel :html "<h1>Hello</h1>"})

;; Second call reuses and updates same panel
(flare!+ {:key :my-panel :html "<h1>Updated</h1>"})
```

### Sidebar Views
```clojure
;; Create sidebar view in slot 1
(flare!+ {:key :sidebar-1 :html "<p>Sidebar content</p>"})

;; Sidebar keys :sidebar-1 through :sidebar-5 map to sidebar slots
```

### Bidirectional Communication
```clojure
;; Extension → Webview
(post-message!+ :my-flare {:action "update" :data {...}})

;; Webview → Extension
(flare!+ {:key :my-flare
          :html "..."
          :message-handler (fn [msg]
                            (js/console.log "Received:" msg))})
```

### Hiccup with File Paths
```clojure
;; Templates expanded, paths converted to webview URIs
(flare!+ {:html [:div
                 [:img {:src "{joyride/user-dir}/images/logo.png"}]
                 [:link {:href "styles/main.css" :rel "stylesheet"}]]})
```

## Error Handling

- **Missing content** - Throws if none of `:html`, `:url`, `:file` provided
- **Relative path without workspace** - Throws if relative `:file` path used without open workspace
- **Hiccup rendering errors** - Wrapped with context about structure and original error
- **Disposed views** - Return `nil` or `false` when operating on disposed flares