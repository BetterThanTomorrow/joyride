// Reports on the the size and the time it takes to load the joyride.js bundle.

const fs = require("fs");
const path = require("path");

const joyrideDir = process.argv[2];
if (!joyrideDir) {
  console.error("Usage: node bundle-load-check.js <joyride-extension-dir> [runs]");
  process.exit(1);
}
const runs = process.argv[3] || 10;

const joyrideJs = joyrideDir + "/out/joyride.js";
if (!fs.existsSync(joyrideJs)) {
  console.error("joyride.js not found, exiting");
  process.exit(1);
}
const stats = fs.statSync(joyrideJs)
console.log("joyride.js bundle size", stats.size, "bytes");  

const mockVscodeNodeModulesSrcDir = path.join(__dirname, "mock-vscode/node_modules");
const fakeVscodeNodeModulesDestDir = path.join(joyrideDir, '/out/node_modules');

fs.cpSync(mockVscodeNodeModulesSrcDir, fakeVscodeNodeModulesDestDir, { recursive: true });

const start = performance.now();
require(joyrideJs); // First require is slower
const end = performance.now();
console.log("initial load joyride.js", end - start, "ms");
delete require.cache[require.resolve(joyrideJs)];

let runTimes = [];
for (let i = 0; i < runs; i++) {
  const start = performance.now();
  require(joyrideJs);
  const end = performance.now();
  const time = end - start;
  console.log("load joyride.js", time, "ms");
  runTimes.push(time);
  delete require.cache[require.resolve(joyrideJs)];
}
console.log("average load time", averageRunTime(runTimes), "ms");

fs.rmSync(fakeVscodeNodeModulesDestDir, { recursive: true });

function averageRunTime(runTimes) {
  if (runTimes.length === 0) {
    return 0;
  }

  let sum = 0;
  for (let i = 0; i < runTimes.length; i++) {
    sum += runTimes[i];
  }
  let average = sum / runTimes.length;
  let threshold = average * 1.5;

  sum = 0;
  let count = 0;
  for (let i = 0; i < runTimes.length; i++) {
    if (runTimes[i] < threshold) {
      sum += runTimes[i];
      count++;
    }
  }

  return sum / count;
}