import com.palantir.gradle.graal.ExtractGraalTask
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.10"
    id("com.palantir.graal") version "0.7.1"
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

graal {
    // See https://github.com/palantir/gradle-graal for options
    graalVersion("20.2.0") // When updating this, remember also to update the cache key with the same value in .github/workflows/gradle.yml
    javaVersion("11")
    mainClass("com.github.bjornvester.gww.AppKt")
    outputName("gw")
    if (getCurrentOperatingSystem().isWindows && getWindowsVsVarsPath().get() == "") {
        // Needed for some installations for Windows
        windowsVsEdition("BuildTools")
    }
    option("--no-fallback")
}

tasks.withType<Wrapper> {
    gradleVersion = "6.6"
    distributionType = Wrapper.DistributionType.BIN
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    doFirst {
        // Use the GraalVM distribution for compiling the Kotlin classes
        if (getCurrentOperatingSystem().isMacOsX) {
            // For some reason, GraalVM doesn't work on MacOS for compiling the Kotlin source code
            // So in that case, use the same version version as Gradle
            // Of cause, we still use GraalVM to generate the final native image
            if (JavaVersion.current() < JavaVersion.VERSION_11) {
                throw GradleException("This build must be run with Java 11 or higher")
            }
        } else {
            val extractGraalTask = tasks.getByName("extractGraalTooling", ExtractGraalTask::class)
            kotlinOptions.jdkHome = extractGraalTask.outputs.files.singleFile.absolutePath
        }
    }
}

val run by tasks.registering(JavaExec::class) {
    description = "Runs the application with the argument '--version'. Only used for quick testing."
    main = "com.github.bjornvester.gww.AppKt"
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
