# Joyride Agent Education Strategy

## The Problem
Agents frequently make common mistakes with Joyride that could be prevented with better guidance:
- Common coding errors
- Misunderstanding of the environment
- Not knowing available APIs
- Incorrect usage patterns

## Current State
- Tool descriptions provide basic parameter info
- Classpath information included in `code` parameter
- No comprehensive guidance about Joyride patterns/best practices

## Potential Solutions

### Option A: Enhanced Parameter Descriptions
**Pros**: Immediate impact, no new tools needed
**Cons**: Limited space, can't provide comprehensive guidance
**Approach**: Include mini crash course in `code` parameter description

### Option B: Dedicated README Tool
**Pros**: Comprehensive guidance, can include examples, patterns, best practices
**Cons**: Another tool for agents to discover and use
**Approach**: Create `joyride_get_guidance` or similar tool

### Option C: Hybrid Approach
**Pros**: Best of both worlds
**Cons**: More complex to implement
**Approach**:
- Keep current parameter descriptions focused and clean
- Add README tool with note in main tool description: "For comprehensive guidance, use the joyride_get_guidance tool first"

## README Tool Concept

### Tool Name Options
- `joyride_get_guidance`
- `joyride_readme`
- `joyride_documentation`
- `joyride_help`

### Content Structure
1. **Quick Start**: Basic patterns and examples
2. **Common Patterns**: VS Code API usage, file manipulation, user interaction
3. **Best Practices**: Error handling, async patterns, namespace usage
4. **Troubleshooting**: Common mistakes and how to avoid them
5. **API Reference**: Key functions and their usage

### Integration Strategy
- Reference the README tool in main tool description
- Keep parameter descriptions focused on their specific purpose
- README tool provides the comprehensive guidance

## Recommendation
Start with **Option C (Hybrid)**:
1. Clean up current parameter descriptions (focus on their specific purpose)
2. Design and implement README tool
3. Reference README tool in main tool description
4. Iterate based on agent usage patterns

## Next Steps
1. Finish current parameter description improvements
2. Design README tool specification
3. Implement README tool
4. Update main tool description to reference README tool
5. Monitor and iterate based on usage
