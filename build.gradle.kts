import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import java.nio.file.Files
import java.nio.file.Path

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("com.palantir.graal") version "0.12.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(gradleApi())
}

graal {
    // See https://github.com/palantir/gradle-graal for options
    graalVersion("22.3.2") // When updating this, remember also to update the cache key with the same value in .github/workflows/gradle.yml
    javaVersion("17")
    mainClass("com.github.bjornvester.gww.AppKt")
    outputName("gw")

    if (getCurrentOperatingSystem().isWindows) {
        if (windowsVsVarsPath.get() == "") {
            // The plugin does not support newer versions of Visual Studio out-of-the-box, so add the information explicitly
            windowsVsVersion("2022")
            windowsVsEdition("BuildTools")
            if (windowsVsVarsPath.get() == "") {
                windowsVsEdition("Enterprise")
                if (windowsVsVarsPath.get() == "") {
                    var expectedPath = Path.of("C:\\Program Files\\Microsoft Visual Studio")
                    if (!Files.exists(expectedPath)) {
                        expectedPath = Path.of("C:\\Program Files (x86)\\Microsoft Visual Studio")
                    }
                    if (Files.exists(expectedPath) && Files.isDirectory(expectedPath)) {
                        logger.warn("VS path not found. Content: {}", Files.list(expectedPath))
                    } else {
                        logger.warn("VS path not found.")
                    }

                    throw GradleException("Unable to locate a MSVS path - please check if the edition is supported")
                }
            }
        }

        logger.info("Using MSVS path: " + windowsVsVarsPath.get())
    }

    option("--install-exit-handlers")
    option("--no-fallback")
    option("-R:MinHeapSize=2m")
    option("-R:MaxHeapSize=10m")
    option("-R:MaxNewSize=1m")
    option("-H:-SpawnIsolates") // Reduces image size
    option("-H:-UseServiceLoaderFeature") // Reduces image size
    option("-H:IncludeResources=org/gradle/build-receipt\\.properties\$") // Functionally unused, but still read and required by the GradleVersion class
}

tasks.withType<Wrapper> {
    gradleVersion = "latest"
    distributionType = Wrapper.DistributionType.BIN
}

kotlin {
    jvmToolchain {
        version = JavaVersion.VERSION_17
    }
}

val run by tasks.registering(JavaExec::class) {
    description = "Runs the application with the argument '--version'. Only used for quick testing."
    mainClass.set("com.github.bjornvester.gww.AppKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--version")
}

val zipDistribution by tasks.registering(Zip::class) {
    description = "Zips the native image executable"
    val nativeImageTask = tasks.getByName("nativeImage")
    dependsOn(nativeImageTask)
    archiveFileName.set("gw-${getCurrentOperatingSystem().toFamilyName()}.zip")
    from(nativeImageTask.outputs.files.singleFile.absolutePath)
    from("$projectDir/LICENSE")
    from("$projectDir/src/main/dist/README.txt")
}
