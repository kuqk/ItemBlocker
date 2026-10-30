plugins {
    java
    alias(libs.plugins.run.paper)
}

group = "pl.variant"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

// Code that needs Paper-only API lives in its own source set. The main source set compiles
// against the Spigot API, so the compiler guarantees it runs on every supported platform.
val paper: SourceSet = sourceSets.create("paper") {
    compileClasspath += sourceSets.main.get().output
}

dependencies {
    compileOnly(libs.spigot.api)
    "paperCompileOnly"(libs.paper.api)

    testImplementation(libs.paper.api)
    testImplementation(libs.mockbukkit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    // Java 21 bytecode keeps the jar loadable on 1.21.x servers; 26.x servers run Java 25.
    options.release = 21
    options.encoding = Charsets.UTF_8.name()
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.processResources {
    val properties = mapOf("version" to project.version)
    inputs.properties(properties)
    filesMatching("plugin.yml") {
        expand(properties)
    }
}

tasks.jar {
    from(paper.output)
    archiveFileName = "ItemBlocker-${project.version}.jar"
}

tasks.test {
    useJUnitPlatform()
}

tasks.runServer {
    // ./gradlew runServer -Pmc=26.3 starts a different server version.
    minecraftVersion(providers.gradleProperty("mc").getOrElse("1.21.11"))
}
