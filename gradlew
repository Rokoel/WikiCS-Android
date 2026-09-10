#!/usr/bin/env sh
set -eu
APP_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$APP_ROOT"
if [ -n "${JAVA_HOME:-}" ]; then
    WIKICS_JAVA="$JAVA_HOME/bin/java"
else
    WIKICS_JAVA=java
fi
if [ -f "$APP_ROOT/gradle/wrapper/gradle-wrapper.jar" ]; then
    exec "$WIKICS_JAVA" -classpath "$APP_ROOT/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi
exec "$WIKICS_JAVA" "$APP_ROOT/tools/GradleBootstrap.java" "$@"
