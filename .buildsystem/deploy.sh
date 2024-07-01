#!/usr/bin/env bash
cd `dirname $0`/..

TARGETS=""
for i in ":core" ":vendor:vendor-android:base" ":vendor:vendor-android:ddmlib" ":marathon-gradle-plugin" ":report:execution-timeline" ":report:html-report"; do
  TARGETS="$TARGETS $i:publishDefaultPublicationToMavenLocal"
done

if [ ! -z "$TRAVIS_TAG" ]
then
    echo "on a tag -> deploy release version $TRAVIS_TAG"
    ./gradlew $TARGETS -PreleaseMode=RELEASE
else
    echo "not on a tag -> deploy snapshot version"
    ./gradlew $TARGETS -PreleaseMode=SNAPSHOT
fi
