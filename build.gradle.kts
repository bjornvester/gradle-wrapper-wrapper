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
    graalVersion("20.2.0")
    javaVersion("11")
    mainClass("com.github.bjornvester.gww.AppKt")
    outputName("gw")
    //windowsVsEdition("BuildTools")
    option("--no-fallback")
}

tasks.withType<Wrapper> {
    gradleVersion = "6.6"
    distributionType = Wrapper.DistributionType.ALL
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    /*
    doFirst {
        if(JavaVersion.current() != JavaVersion.VERSION_1_8){
            throw GradleException("This build must be run with java 11")
        }
    }
     */
}

val run by tasks.registering(JavaExec::class) {
    description = "Runs the application with the argument '--version'. Only used for quick testing."
    main = "com.github.bjornvester.gww.AppKt"
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--version")
}
