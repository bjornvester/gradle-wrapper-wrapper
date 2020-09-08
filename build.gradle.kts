plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.0"
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
    //windowsVsEdition("BuildTools") // Needed for some installations for Windows. Not sure why.
    option("--no-fallback")
}

tasks.withType<Wrapper> {
    gradleVersion = "6.6"
    distributionType = Wrapper.DistributionType.ALL
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // The source and target compatibility only functions as a hint to IntelliJ when importing project for selecting the correct JDK
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    doFirst {
        // Use the GraalVM distribution for compiling the Kotlin classes
        val extractGraalTask = tasks.getByName("extractGraalTooling", com.palantir.gradle.graal.ExtractGraalTask::class)
        kotlinOptions.jdkHome = extractGraalTask.outputs.files.singleFile.absolutePath
    }
}

val run by tasks.registering(JavaExec::class) {
    description = "Runs the application with the argument '--version'. Only used for quick testing."
    main = "com.github.bjornvester.gww.AppKt"
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--version")
}
