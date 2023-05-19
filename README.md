# gradle-wrapper-wrapper

![example workflow name](https://github.com/bjornvester/gradle-wrapper-wrapper/workflows/Build/badge.svg)

This project facilitates executing the Gradle wrapper script in a multi-project.

It is similar to [gng](https://github.com/gdubw/gng) (which only works in Bash) and [gw](https://github.com/srs/gw) (which is written in Go and has no released
artifacts).

Please see the [gng](https://github.com/gdubw/gng) project for a good explanation of some shortcomings of working with the normal wrapper script in a
multi-project. Also see [Gradle issue #1368](https://github.com/gradle/gradle/issues/1368) for voting on getting the feature built directly into Gradle.

## Using the wrapper-wrapper

Download the executable from the [release page](https://github.com/bjornvester/gradle-wrapper-wrapper/releases) and put it in a folder of your choice. Add the
folder to your PATH variable.

You can now type `gw` anywhere in a Gradle multi-project structure to invoke the wrapper script found in the root, just like you would type `gradlew`
or `gradle`.

For instance: `gw build` or `gw test`.

It has a some fall-back logic in case there is no wrapper script in the project. The search logic for finding a suitable Gradle executable is as follows:

1. Use the `gradlew` script in the current directory if present.
2. Search in parent directories for a `gradlew` script until found or until it reaches the root.
3. Use `gradle` on the PATH variable if present.
4. Use `gradle` from a previously downloaded Wrapper distribution in `[USER_HOME]/.gradle/wrapper/dists`. It will choose the latest version if multiple
   distributions have been downloaded.

## Build the project from source

The project uses Gradle for building the wrapper, and GraalVM for creating a native executable.

### Prerequisites

The requirements for Gradle is just a compatible JDK:

1. Install a [Java Development Kit](https://adoptium.net/) version 17. Note that you can use any version that is compatible with the version of Gradle used
   by the project (so JDK 11 or 20 should be fine too, though not tested by me).
2. Set the environment variable `JAVA_HOME` to point to where you installed the JDK (if this isn't done already).

While GraalVM itself is downloaded automatically as part of the build, it requires a local toolchain. For Linux, you need glibc and for Windows a version of
MSVS. See the [GraalVM installation guide](https://www.graalvm.org/reference-manual/native-image/) for help.

#### Windows

For Windows specifically, the build is configured for a particular version of MSVS.
Download and install Tools for Visual Studio from https://visualstudio.microsoft.com/downloads/?q=build+tools.
The version needs to correspond to the one specified in the build.gradle.kts file.
During installation, chose the "Desktop development with C++" component.

### Building it

Checkout the project. Then run `gradlew nativeImage` from the root. It will build a distribution for your local platform in `build/graal/` called `gw`
or `gw.exe` (depending on the platform).

### Contributions

Contributions are welcome.

If you create a PR, GitHub will automatically try to build it.

Note that there are no unit tests for the project at the moment though.
