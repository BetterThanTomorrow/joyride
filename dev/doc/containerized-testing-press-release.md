# Breaking the VS Code Instance Lock: Joyride Introduces Containerized Testing for Extension Developers

*Amazon Working Backwards Press Release*

**STOCKHOLM, Sweden - [Future Date]** - Today, the Joyride project announced containerized integration testing, a breakthrough solution that eliminates the "VS Code instance lock" problem that has plagued extension developers for years. The new Docker Compose-based testing environment allows developers to run comprehensive end-to-end tests while simultaneously using VS Code Insiders for active development, ending the frustrating cycle of having to choose between productive coding and thorough testing.

## The Problem: Choose Your Pain

Extension developers using VS Code's `@vscode/test-electron` framework have long faced an impossible choice: either stop all development work to run integration tests, or skip testing altogether. The underlying issue stems from VS Code's architectural limitation that prevents multiple instances from running simultaneously, forcing developers into a productivity-killing workflow of constant context switching.

"I've been developing Calva and Joyride extensions for years, and this has been my daily frustration," said Peter Strömberg, creator of Joyride and Calva. "I want to use VS Code Insiders for my development because it's like having a canary in the coal mine for breaking changes. But the moment I need to run integration tests, I have to completely stop my development workflow. It's particularly painful when you're in a deep REPL session working through complex problems - suddenly you have to context switch just to verify your code works."

The problem becomes even more acute for teams practicing continuous integration, where local testing should mirror CI environments but instead creates an entirely different developer experience.

## The Solution: True Isolation Through Containers

Joyride's new containerized testing solution leverages Docker Compose to create a completely isolated Linux environment that runs VS Code Insiders with full GitHub Copilot support. The solution seamlessly integrates with existing test infrastructure through a new `npm run integration-test-containerized` command that accepts all the same arguments as the current testing framework.

"This is exactly what we needed," said Strömberg. "The containerized tests run in an Ubuntu environment with xvfb, which is identical to our CI setup, so we finally have true dev/CI parity. Meanwhile, I can keep my development VS Code instance running with all my REPL connections and productivity tools active. It's the best of both worlds."

The solution builds on modern GitHub Actions runner images and established CI patterns, ensuring that the containerized environment matches production testing infrastructure. By using volume mounts for the project files, the system maintains the fast iteration cycles developers expect while providing complete isolation.

## Key Benefits

**Complete Development/Testing Isolation**: Run integration tests in a separate Linux container while maintaining your productive macOS/Windows development environment.

**True Local Testing**: Tests run against your actual project files on the host machine via volume mounts, making the experience nearly identical to local testing - just without the VS Code instance conflict.

**CI/Local Parity**: Tests run in the same Ubuntu+xvfb environment as GitHub Actions, eliminating "works on my machine" issues.

**Zero VS Code Conflicts**: The containerized VS Code instance is completely separate from your development instance, with full GitHub Copilot support and extension marketplace access.

**Minimal Migration**: Existing test scripts work unchanged - the new system is a drop-in replacement that accepts all current command-line arguments.

**Cross-Platform Support**: Works identically on macOS, Windows, and Linux development machines with just Docker and Docker Compose as prerequisites.

## Real-World Impact

The Joyride project immediately adopted the new testing approach for their development workflow. "We're already using this for all our integration testing," reported Strömberg. "The developer experience is transformative - I can have a complex debugging session running in my main VS Code window while tests execute in the background. No more choosing between productivity and quality."

The solution has proven particularly valuable for the Calva organization's projects, which maintain multiple VS Code extensions. "We're already working on porting this approach to our other extension projects," noted Strömberg. "The pattern is so clean and the benefits so clear that it's becoming our standard testing infrastructure."

## Getting Started

The new containerized testing requires only Docker and Docker Compose as prerequisites. Developers can immediately upgrade their testing workflow with:

```bash
# Install Docker and Docker Compose (if not already installed)
# Then run containerized tests with all existing arguments:
npm run integration-test-containerized

# Or with specific arguments (all passed through):
npm run integration-test-containerized -- --joyride-vsix ./my-extension.vsix
```

The Docker Compose configuration automatically handles:
- Ubuntu environment with Node.js, Java, and Clojure toolchain
- VS Code Insiders download and caching
- Extension installation and workspace setup
- xvfb display server for headless operation
- Volume mounting for project files and caching

"The setup is intentionally minimal," explained Strömberg. "We assume Docker is working and focus on making the testing experience seamless. The first run downloads and caches everything, then subsequent runs are fast because VS Code and dependencies are cached on the host volume."

## Technical Innovation

The solution leverages modern CI runner images that include the complete toolchain needed for Clojure/ClojureScript development. By using the same base images as GitHub Actions, the system ensures perfect environment consistency between local development and automated CI pipelines.

The Docker Compose approach provides several technical advantages:
- **Declarative infrastructure**: The testing environment is defined as code
- **Efficient caching**: VS Code downloads and extension installations are cached between runs
- **Volume optimization**: Project files are mounted for fast iteration without container rebuilds
- **Resource isolation**: Test execution doesn't impact development machine performance

## Availability

The containerized testing solution is immediately available in the Joyride project repository. The implementation includes complete Docker Compose configuration, updated npm scripts, and comprehensive documentation for extension developers looking to adopt similar approaches.

Future development will focus on expanding the solution to additional extension projects within the Calva organization and publishing best practices for the broader VS Code extension development community.

## About Joyride

Joyride makes VS Code hackable through ClojureScript scripting, powered by SCI (Small Clojure Interpreter). The project enables developers to customize and extend VS Code using the same functional programming principles that make Emacs uniquely powerful. Joyride is part of the broader Calva ecosystem supporting Clojure development in VS Code.

---

*This press release describes the intended future state of the containerized testing solution and represents our development target.*