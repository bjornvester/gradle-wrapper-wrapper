package com.github.bjornvester.gww

import java.io.File
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.max
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

private tailrec fun findGradleWrapper(currentDirectory: Path? = getCurrentDir()): Path? {
    return if (currentDirectory == null) {
        null
    } else {
        val gradlePath = currentDirectory.resolve(getGradleWrapperName())
        if (Files.isExecutable(gradlePath)) gradlePath else findGradleWrapper(currentDirectory.parent)
    }
}

private fun findGradleFromPath(): Path? {
    val gradleExecutablePath = System.getenv("PATH")?.split(File.pathSeparator)?.mapNotNull { path ->
        try {
            Paths.get(path).resolve(getGradleName())
        } catch (ignore: InvalidPathException) {
            // The path could not be resolved. Skip it.
            null
        }
    }?.find { Files.isExecutable(it) }

    if (gradleExecutablePath != null) {
        // Go down from the "bin" directory to be able to extract the version number from the main directory name
        val gradleVersion = getVersionFromGradlePath(gradleExecutablePath.parent)
        if (gradleVersion != null) {
            println("[GW] Using Gradle version $gradleVersion from path: $gradleExecutablePath")
            return gradleExecutablePath
        }
    }

    return null
}

/**
 * Finds the path to the executable of a Gradle wrapper distribution.
 * If there are multiple distributions, the one with the highest version number is selected.
 * If there is both an "all" and a "bin" variant of the same highest version, it is not defined which of them will be returned.
 */
private fun findGradleFromWrapperDist(): Path? {
    // An example of the directory containing the executable could be: [USER]/.gradle/wrapper/dists/gradle-6.6-bin/dflktxzwamd4bv66q00iv4ga9/gradle-6.6/bin
    val distsPath = getUserHome().resolve(".gradle/wrapper/dists")
    val executablesMap: MutableMap<Version, Path> = mutableMapOf() // Gradle version to executable path

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

    // Find the highest version and get the Gradle executable for that version (if any)
    val executableMapEntry = executablesMap.maxByOrNull { it.key }

    if (executableMapEntry != null) {
        // This is mostly a fall-back method. In this case, as the Gradle version is not specified and can be somewhat arbitrary, print out what was found.
        println("[GW] Using Gradle version ${executableMapEntry.key} from path: ${executableMapEntry.value}")
    }

    return executableMapEntry?.value
}

private fun getCurrentDir() = Paths.get(System.getProperty("user.dir"))

private fun getUserHome() = Paths.get(System.getProperty("user.home"))

private fun getGradleName() = if (isWindows()) "gradle.bat" else "gradle"

private fun getGradleWrapperName() = if (isWindows()) "gradlew.bat" else "gradlew"

private fun isWindows() = System.getProperty("os.name").toLowerCase().contains("windows")

private fun getVersionFromGradlePath(path: Path): Version? {
    val versionString = Regex("gradle-([\\d.]*)").matchEntire(path.fileName.toString())?.groupValues?.last()
    return if (versionString == null) null else Version(versionString)
}

/**
 * Simple version class. Only supports numbers and does not work with text (e.g. "rc-1").
 */
private class Version(val version: String) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        val version1Splits = version.split(".")
        val version2Splits = other.version.split(".")
        val maxLengthOfVersionSplits = max(version1Splits.size, version2Splits.size)
        var comparisonResult = 0

        for (i in 0 until maxLengthOfVersionSplits) {
            val v1 = if (i < version1Splits.size) version1Splits[i].toInt() else 0
            val v2 = if (i < version2Splits.size) version2Splits[i].toInt() else 0
            val compare = v1.compareTo(v2)

            if (compare != 0) {
                comparisonResult = compare
                break
            }
        }

        return comparisonResult
    }
}
