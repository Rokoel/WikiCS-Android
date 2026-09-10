#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mkdir -p test-output/classes
java com.sun.tools.javac.Main -encoding UTF-8 -d test-output/classes app/src/main/java/site/wikics/reader/core/*.java tests/CoreTests.java
java -cp test-output/classes CoreTests
