#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper-main-9.6.0.jar:$APP_HOME/gradle/wrapper/gradle-wrapper-shared-9.6.0.jar"
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
