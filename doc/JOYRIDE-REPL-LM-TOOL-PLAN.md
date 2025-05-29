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

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   GitHub        │    │   VS Code       │    │   Joyride       │
│   Copilot       │◄──►│   LM Tool API   │◄──►│   SCI Direct    │
│   (LLM)         │    │   + Security    │    │   Evaluation    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Tool Call     │    │   Built-in      │    │   VS Code API   │
│   Request       │    │   Approval UI   │    │   Access        │
│                 │    │   & Validation  │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 3. Core Components

#### A. Language Model Tool Registration (`src/joyride/lm_tool.cljs`)
```clojure
(ns joyride.lm-tool
  (:require ["vscode" :as vscode]
            [joyride.sci :as sci]
            [promesa.core :as p]))

(defn execute-code+ [code namespace]
  ;; Direct SCI evaluation - no nREPL complexity
  (sci/eval-string code {:ns (or namespace 'user)}))

(defn register-lm-tool! []
  ;; Register with VS Code's LanguageModelTool API
  (vscode/lm.registerTool
   #js {:name "joyride_repl_execute"
        :description "Execute ClojureScript code in VS Code's Joyride environment"
        :parametersSchema #js {:type "object"
                              :properties #js {:code #js {:type "string"}
                                              :namespace #js {:type "string"}}}
        :prepareInvocation (fn [request token]
                            ;; VS Code handles confirmation UI
                            )
        :invoke (fn [request stream token]
                 ;; Direct execution via existing SCI
                 )}))
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
;; 1. Create simple tool that calls existing SCI evaluation
(defn invoke-tool [request stream token]
  (p/let [code (.-code (.-parameters request))
          namespace (.-namespace (.-parameters request))
          result (sci/eval-string code {:ns (or namespace 'user)})]
    (stream.textContent (pr-str result))))

;; 2. Register with VS Code's tool system (not Copilot directly)
(vscode/lm.registerTool #js {:invoke invoke-tool ...})
```

**Success Criteria**:
- LM can execute ClojureScript via VS Code's tool system

## Technical Implementation Details

### 1. Minimal File Structure

```
src/joyride/
├── lm_tool.cljs           # Main tool implementation
└── lm_context.cljs        # Optional context helpers
```

### 2. Core Implementation

```clojure
;; src/joyride/lm_tool.cljs
(ns joyride.lm-tool
  (:require ["vscode" :as vscode]
            [joyride.sci :as sci]
            [promesa.core :as p]))

(defn prepare-invocation [request token]
  (let [code (.-code (.-parameters request))
        namespace (.-namespace (.-parameters request))]
    #js {:content (str "Execute ClojureScript:\n\n" code
                      "\n\nIn namespace: " (or namespace "user"))}))

(defn invoke-tool [request stream token]
  (let [code (.-code (.-parameters request))
        namespace (.-namespace (.-parameters request))]
    (try
      (let [result (sci/eval-string code {:ns (symbol (or namespace "user"))})]
        (stream.textContent (pr-str {:status :success :result result})))
      (catch js/Error e
        (stream.textContent (pr-str {:status :error :error (.-message e)}))))))

(defn register-tool! []
  (vscode/lm.registerTool
   #js {:name "joyride_repl_execute"
        :description "Execute ClojureScript code in VS Code's Joyride environment"
        :parametersSchema #js {:type "object"
                              :properties #js {:code #js {:type "string"
                                                         :description "ClojureScript code to execute"}
                                              :namespace #js {:type "string"
                                                             :description "Target namespace"}}}
        :prepareInvocation prepare-invocation
        :invoke invoke-tool}))
```

### 3. Extension Integration

```clojure
;; Add to src/joyride/extension.cljs
(when (exists? js/vscode.lm)
  (require '[joyride.lm-tool :as lm-tool])
  (lm-tool/register-tool!))
```

### 4. Simple Configuration

```json
// package.json additions
{
  "contributes": {
    "configuration": {
      "title": "Joyride AI Integration",
      "properties": {
        "joyride.ai.enableReplTool": {
          "type": "boolean",
          "default": true,
          "description": "Enable AI tool integration"
        }
      }
    }
  }
}
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
