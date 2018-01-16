#!/bin/bash

./gradlew bundleJar

cp build/libs/network-patterns-all.jar .

jar cf dist.zip ./run_*.sh ./network-patterns-all.jar

echo "Created archive dist.zip"