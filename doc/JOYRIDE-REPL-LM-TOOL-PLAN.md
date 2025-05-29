# Joyride REPL Language Model Tool Implementation Plan

## Executive Summary

This document outlines the simplified architecture and implementation plan for creating a Language Model Tool that gives GitHub Copilot (and other LLMs) direct access to Joyride's SCI evaluation environment. This enables AI assistants to interactively program and modify VS Code in real-time, creating an "AI Interactive Programming" experience where the LLM can hack the user's development environment while they work.

## Vision

Transform GitHub Copilot from a code completion tool into an **Interactive Programming Partner** that can:
- Execute ClojureScript code directly in VS Code's context
- Modify editor behavior and UI dynamically
- Create custom automation workflows on-demand
- Install and configure extensions programmatically
- Respond to user requests with live code execution
- Learn and adapt to user preferences through experimentation

## Core Architecture

### 1. Language Model Tool Interface

Leverage VS Code's built-in LanguageModelTool API with minimal parameters:

```typescript
interface JoyrideReplTool {
  name: "joyride_repl_execute"
  description: "Execute ClojureScript code in VS Code's Joyride environment"
  parameters: {
    code: string           // ClojureScript code to execute
    namespace?: string     // Target namespace (default: user)
  }
}
```

### 2. Architecture Overview

#### User Trust and Control Model

The user maintains complete sovereignty over code execution through VS Code's built-in trust system. Users can configure their trust level on multiple scopes:

- **Interactive** (default): Prompt for each execution request - user sees and approves code
- **Per-session**: Auto-allow for current VS Code session
- **Per-workspace**: Auto-allow for specific project/workspace
- **Global**: Auto-allow always (not recommended for security)

The architecture diagram below shows the **default interactive flow** where the user sees and approves each code execution request.

```
                           ┌─────────────────┐
                           │      USER       │
                           │   (Developer)   │
                           │   Sees Code +   │
                           │   Approves/     │
                           │   Denies        │
                           └─────────────────┘
                                    │
                                    │ Interactive
                                    │ Decision
                                    ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   GitHub        │    │   VS Code       │    │   Joyride       │
│   Copilot       │◄──►│   LM Tool API   │◄──►│   SCI Direct    │
│   (LLM)         │    │   + Security    │    │   Evaluation    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Tool Call     │    │   User Consent  │    │   VS Code API   │
│   Request       │    │   Dialog with   │    │   Access        │
│                 │    │   Code Preview  │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

**Key Architectural Principle**: Joyride's implementation **delegates all trust and security decisions to VS Code's established patterns**. When users configure auto-approval at any scope, the approval step becomes transparent but the same security infrastructure is leveraged - Joyride simply receives the "approved" execution request without needing to implement custom trust logic.

This delegation strategy ensures:
- Consistent security model across all VS Code tools
- No custom security implementation to maintain
- User familiarity with existing VS Code trust patterns
- Simplified Joyride implementation focused on core REPL functionality

### 3. Core Components

#### A. Language Model Tool Registration (`src/joyride/lm_tool.cljs`)

**Key Implementation Requirements:**

1. **Static Declaration**: Tool must be declared in `package.json` under `contributes.languageModelTools`
2. **Dynamic Registration**: Tool implementation registered via `vscode.lm.registerTool`
3. **Rich Confirmation**: Use `MarkdownString` for user-friendly code previews
4. **Error Handling**: Provide LLM-friendly error messages with actionable suggestions
5. **Cancellation Support**: Respect cancellation tokens for long-running operations
6. **Chat Integration**: Support `#joyride` tool references in chat prompts

```clojure
(ns joyride.lm-tool
  (:require ["vscode" :as vscode]
            [joyride.sci :as sci]
            [promesa.core :as p]))

;; Direct SCI evaluation - leverages existing Joyride infrastructure
(defn execute-code+ [code namespace]
  (sci/eval-string code {:ns (or namespace 'user)}))

;; Tool registration follows VS Code LanguageModelTool API
(defn register-lm-tool! []
  (vscode/lm.registerTool "joyride_evaluate_code" (create-joyride-tool)))
```

## Implementation Phases

### Phase 1: MVP Tool Integration
**Goal**: Leverage VS Code's built-in tool API with minimal custom security

**Deliverables**:
1. **Tool Registration**: Register with VS Code's LanguageModelTool API
2. **Direct SCI Integration**: Use existing Joyride SCI evaluation directly
3. **Built-in Security**: Rely on VS Code's confirmation dialogs
4. **Basic Error Handling**: Structured return of execution results

**Implementation Steps**:
```clojure
;; 1. Implement LanguageModelTool interface with enhanced features
(defn create-joyride-tool []
  #js {:prepareInvocation prepare-invocation
       :invoke invoke-tool})

;; 2. Register with VS Code's Language Model API (not directly with Copilot)
(vscode/lm.registerTool "joyride_evaluate_code" (create-joyride-tool))

;; 3. Add to package.json languageModelTools contribution point
;; 4. Enhanced error handling with LLM-friendly messages
;; 5. Rich confirmation dialogs with code syntax highlighting
```

**Success Criteria**:
- LM can execute ClojureScript via VS Code's Language Model Tool system
- Rich confirmation dialogs display code with syntax highlighting
- Tool appears in Copilot's available tools and can be referenced with `#joyride`
- Error messages provide actionable guidance for the LLM
- Proper cancellation support for long-running operations

## Technical Implementation Details

### 1. Minimal File Structure

```
src/joyride/
├── lm_tool.cljs           # Main tool implementation
└── lm_context.cljs        # Optional context helpers
```

### 2. Enhanced Core Implementation

```clojure
;; src/joyride/lm_tool.cljs
(ns joyride.lm-tool
  (:require ["vscode" :as vscode]
            [joyride.sci :as sci]
            [promesa.core :as p]))

(defn prepare-invocation [options token]
  "Prepare confirmation message with rich code preview"
  (let [code (.-code (.-input options))
        namespace (or (.-namespace (.-input options)) "user")]
    #js {:invocationMessage "Executing ClojureScript in Joyride environment"
         :confirmationMessages
         #js {:title "Execute ClojureScript Code"
              :message (vscode/MarkdownString.
                        (str "**Execute the following ClojureScript:**\n\n"
                             "```clojure\n" code "\n```\n\n"
                             "**Namespace:** `" namespace "`\n\n"
                             "This code will run with full VS Code API access and can modify your development environment."))}}))

(defn invoke-tool [options stream token]
  "Execute ClojureScript code with enhanced error handling"
  (let [code (.-code (.-input options))
        namespace (or (.-namespace (.-input options)) "user")]
    (try
      (when (.-isCancellationRequested token)
        (throw (js/Error. "Operation was cancelled")))

      (let [result (sci/eval-string code {:ns (symbol namespace)})]
        (.textContent stream (pr-str {:status :success
                                     :result result
                                     :namespace namespace})))
      (catch js/Error e
        (let [error-msg (str "ClojureScript execution failed: " (.-message e)
                            "\n\nSuggestion: Check syntax and ensure all required namespaces are available. "
                            "Try simpler expressions first if this is a complex operation.")]
          (.textContent stream (pr-str {:status :error
                                       :error error-msg
                                       :original-error (.-message e)}))
          (throw (js/Error. error-msg)))))))

(defn create-joyride-tool []
  "Create the Joyride Language Model Tool implementation"
  #js {:prepareInvocation prepare-invocation
       :invoke invoke-tool})

(defn register-tool! []
  "Register the Joyride tool with VS Code's Language Model API"
  (vscode/lm.registerTool "joyride_evaluate_code" (create-joyride-tool)))
```

### 3. Extension Integration

```clojure
;; Add to src/joyride/extension.cljs
(when (exists? js/vscode.lm)
  (require '[joyride.lm-tool :as lm-tool])
  (lm-tool/register-tool!))
```

### 4. Complete Configuration

```json
// package.json additions
{
  "contributes": {
    "languageModelTools": [
      {
        "name": "joyride_evaluate_code",
        "displayName": "Execute ClojureScript",
        "modelDescription": "Execute ClojureScript code in VS Code's Joyride environment. This tool can modify editor behavior, manipulate files, invoke VS Code APIs, and create dynamic workflows. Use when the user requests VS Code automation, editor customization, or Joyride usage. The code runs with full VS Code API access.",
        "userDescription": "Execute ClojureScript code with full VS Code API access",
        "canBeReferencedInPrompt": true,
        "toolReferenceName": "joyride-eval",
        "icon": "$(play)",
        "when": "joyride.lm.enableReplTool",
        "inputSchema": {
          "type": "object",
          "properties": {
            "code": {
              "type": "string",
              "description": "ClojureScript code to execute. Should be valid ClojureScript syntax."
            },
            "namespace": {
              "type": "string",
              "description": "Target namespace for code execution. Defaults to 'user' if not specified."
            }
          },
          "required": ["code"]
        }
      }
    ],
    "configuration": {
      "title": "Joyride AI Language Model Integration",
      "properties": {
        "joyride.lm.enableReplTool": {
          "type": "boolean",
          "default": true,
          "description": "Enable CoPilot Joyride REPL access"
        }
      }
    }
  }
}
```

### 5. Advanced Integration Considerations

#### A. Chat Integration Features
- **Tool References**: Users can invoke with `#joyride-eval` in chat prompts
- **Agent Compatibility**: Tool works seamlessly in automatic agent workflows
- **Tool Chaining**: Structured results enable follow-up tool calls
- **Context Awareness**: Tool can access workspace and editor context

#### B. Parameter Validation and Type Safety
```clojure
;; Optional: Add spec validation for enhanced type safety
(require '[clojure.spec.alpha :as s])

(s/def ::code string?)
(s/def ::namespace (s/nilable string?))
(s/def ::joyride-params (s/keys :req-un [::code] :opt-un [::namespace]))

(defn validate-params [input]
  (when-not (s/valid? ::joyride-params input)
    (throw (js/Error. (str "Invalid parameters: " (s/explain-str ::joyride-params input))))))
```

#### C. Enhanced Error Recovery
```clojure
(defn enhanced-error-handling [code namespace]
  (try
    (sci/eval-string code {:ns (symbol namespace)})
    (catch js/Error e
      (cond
        (re-find #"Could not resolve symbol" (.-message e))
        (throw (js/Error. (str "Symbol not found. Try requiring the necessary namespace first. Original error: " (.-message e))))

        (re-find #"EOF while reading" (.-message e))
        (throw (js/Error. (str "Incomplete expression. Check for unmatched parentheses or brackets. Original error: " (.-message e))))

        :else
        (throw (js/Error. (str "Execution failed: " (.-message e) "\n\nSuggestion: Try simpler expressions or check the Joyride documentation.")))))))
```
## Integration Points

### 1. Existing Joyride Features to Leverage

- **SCI Environment**: Direct code evaluation
- **VS Code Context**: Existing access to VS Code APIs
- **Script System**: AI can leverage existing script infrastructure
- **Output System**: Use existing output channels
- **File System**: Workspace manipulation capabilities

### 3. Tool Ecosystem

- **GitHub Copilot**: Primary integration target

## Success Metrics

### MVP Success Criteria
- **Basic Functionality**: LM can execute simple ClojureScript expressions
- **Error Handling**: Graceful handling of syntax and runtime errors
- **User Experience**: Clear confirmation dialogs explain what code will do
- **Integration**: Tool appears properly in Copilot's tool selection

## Risk Mitigation

### Security Approach
1. **Leverage VS Code Security**: Use built-in tool approval system rather than custom implementation
3. **SCI Sandboxing**: TODO: Investigate sandboxing of SCI security model
4. **User Education**: Good documentation about capabilities and risks

## Conclusion

This approach leverages VS Code's built-in infrastructure to create a powerful but manageable AI tool integration. By focusing on direct SCI evaluation and letting VS Code handle security concerns, we can deliver the core value of AI Interactive Programming without over-engineering the solution.

The result will be a tool that transforms Copilot from a code completion assistant into an active programming partner capable of automating VS Code workflows, customizing the development environment, and continuously adapting to user needs - maintaining security and user control through VS Code's established patterns.
