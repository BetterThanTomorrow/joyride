---
name: joyride-dev
description: 'Joyride VS Code extension development — ClojureScript shadow-cljs hot-reload, SCI runtime, nREPL server, Flares webviews, disposable lifecycle, REPL-driven interactive programming. Use when: developing Joyride features, debugging extension issues, writing integration tests, modifying SCI context, working with Flare sidebar/panels, or investigating state management.'
argument-hint: Describe the Joyride development task or issue
target: vscode
---
λ engage(nucleus).
[phi fractal euler tao pi mu ∃ ∀] | [Δ λ Ω ∞/0 | ε/φ Σ/μ c/h signal/noise order/entropy truth/provability self/other] | OODA
Human ⊗ AI ⊗ REPL

# Joyride — System VSM

Joyride makes VS Code hackable in user space — the Emacs-ELisp model brought to VS Code. A scripting runtime powered by SCI (Small Clojure Interpreter), giving users full Extension Host access with live REPL interaction. The development workflow is a three-party collaboration between human, AI, and REPL.

## S5 — Identity

```
λ joyride.
  purpose ≡ make(vscode, hackable_in_user_space)
  | role ≡ scripting_runtime ∧ repl_server ∧ api_bridge
  | model ≡ emacs_elisp_for_vscode | sci ≡ the_engine | clojurescript ≡ the_language
  | values: empowerment > hand_holding | interactivity > batch | reach > surface | data > objects
  | stance: repl_first | files_second | user_is_capable
  | ¬framework | ¬sandboxed | ¬replacement_for_proper_extensions | ¬opinionated_automation

λ scripting_runtime.
  sci(clojurescript) → extension_host(node) → vscode_api(unrestricted)
  | thin_layer ≡ hands_user_the_keys | ¬reimplements_features
  | value ≡ what_it_connects_to > what_it_implements
  | scripts ≡ super_powers ∧ responsibility | power ⊗ safety

λ core_tension.
  power ⊗ safety
  | full_extension_host_access → scripts_can_do_anything
  | users_bear_responsibility | ¬guardrails_beyond_awareness
  | tension_resolved_by: trust(user_is_programmer) ∧ inform(consequences_exist)

λ repl_triad.
  joyride_provides_nrepl | ¬connects_to_one
  | three_environments ⊗ three_purposes:
    backseat_driver_cljs ≡ primary_dev | builds_the_extension | replSessionKey("cljs")
    local_joyride ≡ user_interaction | quick_input ∧ progress ∧ questions
    dev_host_joyride ≡ user_api_testing | via(joyride.sci/eval-string) | reachable_through_backseat_driver
  | user_reports_issue → occurred_in_dev_host → investigate_via_backseat_driver

λ human_ai_cooperation.
  central ∧ irreplaceable | ¬optional | ¬afterthought
  | ai: evaluates ∧ inspects_state ∧ builds_incrementally
  | human: sees_ui ∧ guides_direction ∧ confirms_experience
  | ai_cannot_see_ui → must_stop_and_ask → must_wait_for_answer
  | protocol: evaluate → verify_state → STOP → ask_human → WAIT → iterate
  | ¬conclude_from_repl_alone_for_ui_features

λ design_values.
  empowerment > hand_holding | minimal_surface > feature_accumulation
  | interactive > static | data_oriented > object_oriented
  | composable > monolithic | explicit > magic
  | clojure_way ≡ guiding_philosophy | what_would_rich_hickey_do
  | fn(args) → results | side_effects ≡ last_resort_serving_larger_goal
  | prefer(destructuring ∧ maps ∧ namespaced_keywords ∧ flatness)

λ communication.
  direct ∧ data_focused | show(repl_expressions) ∧ show(results)
  | think_in(subexpressions) | build_up(step_by_step)
  | code_blocks_in_chat: include(in-ns) form_first | reader_knows_context
  | ¬println | prefer(inline_def) for_debugging
  | involve_human_often | use_local_joyride_for_interaction

λ absent(x).
  ∀present(element) → ∃absent(companion) | attend(absent) ≡ attend(present)
  | missing_FROM(x) > missing_NEAR(x) | completeness(¬assumed)
  | handler(¬written) ∧ test(¬exists) ∧ state(¬considered) ∧ assumption(¬explicit)
  | ui_verification(¬done) ∧ hot_reload(¬confirmed) ∧ watcher(¬checked)
  | default_mode ≡ attend(present_only) | resist(default_mode)

λ phase(x).
  repl_first(x) ∧ ¬file_edit(x) | file_edit(x) ∧ ¬assume_compiled(x)
  | propose(x) ∧ ¬implement(x) | implement(x) ∧ ¬exceed(x)
  | output(phase) ∩ output(next_phase) = ∅ | boundary ≡ what_you_withhold
  | ui_feature → execute(repl) → inspect(state) → STOP → ask(human) → WAIT
  | collapse(phases) ≡ default_mode | resist(default_mode)
```

## S4 — Decision Rules

```
λ which_repl.
  developing_extension_code → backseat_driver_cljs | replSessionKey: "cljs"
  testing_user_facing_api → dev_host_joyride | via(joyride.sci/eval-string) | through(backseat_driver)
  user_interaction(quick_input ∧ progress ∧ questions) → local_joyride | joyride_evaluate_code
  shadow_cljs_build_tooling → clj_session | replSessionKey: "clj" | build_system_only
  user_reports_bug → backseat_driver | bug_occurred_in(dev_extension_host)
  | ¬mix_environments | each_repl_has_distinct_purpose
  | uncertain_which → backseat_driver | default_for_extension_work

λ script_execution_routing.
  script_path_resolution: workspace/src → workspace/scripts → user/src → user/scripts
  | first_match_wins | workspace_shadows_user
  symbol_lib(require 'foo.bar) → search(src_dirs) | namespace_to_path
  string_lib("vscode") → vscode_api_object
  string_lib("ext://publisher.extension") → extension_api_export
  node_module("some-npm-pkg") → node_require | commonjs_only
  | ¬found_in_any_path → sci_load_error | clear_message

λ evaluation_dispatch.
  code ∧ context → route_to_sci_eval_string
  | from_nrepl → session_bound | ns_tracked | print_fn_captured
  | from_lm_tool → bracket_validated_first | parinfer_check | ¬eval_unbalanced
  | from_activation_script → run_at_startup | errors_shown_in_output
  | from_command(run_code) → inline_code | quick_pick_selection
  | from_command(run_script) → file_resolved | executed_in_sci
  | ¬raw_eval_without_context | always_know_origin

λ feature_development_approach.
  new_feature → repl_prototype_first | ¬file_edit_until_validated
  | pure_logic → separate_namespace | ¬require_vscode | unit_testable
  | side_effects → parent_namespace | !_suffix | minimal_surface
  | promise_returning → +_suffix | always_await_in_tests
  | state_change → accessor_function | ¬direct_deref_in_callers
  | ui_feature → ai_executes → ai_verifies_state → STOP → ask_human → WAIT
  | ¬conclude_from_repl_alone | repl_shows_state ≠ repl_shows_rendering

λ when_to_edit_files.
  human_signals_ready ∧ repl_validated → edit_files
  | ¬edit_speculatively | ¬edit_before_repl_confirms
  | structural_editing_only | ¬text_replacement_for_clojure
  | bottom_to_top | multiple_edits_highest_line_first
  | save → shadow_cljs_hot_reloads → check_watcher_output

λ when_to_restart_dev_host.
  package_json_changed(commands ∧ views ∧ configuration ∧ activation_events ∧ when_contexts) → restart_required
  | restart_resets_repl_state_completely
  | pre_restart: confirm(can_recreate_state) | ¬request_restart_if_uncertain
  | human_executes_restart | ai_never_restarts_directly
  | post_restart: reconnect_repl → re_require_namespaces → verify_state

λ debug_approach.
  1_context:    gather(failing_env ⊗ working_env) | what_data_differs
  2_reproduce:  exact_conditions_in_repl | inline_def_for_inspection | ¬println
  3_trace:      data_flow(input → transform → output) | inspect_atoms | dissoc(:extension-context)
  4_fix:        root_cause(data_flow) | ¬symptom_patch | repl_validate_fix
  5_validate:   test_in(original_failing_conditions) | human_confirms_ui
  | trace > guess | data > narrative | inline_def > println | repl > log_reading

λ truth_hierarchy.
  dev_extension_host > repl_state_inspection > automated_test > source > docs > assumption
  | dev_extension_host ≡ ground_truth | where_new_code_actually_runs
  | repl_state_inspection ≡ probe_app_db ∧ when_contexts ∧ nrepl_db | ¬assume_from_source
  | automated_test ≡ unit(pure_logic) ∧ integration(vscode_host) | ¬covers(ui_rendering)
  | human_visual_report > ai_state_inference | for_ui_behavior
  | ¬trust(repl_return_value_alone) → verify_in(dev_host) | ask_human_for_visual

λ state_inspection_safety.
  inspect(!app-db) → always(dissoc :extension-context) | circular_references
  inspect(vscode_api_objects) → select-keys ∨ dissoc | ¬print_raw
  inspect(when_contexts) → (:contexts @when-contexts/!db)
  inspect(nrepl_state) → @nrepl/!db | check_server_running?
  | verbose_output ≠ useful_output | targeted_inspection > full_deref

λ test_layer_selection.
  pure_logic(¬requires_vscode) → unit_test | test/joyride/ | cljs.test
  vscode_api_dependent → integration_test | vscode-test-runner/workspace-1/.joyride/src/integration_test/ | deftest-async
  user_experience → repl_manual_test | ai_evaluates ∧ human_verifies
  | unit_first | integration_when_needed | repl_always
  | run_integration: npm_run_integration-test | downloads_vscode_insiders ∧ runs ∧ closes

λ async_convention.
  extension_source_code: promesa.core | p/let ∧ p/do ∧ p/chain
  user_scripts: sci_async/await | ^:async ∧ await | idiomatic_lightweight
  | promesa_for_user_scripts: only_when(advanced_combinators ∨ error_channels)
  | ¬mix_paradigms_within_module
```

## S3 — Temporal Rules

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

λ development_iteration_cycle.
  1_repl_explore: evaluate_subexpressions | understand_current_behavior
  2_repl_prototype: build_solution_incrementally | inline_def_for_debugging
  3_repl_test: cljs.test_assertions | verify_behavior_interactively
  4_repl_validate: inspect_state(app_db ∧ when_contexts) | confirm_data_flow
  5_human_signal: human_approves_approach | "write it" | ready_for_files
  6_edit_files: structural_editing | bottom_to_top | minimal_diff
  7_watcher_verify: shadow_cljs_compiles → check_output | zero_warnings
  8_hot_reload_test: dev_host_picks_up_changes | re_evaluate_in_repl
  9_ui_verify: if(ui_feature) → ask_human | STOP ∧ WAIT
  10_iterate: if(¬satisfied) → goto(1) | if(done) → commit
  | skip(1) → coding_blind | ¬understand_before_changing
  | skip(5) → premature_file_edit | human_not_consulted
  | skip(7) → broken_build_undetected | errors_accumulate
  | skip(9) → ui_bug_shipped | ai_assumed_rendering_correct

λ issue_workflow_sequence.
  1_create_branch: git_checkout -b <issue-number>-descriptive-name
  2_repl_explore: understand_problem | reproduce_in_dev_host
  3_establish_criteria: data_oriented ∧ focused ∧ minimal_change
  4_validate_approach: confirm_with_maintainer | share_findings
  5_repl_iterate: develop_solution | cljs.test_coverage
  6_apply_edits: structural_editing | minimal_diff | include_tests
  7_update_changelog: [Unreleased] section | "Fix: [title](issue_url)" for_bugs | no_prefix_for_features
  8_run_integration_tests: npm run integration-test | all_pass
  9_prepare_pr: description ∧ checklist(read_CONTRIBUTE ∧ clear_issue ∧ regression_tests ∧ changelog)
  | skip(2) → fixing_without_understanding | wrong_solution_likely
  | skip(4) → wasted_effort_if_approach_wrong
  | skip(6:tests) → regression_risk | untested_change
  | skip(7) → forgotten_changelog | release_notes_incomplete

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

## S2 — Coordination Rules

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
  | ¬contract_responsibility: cross_atom_consistency | caller_coordinates

λ sci_context_contract.
  sci.ctx-store ≡ global_SCI_interpreter_context
  | !last-ns ≡ volatile | tracks_most_recent_evaluation_namespace
  | init: (sci.ctx-store/init ctx) at_activation | single_initialization
  | access: (sci.ctx-store/get-ctx) → SCI_context | thread_safe
  | features: #{:joyride :cljs} | reader_conditionals
  | classes: goog/global :allow :all | unrestricted_JS_interop
  | load-fn: symbol_libs(workspace→user_search) ∧ "vscode" ∧ "ext://..." ∧ node_require
  | alter-var-root: print_fn_capture/restore | ⚠ global_mutation_could_race
  | ¬contract_responsibility: SCI_version_compatibility | pinned_by_deps.edn_SHA

λ nrepl_contract.
  nrepl/!db ≡ separate_atom | domain_isolation_from_app_db
  | server: middleware_based | start → port_file_written → accepting_connections
  | middleware_chain: standard_nrepl_ops(eval, load-file, interrupt, describe, completions, info)
  | session: clone_per_client | session_id_tracks_context
  | eval_protocol: { op: "eval", code, session, ns } → { status, value ∨ ex, ns, out, err }
  | when_context: joyride.isNReplServerRunning | set_on_start ∧ clear_on_stop
  | ¬contract_responsibility: Calva_client_behavior | Calva_coordinates_own_sessions

λ disposable_lifecycle_contract.
  push-disposable!(disposable) → appends_to(:disposables @db/!app-db) ∧ context.subscriptions
  | clear-disposables!() → .dispose()_each ∧ reset_to_[]
  | activation_scripts: clear_before_re-run | idempotent_re-activation
  | command_registration: returns_Disposable → push_immediately
  | event_subscriptions: returns_Disposable → push_immediately
  | ¬contract_responsibility: VS_Code_GC_of_disposed_objects

λ output_system_contract.
  append-clojure-eval!(result-string) → output_terminal | ANSI_themed
  append-line-other-err!(error-string) → output_terminal | red_themed
  append-eval-result!(result ns who) → output_terminal | formatted
  | terminal: VS_Code_pseudoterminal | colored_output | theme_aware(light/dark)
  | who_tracking: evaluator_attribution | cross_evaluator_awareness
  | ¬contract_responsibility: output_persistence_across_sessions

λ when_context_contract.
  when-contexts/!db ≡ separate_atom | { context-key → boolean }
  | set-context!(key value) → vscode.commands.executeCommand("setContext" key value)
                             ∧ swap!(when-contexts/!db assoc key value)
  | timing_invariant: set_context BEFORE creating_dependent_resources
  | sidebar_views: set_when_context_true → THEN_create_webview_provider
  | ¬contract_responsibility: VS_Code_when_clause_evaluation_timing

λ hot_reload_contract.
  shadow-cljs: watches_src/ → compiles_on_save → hot_reloads_into_dev_host
  | before-load-async: cleanup_hook | called_before_hot_reload
  | after-load-async: reinitialize_hook | called_after_hot_reload
  | dev_host_restart: required_only_for_package.json_changes
  | state_preservation: app_atoms_survive_hot_reload | functions_replaced
  | ¬contract_responsibility: shadow-cljs_compilation_errors | watcher_health
```

## S1 — Architectural Rules

```
λ naming_conventions.
  ∀side_effecting_fn: suffix(!) | set-context! clear-disposables! push-disposable!
  ∀promise_returning_fn: suffix(+) | run-code+ slurp+ eval-string+
  ∀both_effect_and_promise: suffix(!+) | flare!+ post-message!+
  ∀predicate_fn: suffix(?) | light-theme? server-running?
  ∀atom: prefix(!) | !app-db !db !last-ns
  ∀private_helper: defn- | ¬export ¬public
  | violations ≡ immediate_fix | naming_is_documentation

λ state_management.
  ∀app_state: stored_in(db/!app-db) | central_atom
  ∀domain_state: isolated_atoms(when-contexts/!db, nrepl/!db) | ¬single_tree
  | access_via_functions: (db/extension-context) | ¬(deref db/!app-db) in_helpers
  | pass_data_explicitly: ¬access_atoms_directly_from_helpers
  | deprecated_accessors: ^{:deprecated "version"} metadata | migration_path_documented

λ pure_impure_separation.
  ∀namespace_with_side_effects: pure_logic_in_sub_namespace
  | example: lm.eval.core(pure) ∧ lm.eval.validation(pure) ∧ lm.evaluation(side_effects)
  | pure_ns: ¬require_vscode | unit_testable | deterministic
  | impure_ns: requires_vscode ∧ extension_context | integration_testable
  | ¬mix_pure_and_impure_in_same_ns_when_separable

λ error_handling.
  ∀user_facing_boundary: try_catch | show_error_message ∨ output_to_terminal
  ∀optional_integration: no-op_gracefully | (when calva-available? ...)
  ∀bracket_validation: before_LM_code_execution | reject_unbalanced
  | ¬silent_error_swallowing | error_always_visible_somewhere

λ vscode_api_preference.
  ∀file_operations: vscode/workspace.fs > node/fs | remote_friendly
  | disposable_pattern: always_push_to_subscriptions | ¬leak_event_handlers
  | when_clauses: set_context BEFORE dependent_command_enablement

λ watcher_gate.
  MANDATORY: ∀code_change → verify_watcher_health_AFTER_save
  | shadow-cljs_watch: check_task_output | compilation_errors ≡ blocking
  | test_runner: check_Calva_Output_Log | failures ≡ blocking
  | test_failure_format: test_name ∧ file:line:column ∧ expected_vs_actual
  | problems_panel: linting_info | warnings ≡ address_root_cause
  | ¬proceed_to_next_change until(current_change_verified)
  | broken_structure → fix_before_continuing | ¬accumulate_errors

λ dev_workflow.
  branch: <issue-number>-descriptive-name | 247-remove-eval-autobalancing
  | CHANGELOG: [Unreleased] section | Fix: prefix_for_bugs | plain_for_features
  | issue_link: (https://github.com/BetterThanTomorrow/joyride/issues/<number>)
  | ¬force_push | preserve_PR_review_trail
  | package.json_changes → dev_host_restart_required | human_executes

λ dev_validation.
  ladder(ascending_confidence):
    1_repl_eval:        evaluate_subexpressions | inline_def_for_debug | ¬println
    2_cljs_test:        write_tests_interactively | run_in_repl
    3_state_inspection: @db/!app-db(dissoc :extension-context) | verify_internal_state
    4_human_ui_verify:  STOP → ask → WAIT | ¬conclude_from_repl_alone
    5_file_edit:        structural_editing | bottom_to_top | at_human_signal
    6_watcher_verify:   shadow-cljs_output ∧ calva_output_log | after_save
    7_integration_test: npm_run_integration-test | before_PR
  | ¬skip_rungs | each_rung_gates_the_next

λ structural_editing.
  ∀clojure_file_edit: structural_editing_tools | ¬text_replacement
  | replace_top_level_form ∧ insert_top_level_form ∧ clojure_append_code
  | multiple_edits: bottom_to_top | line_numbers_shift_down_on_edit_above
  | bracket_integrity: parinfer_powered | automatic_balancing
  | rich_comment_forms: treated_as_top_level | preserved_during_edits
```

## Memory Anchors

```
λ remember.
  joyride ≡ vscode_hackable_in_user_space | emacs_elisp_model | sci_powered
  | core_tension: power ⊗ safety | full_access ∧ user_responsibility

  the_invariants:
    ∀script → runs_in_extension_host | full_vscode_api_access | unrestricted
    ∀eval → flows_through_sci | ¬compiled | interpreted_at_runtime
    ∀path → user_scope ∨ workspace_scope | workspace_takes_precedence
    ∀activation → user_activate.cljs ∧ workspace_activate.cljs | re-runnable ∧ disposable_managed
    ∀state → lives_in(!app-db ∨ domain_atoms) | accessors_are_functions | ¬direct_deref_from_helpers
    ∀nrepl → joyride_provides_server | calva_connects_as_client | ¬reverse
    ∀ai_interaction → first_class_via_lm_tool | ¬afterthought
    ¬sandbox | ¬compilation_step | ¬framework_opinions

  the_fears:
    sci_config_missing_namespace → user_script_fails_silently → frustration
    load-fn_resolution_wrong → require_finds_wrong_file → subtle_bugs → trust_broken
    !app-db_circular_reference → repl_inspection_hangs → ai_blind → debugging_blocked
    hot_reload_breaks_state → extension_inconsistent → dev_host_restart_needed → repl_state_lost
    when_context_race → view_creation_deferred → ui_element_missing → user_sees_nothing
    activation_script_throws → extension_startup_broken → all_scripts_dead → user_locked_out
    nrepl_server_port_conflict → server_fails_to_start → no_repl → no_interactive_programming
    ai_concludes_without_human_verification → ui_bug_ships → trust_in_process_broken
    package.json_change_without_restart → dev_host_stale → feature_appears_broken → wasted_debugging
    file_edit_without_repl_validation → syntax_error_in_cljs → watcher_breaks → cascade_of_errors

  the_checks:
    before_eval: repl_connected ∧ correct_session(cljs) | ¬clj_session
    before_file_edit: solution_validated_in_repl | ¬edit_first
    before_inspecting_app_db: dissoc(:extension-context) | circular_reference_safety
    before_ui_conclusion: human_asked ∧ human_confirmed | ¬repl_return_value_alone
    before_dev_host_restart: confirm_ability_to_recreate_repl_state | ¬lose_work
    before_structural_edit: bottom_to_top_ordering | line_numbers_shift_downward
    after_file_save: check_shadow_cljs_watcher ∧ check_calva_output_log | compilation ≡ ground_truth
    after_hot_reload: verify_in_dev_host | ¬assume_reload_succeeded
    after_package_json_change: human_restarts_dev_host | ai_waits

  the_dev_checks:
    before_coding: shadow_cljs_watcher_running ∧ healthy | ¬code_without_feedback
    before_trusting_state: repl_probe > memory | ¬assume_state_from_prior_evaluation
    before_multiple_edits: plan_bottom_to_top | highest_line_number_first
    before_when_context_use: set_context_BEFORE_creating_dependent_view | context_first_approach
    after_change: verify_via_repl ∧ check_watcher | ¬proceed_until_verified
    after_test_write: run_via_cljs.test_in_repl | shadow_test_watch_confirms

  the_dynamics:
    script_runs: user_triggers → sci_eval → extension_host_executes → result_returns
    repl_connects: nrepl_server_starts → calva_connects_as_client → eval_available
    activation_flows: vscode_activates_extension → user_activate.cljs → workspace_activate.cljs → ready
    dev_iteration: repl_experiment → validate → file_edit → shadow_hot_reload → repl_verify → ask_human
    feature_development: backseat_driver_repl → build_incrementally → test_in_dev_host → human_verifies_ui
    state_inspection: @!app-db → dissoc_extension_context → inspect_safely → targeted_select_keys
    error_recovery: watcher_reports_error → fix_in_repl_first → apply_to_file → watcher_confirms_clean
    three_repl_flow: backseat_driver(builds) → local_joyride(interacts) → dev_host_joyride(tests_user_api)
```


