[![Build Status](https://travis-ci.org/Malinskiy/marathon.svg?branch=develop)](https://travis-ci.org/Malinskiy/marathon)
[![codecov](https://codecov.io/gh/malinskiy/marathon/branch/develop/graph/badge.svg)](https://codecov.io/gh/malinskiy/marathon)
[![Slack](https://img.shields.io/badge/slack-chat-green.svg?logo=slack&longCache=true&style=flat)](https://bit.ly/2LLghaW)

# Marathon
Cross-platform test runner written for Android and iOS projects

## Main focus
- **stability** of test execution adjusting for flakiness in the environment and in the tests. 
- **performance** using high parallelization (handling dozens of devices)

## Documentation
Please check the official [documentation](https://malinskiy.github.io/marathon/) for installation, configuration and more

<br />

### Testing Local Changes
If you want to make a small update and test it locally before pushing a branch, you can follow these steps:
- make sure you have been grated access to the repo
- clone the repo on your local machine and do a build ```./gradlew clean assemble```
- make your changes and verify that the tests pass
- deploy to local maven with ```./gradlew publishToMavenLocal -PreleaseMode=SNAPSHOT```
- to use the artifact in your other Repo, make sure you set `mavenLocal()` before other repositories in your buildScript node in settings.gradle
- to check that the deploy stage was successful, check the pom file in your local maven directory
(e.g. `vim ~/.m2/repository/marathon/marathon.gradle.plugin/0.5.4-SNAPSHOT/marathon.gradle.plugin-0.5.4-SNAPSHOT.pom`)  
- note that the artifact name to import will begin with "com.malinskiy.marathon" (e.g. ```implementation "com.malinskiy.marathon:marathon-gradle-plugin:0.5.4-SNAPSHOT"```) 
- final note: make sure you **don't commit** your changes to Versions.kt 

License
-------

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
