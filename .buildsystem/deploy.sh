#!/usr/bin/env bash
cd `dirname $0`/..

if [ -n "$TRAVIS_TAG" ]
then
    echo "on a tag -> deploy release version $TRAVIS_TAG"
    release_mode="RELEASE"
else
    echo "not on a tag -> deploy snapshot version"
    release_mode="SNAPSHOT"
fi

./gradlew -PreleaseMode="$release_mode" publishToMavenLocal
