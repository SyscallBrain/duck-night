#!/usr/bin/env bash
# Gradle wrapper for parallel worktrees: sets the toolchain and allows at most two concurrent
# builds on this machine, sharing the same two build slots as Night Fleet.
set -euo pipefail
cd -- "$(git rev-parse --show-toplevel)"
export JAVA_HOME=${JAVA_HOME:-/opt/android-studio/jbr} ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
[[ -f local.properties ]] || printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
# The lock directory is shared with Night Fleet on purpose: both projects draw from the same
# two build slots, so running them together never exceeds the memory budget of the machine.
lockdir=/tmp/nightfleet-gradle-locks
mkdir -p "$lockdir"
while true; do
    for slot in 1 2; do
        exec {fd}>"$lockdir/slot$slot"
        if flock -n "$fd"; then
            # A private daemon registry keeps other projects' `gradlew --stop` from killing our builds.
            ./gradlew --console=plain --max-workers=2 -Dorg.gradle.daemon.registry.base="$HOME/.gradle/ducknight-daemons" "$@"
            exit $?
        fi
        exec {fd}>&-
    done
    sleep 5
done
