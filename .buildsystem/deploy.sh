#!/usr/bin/env bash
cd `dirname $0`/..

if [ -z "$SONATYPE_USERNAME" ]
then
    echo "error: please set SONATYPE_USERNAME and SONATYPE_PASSWORD environment variable"
    exit 1
fi

if [ -z "$SONATYPE_PASSWORD" ]
then
    echo "error: please set SONATYPE_PASSWORD environment variable"
    exit 1
fi

if [ -z "$GPG_PASSPHRASE" ]
then
    echo "error: please set GPG_PASSPHRASE environment variable"
    exit 1
fi

TARGETS=":core:publishDefaultPublicationToOSSHRRepository :vendor:vendor-android:publishDefaultPublicationToOSSHRRepository :marathon-gradle-plugin:publishDefaultPublicationToOSSHRRepository :report:execution-timeline:publishDefaultPublicationToOSSHRRepository :report:html-report:publishDefaultPublicationToOSSHRRepository"

if [ ! -z "$TRAVIS_TAG" ]
then
    echo "on a tag -> deploy release version $TRAVIS_TAG"
    ./gradlew $TARGETS -PreleaseMode=RELEASE
else
    echo "not on a tag -> deploy snapshot version"
    ./gradlew $TARGETS -PreleaseMode=SNAPSHOT
fi