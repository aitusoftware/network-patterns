#!/bin/bash

./gradlew clean bundleJar

cp scripts/*.sh build/libs

cd build/libs

jar cf ../../dist.zip ./*
cd ..

echo "Created archive dist.zip"