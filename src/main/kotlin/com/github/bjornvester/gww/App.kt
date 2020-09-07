package com.github.bjornvester.gww

import java.io.File
import java.lang.Runtime.Version
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val gradleExecutablePath = findGradleExecutable()

    if (gradleExecutablePath == null) {
        System.err.println("[GW] Could not find a Gradle executable on the system.")
        exitProcess(1)
    } else {
        val fullPath = gradleExecutablePath.toAbsolutePath().toString()
        ProcessBuilder(fullPath, *args)
                .inheritIO()
                .start()
                .waitFor()
    }
}

private fun findGradleExecutable(): Path? {
    return findGradleWrapper() ?: findGradleFromPath() ?: findGradleFromWrapperDist()
}

private fun findGradleWrapper(): Path? {
    var gradlePath: Path?
    var currentDirectory = Paths.get(System.getProperty("user.dir"))

    do {
        gradlePath = currentDirectory.resolve(getGradleWrapperName())
        if (!Files.isExecutable(gradlePath)) {
            gradlePath = null
        }
        currentDirectory = currentDirectory.parent
    } while (gradlePath == null && currentDirectory != null)

    return gradlePath
}

private fun findGradleFromPath(): Path? {
    return System.getenv("PATH")?.split(File.pathSeparator)?.mapNotNull { path ->
        try {
            Paths.get(path).resolve(getGradleName())
        } catch (ignore: InvalidPathException) {
            // The path could not be resolved. Skip it.
            null
        }
    }?.find { Files.isExecutable(it) }
}

/**
 * Finds the path to the executable of a Gradle wrapper distribution.
 * If there are multiple distributions, the one with the highest version number is selected.
 * If there is both an "all" and a "bin" variant of the same highest version, it is not defined which of them will be returned.
 */
private fun findGradleFromWrapperDist(): Path? {
    // An example of the directory containing the executable could be: [USER]/.gradle/wrapper/dists/gradle-6.6-bin/dflktxzwamd4bv66q00iv4ga9/gradle-6.6/bin
    var distsPath = Paths.get(System.getProperty("user.home")).resolve(".gradle/wrapper/dists")
    var executablesMap: MutableMap<Version, Path> = mutableMapOf() // Gradle version to executable path

    if (Files.isDirectory(distsPath)) {
        Files.list(distsPath).forEach { distPath ->
            // The distribution path should have a single folder inside (with an auto-generated name like 'dflktxzwamd4bv66q00iv4ga9'). Resolve it.
            val dirWithZipFile = Files.newDirectoryStream(distPath).toList().singleOrNull()
            if (dirWithZipFile != null && Files.isDirectory(dirWithZipFile)) {
                // The zip folder should also have a single folder inside, but also some other elements (like the actual zip file)
                // Note that, apparently, Gradle regularly cleans the folder containing the unzipped resources if unused, but leaves the zip file alone.
                val realDistPath = Files.newDirectoryStream(dirWithZipFile).toList().singleOrNull { Files.isDirectory(it) }
                if (realDistPath != null) {
                    // Find the version number from the folder name
                    val version = getVersionFromGradlePath(realDistPath)
                    if (version != null) {
                        // Resolve the path to the actual executable file and put it in the map with versions
                        val executablePath = realDistPath.resolve("bin").resolve(getGradleName())
                        if (Files.isExecutable(executablePath)) {
                            executablesMap[version] = executablePath
                        }
                    }
                }
            }
        }
    }

    val executableMapEntry = executablesMap.maxByOrNull { entry -> entry.key }

    if (executableMapEntry != null) {
        // This is mostly a fall-back method. In this case, as the Gradle version is not specified and can be somewhat arbitrary, print out what was found.
        println("[GW] Using Gradle version ${executableMapEntry.key} from path: ${executableMapEntry.value}")
    }

    return executableMapEntry?.value
}

private fun getGradleWrapperName(): String {
    return if (isWindows()) "gradlew.bat" else "gradlew"
}

private fun getGradleName(): String {
    return if (isWindows()) "gradle.bat" else "gradle"
}

private fun isWindows(): Boolean {
    return System.getProperty("os.name").toLowerCase().contains("windows")
}

/**
 * Note that while this returns a java.lang.Runtime.Version object that represents a Java version, it is (so far) compatible with Gradle versions too.
 */
private fun getVersionFromGradlePath(path: Path): Version? {
    val versionString = Regex("gradle-([\\d.]*)").matchEntire(path.fileName.toString())?.groupValues?.last()
    return if (versionString == null) null else Version.parse(versionString)
}
