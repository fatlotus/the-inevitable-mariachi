#!/usr/bin/env bash

$PLATFORM=macosx

java -classpath 'classes:lib/*' -Djava.library.path=ext/$PLATFORM Sketch