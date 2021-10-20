import com.palantir.gradle.graal.ExtractGraalTask
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.0-RC"
    id("com.palantir.graal") version "0.9.0"
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
    graalVersion("21.3.0") // When updating this, remember also to update the cache key with the same value in .github/workflows/gradle.yml
    //javaVersion("17") // Doesn't currently work as the plugin only accepts <= 16 at this moment
    (javaVersion as Property<String>).set("17") // Workaround for the above problem
    mainClass("com.github.bjornvester.gww.AppKt")
    outputName("gw")

    if (getCurrentOperatingSystem().isWindows && windowsVsVarsPath.get() == "") {
        // Needed for some installations for Windows
        windowsVsEdition("BuildTools")
    }

    option("--install-exit-handlers")
    option("--no-fallback")
    option("-R:MinHeapSize=2m")
    option("-R:MaxHeapSize=10m")
    option("-R:MaxNewSize=1m")
    option("-H:IncludeResources='org/gradle/build-receipt\\.properties\$'") // Functionally unused, but still read and required by the GradleVersion class
}

tasks.withType<Wrapper> {
    gradleVersion = "7.2"
    distributionType = Wrapper.DistributionType.BIN
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("extractGraalTooling")

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    doFirst {
        // Use the GraalVM distribution for compiling the Kotlin classes
        if (getCurrentOperatingSystem().isMacOsX) {
            // For some reason, GraalVM doesn't work on MacOS for compiling the Kotlin source code
            // So in that case, use the same version as Gradle
            // Of cause, we still use GraalVM to generate the final native image
            if (JavaVersion.current() < JavaVersion.VERSION_17) {
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
