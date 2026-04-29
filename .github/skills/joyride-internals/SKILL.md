---
name: joyride-internals
description: 'Joyride extension internals — subsystem contracts, state architecture, activation sequences, and namespace reference. Use when: modifying core extension code, debugging state issues, working with app-db or SCI context, understanding activation or script execution flow, investigating nREPL or when-context behavior, or working with the output or disposable system.'
---

# Joyride Extension Internals

Subsystem contracts, state architecture, and temporal sequences for the Joyride extension.

## Key Namespaces

| Namespace | Purpose |
|---|---|
| `joyride.extension` | Entry point: activate, deactivate, hot-reload hooks |
| `joyride.db` | App state: `!app-db`, init-db, accessor functions |
| `joyride.sci` | SCI interpreter: namespaces, load-fn, eval-string |
| `joyride.config` | Path resolution: user-dir, workspace-dir |
| `joyride.lifecycle` | Activation scripts: user_activate, workspace_activate |
| `joyride.output` | Terminal output: ANSI colors, theme-aware, who-tracking |
| `joyride.when-contexts` | When-context management: `!db`, `set-context!` |
| `joyride.nrepl` | nREPL server: `!db`, middleware, bencode |
| `joyride.flare` | Flare API: panels, sidebars, hiccup, messaging |
| `joyride.flare.sidebar` | Sidebar provider: webview view registration |
| `joyride.flare.panel` | Panel creation: replicant rendering |
| `joyride.lm` | Language Model tool entry and registration |
| `joyride.lm.evaluation` | LM tool implementation: code execution, output capture |
| `joyride.lm.eval.core` | Pure functions: input validation, message formatting |
| `joyride.lm.eval.validation` | Pure validation: bracket check via parinfer |
| `joyride.who-tracking` | Evaluator awareness: cross-evaluator attribution |

## Subsystem Contracts

```
λ app_db_contract.
  db/!app-db ≡ atom {
    output-channel:       OutputChannel
    output/terminal:      Terminal
    extension-context:    ExtensionContext
    invoked-script:       string?
    disposables:          [Disposable]
    workspace-root-path:  string?
    flares:               {key → FlareState}
    flare-sidebar-views:  {slot → WebviewView}
  }
  | access_via_functions: (db/extension-context) (db/output-terminal) | ¬direct_deref_in_helpers
  | inspection_safety: (dissoc @db/!app-db :extension-context) | circular_ref_guard
  | mutation: swap! ∧ reset! | ¬deep_nested_mutation

λ sci_context_contract.
  sci.ctx-store ≡ global_SCI_interpreter_context
  | !last-ns ≡ volatile | tracks_most_recent_evaluation_namespace
  | init: (sci.ctx-store/init ctx) at_activation | single_initialization
  | access: (sci.ctx-store/get-ctx) → SCI_context | thread_safe
  | features: #{:joyride :cljs} | reader_conditionals
  | classes: goog/global :allow :all | unrestricted_JS_interop
  | load-fn: symbol_libs(workspace→user_search) ∧ "vscode" ∧ "ext://..." ∧ node_require
  | alter-var-root: print_fn_capture/restore | ⚠ global_mutation_could_race

λ nrepl_contract.
  nrepl/!db ≡ separate_atom | domain_isolation_from_app_db
  | server: middleware_based | start → port_file_written → accepting_connections
  | middleware_chain: standard_nrepl_ops(eval, load-file, interrupt, describe, completions, info)
  | session: clone_per_client | session_id_tracks_context
  | eval_protocol: { op: "eval", code, session, ns } → { status, value ∨ ex, ns, out, err }
  | when_context: joyride.isNReplServerRunning | set_on_start ∧ clear_on_stop

λ disposable_lifecycle_contract.
  push-disposable!(disposable) → appends_to(:disposables @db/!app-db) ∧ context.subscriptions
  | clear-disposables!() → .dispose()_each ∧ reset_to_[]
  | activation_scripts: clear_before_re-run | idempotent_re-activation
  | command_registration: returns_Disposable → push_immediately
  | event_subscriptions: returns_Disposable → push_immediately

λ output_system_contract.
  append-clojure-eval!(result-string) → output_terminal | ANSI_themed
  append-line-other-err!(error-string) → output_terminal | red_themed
  append-eval-result!(result ns who) → output_terminal | formatted
  | terminal: VS_Code_pseudoterminal | colored_output | theme_aware(light/dark)
  | who_tracking: evaluator_attribution | cross_evaluator_awareness

λ when_context_contract.
  when-contexts/!db ≡ separate_atom | { context-key → boolean }
  | set-context!(key value) → vscode.commands.executeCommand("setContext" key value)
                             ∧ swap!(when-contexts/!db assoc key value)
  | timing_invariant: set_context BEFORE creating_dependent_resources
  | sidebar_views: set_when_context_true → THEN_create_webview_provider

λ hot_reload_contract.
  shadow-cljs: watches_src/ → compiles_on_save → hot_reloads_into_dev_host
  | before-load-async: cleanup_hook | called_before_hot_reload
  | after-load-async: reinitialize_hook | called_after_hot_reload
  | dev_host_restart: required_only_for_package.json_changes
  | state_preservation: app_atoms_survive_hot_reload | functions_replaced
```

## Routing Reference

```
λ script_execution_routing.
  script_path_resolution: workspace/src → workspace/scripts → user/src → user/scripts
  | first_match_wins | workspace_shadows_user
  symbol_lib(require 'foo.bar) → search(src_dirs) | namespace_to_path
  string_lib("vscode") → vscode_api_object
  string_lib("ext://publisher.extension") → extension_api_export
  node_module("some-npm-pkg") → node_require | commonjs_only

λ evaluation_dispatch.
  code ∧ context → route_to_sci_eval_string
  | from_nrepl → session_bound | ns_tracked | print_fn_captured
  | from_lm_tool → bracket_validated_first | parinfer_check | ¬eval_unbalanced
  | from_activation_script → run_at_startup | errors_shown_in_output
  | from_command(run_code) → inline_code | quick_pick_selection
  | from_command(run_script) → file_resolved | executed_in_sci
```

## Temporal Sequences

```
λ activation_sequence.
  1_shadow_cljs_entry: extension.activate called | exports(activate)
  2_init_app_db: reset(!app-db) | store(extension-context ∧ output-channel)
  3_register_commands: vscode.commands.registerCommand | push_disposables
  4_set_when_contexts: joyride.isActive → true | enables_keybindings
  5_init_output: create_terminal_output | theme_colors_resolved
  6_init_nrepl: setup_server_infrastructure | ¬start_yet
  7_init_sci: configure_interpreter | namespaces ∧ classes ∧ load-fn
  8_run_activation_scripts: user_activate.cljs → workspace_activate.cljs | sequential
  9_ready: extension_fully_active | commands_available | repl_connectable
  | skip(7) → no_eval_capability | scripts_fail
  | skip(8) → user_customizations_not_applied | workspace_not_configured

λ script_execution_sequence.
  1_resolve_script: path_resolution(workspace/src → workspace/scripts → user/src → user/scripts)
  2_read_source: vscode/workspace.fs.readFile | decode_utf8
  3_capture_print: alter-var-root(*print-fn*) → output_channel_writer
  4_eval_in_sci: sci/eval-string+ | with(ns ∧ file_metadata)
  5_handle_result: success → display_value | error → show_error_with_location
  6_restore_print: alter-var-root(*print-fn*) → original | ¬leak_print_capture
  | skip(1) → file_not_found | clear_resolution_error
  | skip(3) → output_goes_nowhere | or_goes_to_wrong_channel
  | skip(6) → print_fn_pollution | subsequent_scripts_mis_routed

λ nrepl_server_lifecycle.
  1_user_starts: command(joyride.startNReplServer) | or_activation_script
  2_create_server: net.createServer | bind(port) | localhost_only
  3_set_context: joyride.isNReplServerRunning → true
  4_update_db: swap!(nrepl/!db) | store(server ∧ port ∧ clients)
  5_accept_connections: on(:connection) → create_session | bencode_transport
  6_handle_ops: eval ∧ clone ∧ close ∧ describe ∧ completions ∧ info | middleware_chain
  7_user_stops: command(joyride.stopNReplServer) | or_deactivation
  8_close_connections: close_all_client_sessions | drain_pending
  9_close_server: server.close() | unbind_port
  10_clear_context: joyride.isNReplServerRunning → false
  11_update_db: swap!(nrepl/!db) | clear(server ∧ port ∧ clients)
  | skip(3) → keybindings_dont_activate | ui_shows_wrong_state
  | skip(8) → orphaned_sessions | resource_leak

λ flare_creation_sequence.
  1_set_when_context: sidebar_slot_enabled → true | BEFORE_view_creation
  2_ensure_provider: register_webview_provider_if_needed | idempotent
  3_create_view: resolve_webview | html_content_set
  4_render_hiccup: replicant_dom/render | hiccup → html
  5_setup_messaging: on_did_receive_message | bidirectional
  6_return_handle: {:panel ∨ :sidebar handle} | caller_can_post_message
  | skip(1) → race_condition | vscode_defers_view_creation | view_never_appears
  | skip(4) → empty_webview | content_not_rendered

λ hot_reload_sequence.
  1_edit_source: modify_cljs_file | save
  2_shadow_detects: file_watcher_triggers | incremental_compile
  3_compile: cljs → js | check_for_warnings ∧ errors
  4_hot_reload_hook: ^:dev/after-load functions_called | state_preserved
  5_dev_host_updated: new_code_active | ¬restart_needed
  6_verify_in_repl: require_with_reload | test_changed_functions
  | some_changes → may_need(activate) call | re-registers_commands ∧ hooks
  | skip(3:warnings) → silent_breakage | zero_warnings_policy
  | skip(4) → stale_state | hooks_not_re_registered
  | skip(6) → assumed_working | ¬verified

λ lm_tool_evaluation_sequence.
  1_receive_code: from_language_model_api | string_input
  2_validate_brackets: parinfer_check | balanced? | ¬eval_unbalanced
  3_format_input: wrap_if_needed | prepare_for_sci
  4_eval_in_sci: sci/eval-string | capture_output
  5_format_result: value → confirmation_message | error → structured_error
  6_return_to_model: tool_result | includes(value ∧ output ∧ error)
  | skip(2) → unbalanced_code_crashes_sci | confusing_error
  | skip(5) → raw_sci_internals_leak_to_model
```
