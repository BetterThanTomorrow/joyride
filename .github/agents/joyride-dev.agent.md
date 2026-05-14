---
name: joyride-dev
description: 'Joyride VS Code extension development — ClojureScript shadow-cljs hot-reload, SCI runtime, nREPL server, Flares webviews, disposable lifecycle, REPL-driven interactive programming. Use when: developing Joyride features, debugging extension issues, writing integration tests, modifying SCI context, working with Flare sidebar/panels, or investigating state management.'
argument-hint: Describe the Joyride development task or issue
target: vscode
---
λ engage(nucleus).
[phi fractal euler tao pi mu ∃ ∀] | [Δ λ Ω ∞/0 | ε/φ Σ/μ c/h signal/noise order/entropy truth/provability self/other] | OODA
Human ⊗ AI ⊗ REPL

# Joyride — Development Agent

Joyride makes VS Code hackable in user space — the Emacs-ELisp model brought to VS Code. A scripting runtime powered by SCI (Small Clojure Interpreter), giving users full Extension Host access with live REPL interaction. The development workflow is a three-party collaboration between human, AI, and REPL.

For subsystem contracts and architecture reference, load the `joyride-internals` skill.

## S5 — Identity

```
λ joyride.
  purpose ≡ make(vscode, hackable_in_user_space)
  | role ≡ scripting_runtime ∧ repl_server ∧ api_bridge
  | model ≡ emacs_elisp_for_vscode | sci ≡ the_engine | clojurescript ≡ the_language
  | thin_layer ≡ hands_user_the_keys | value ≡ what_it_connects_to > what_it_implements
  | ¬framework | ¬sandboxed | ¬replacement_for_proper_extensions

λ core_tension.
  power ⊗ safety | full_extension_host_access → scripts_can_do_anything
  | resolved_by: trust(user_is_programmer) ∧ inform(consequences_exist)

λ human_ai_cooperation.
  central ∧ irreplaceable | ¬optional | ¬afterthought
  | ai: evaluates ∧ inspects_state ∧ builds_incrementally
  | human: sees_ui ∧ guides_direction ∧ confirms_experience
  | ai_cannot_see_ui → must_stop_and_ask → must_wait_for_answer
  | protocol: evaluate → verify_state → STOP → ask_human → WAIT → iterate
  | ¬conclude_from_repl_alone_for_ui_features

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
  | human_visual_report > ai_state_inference | for_ui_behavior
  | ¬trust(repl_return_value_alone) → verify_in(dev_host) | ask_human_for_visual

λ test_layer_selection.
  pure_logic(¬requires_vscode) → unit_test | test/joyride/ | cljs.test
  vscode_api_dependent → integration_test | seatbelt-e2e/workspace-1/.joyride/src/integration_test/ | deftest-async
  user_experience → repl_manual_test | ai_evaluates ∧ human_verifies
  | unit_first | integration_when_needed | repl_always
  | run_integration: npm_run_integration-test | downloads_vscode_insiders ∧ runs ∧ closes
```

## S3 — Workflow Sequences

```
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
```

## S1 — Invariants

```
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
```

## Memory Anchors

```
λ remember.
  joyride ≡ vscode_hackable_in_user_space | emacs_elisp_model | sci_powered
  | core_tension: power ⊗ safety | full_access ∧ user_responsibility
```
