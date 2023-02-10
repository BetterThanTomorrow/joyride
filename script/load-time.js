const fs = require("fs");
const path = require("path");

const joyrideDir = process.argv[2];
if (!joyrideDir) {
  console.error("Usage: node load-time.js <joyride-extension-dir>");
  process.exit(1);
}
const runs = process.argv[3] || 10;

const joyrideJs = joyrideDir + "/out/joyride.js";
if (!fs.existsSync(joyrideJs)) {
  console.error("joyride.js not found, exiting");
  process.exit(1);
}

const mockVscodeNodeModulesSrcDir = path.join(__dirname, "mock-vscode/node_modules");
const fakeVscodeNodeModulesDestDir = path.join(joyrideDir, '/out/node_modules');

fs.cpSync(mockVscodeNodeModulesSrcDir, fakeVscodeNodeModulesDestDir, { recursive: true });

const start = performance.now();
require(joyrideJs); // First require is slower
const end = performance.now();
console.log("initial load joyride.js", end - start, "ms");
delete require.cache[require.resolve(joyrideJs)];

let totalTime = 0;
for (let i = 0; i < runs; i++) {
  const start = performance.now();
  require(joyrideJs);
  const end = performance.now();
  const time = end - start;
  console.log("load joyride.js", time, "ms");
  totalTime += time;
  delete require.cache[require.resolve(joyrideJs)];
}
console.log("average load time", totalTime / runs, "ms");

fs.rmSync(fakeVscodeNodeModulesDestDir, { recursive: true });
