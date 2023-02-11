#!/usr/bin/env bash

# Compare two joyride.js bundles, size and load time

script_dir=$(dirname "$0")
bundle_check_script="$script_dir/bundle-load-check.js"
extension1=$1
extension2=$2
runs=${3:-10}

# Check if both extensions exist
if [ ! -d "$extension1" ] || [ ! -d "$extension2" ]; then
  echo "Usage: bundle-compare.sh <extension-1-dir> <extension-2-dir> [runs]]"
  echo "Both extension directories must exist."
  exit 1
fi

function bundle_size_and_load_time() {
  node "$bundle_check_script" "$1" "$runs" | awk '/bundle size/ { size=$4 } /average load time/ { time=$4 } END { print size " " time }'
}

read -r ext1_size ext1_time <<< $(bundle_size_and_load_time "$extension1")
read -r ext2_size ext2_time <<< $(bundle_size_and_load_time "$extension2")

function report_size_and_load_time() {
  printf "  1. %s\n     Size: %s bytes\n     Load time average: %.2f ms\n" "$1" "$2" "$3"
}
echo "Comparison of:"
report_size_and_load_time "$extension1" "$ext1_size" "$ext1_time"
report_size_and_load_time "$extension2" "$ext2_size" "$ext2_time"
echo "Loaded the bundles $runs times each."

size_diff=$(bc -le "$ext2_size - $ext1_size")
size_diff_percent=$(bc -le "scale=1; $size_diff * 100.0 / $ext1_size")
echo "Size difference: $size_diff $4 ($size_diff_percent%)"

time_diff=$(bc -le "scale=2; $ext2_time - $ext1_time")
time_diff_percent=$(bc -le "scale=1; $time_diff * 100.0 / $ext1_time")
echo "Load time difference: $time_diff $4 ($time_diff_percent%)"

