#!/usr/bin/env bash
# Content pipeline for one place: generate -> calibrate -> certify -> report -> export.
# The published levels, certificates and catalog are committed; candidates and calibration
# samples are regenerable and ignored by git.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
place=${1:-1}
from=${2:-1}
to=${3:-50}
runs=${RUNS:-100}
mkdir -p logs
log="logs/content_pipeline-p$(printf '%02d' "$place").log"
exec > >(tee "$log") 2>&1

printf 'Content pipeline: place %d, levels %d..%d, %d runs\n' "$place" "$from" "$to" "$runs"
tools/gw.sh :leveltool:run --args="generate --place $place --from $from --to $to"
levels=$(ls content/levels/p$(printf '%02d' "$place")/*.json | LC_ALL=C sort)
for level in $levels; do
    id=$(basename "$level" .json)
    tools/gw.sh :leveltool:run --args="calibrate --level $id --runs $runs"
    tools/gw.sh :leveltool:run --args="certify --level $id --runs $runs --attempts 30"
done
tools/gw.sh :leveltool:run --args="daily-pool --version 1"
tools/gw.sh :leveltool:run --args="report --output report.md"
tools/gw.sh :leveltool:run --args="validate --strict"
tools/gw.sh :leveltool:run --args="verify-certificates"
tools/gw.sh :leveltool:run --args="export-assets --output app/build/generated/duckAssets"
printf 'Content pipeline OK.\n'
