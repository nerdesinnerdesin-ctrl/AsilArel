#!/bin/sh
# Uses javac if present, otherwise the JDK compiler module bundled in the JRE.
set -e
cd "$(dirname "$0")"; rm -rf out; mkdir out
if command -v javac >/dev/null 2>&1; then javac -d out $(find src -name '*.java')
else java -m jdk.compiler/com.sun.tools.javac.Main -d out $(find src -name '*.java'); fi
java -cp out com.plakoto.engine.RulesTest
