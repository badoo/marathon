# Marathon
Test runner for Android projects

## Main focus
- **stability** of test execution adjusting for flakiness in the environment and in the tests. 
- **performance** using high parallelization (handling dozens of devices)

## Testing Local Changes
If you want to make a small update and test it locally before pushing a branch, you can follow these steps:
- make sure you have been grated access to the repo
- clone the repo on your local machine and build ```./gradlew build```
- make your changes and verify that the tests pass
- deploy to local maven with ```./gradlew publishToMavenLocal```
- to use the artifact in your other Repo, make sure you set `mavenLocal()` before other repositories in your `pluginManagement` node in settings.gradle
- to check that the deploy stage was successful, check the pom file in your local maven directory
(e.g. `vim ~/.m2/repository/marathon/marathon.gradle.plugin/dev/marathon.gradle.plugin-dev.pom`)  
- note that the artifact name to import will begin with "com.github.badoo.marathon" (e.g. ```implementation("com.github.badoo.marathon:marathon-gradle-plugin:dev")```)  

## Publishing

Marathon can be published to a custom Maven repository, if URL to the repository is provided via `internalMavenUrl` Gradle property.

When publishing to an internal repository, use the publication date as the version, e.g. `2025.07.14`.

```shell
./gradlew build

./gradlew \
  -PinternalMavenUrl=<url> \
  -PinternalMavenUsername=<username> \
  -PinternalMavenPassword=<password> \
  -PVERSION_NAME=<version> \
  publishAllPublicationsToInternalMavenRepository

git tag <version>
git push origin <version>
```

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
