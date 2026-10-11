#!/bin/bash
set -e
[ -f ci/blobs.txt ] || exit 0
while read -r target n; do
  mkdir -p "$(dirname "$target")"; : > "$target"
  for i in $(seq -f "%03g" 0 $((n-1))); do cat "_blobs/$target.part$i" >> "$target"; done
  echo "assembled $target"
done < ci/blobs.txt
