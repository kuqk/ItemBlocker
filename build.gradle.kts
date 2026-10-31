plugins {
    java
    alias(libs.plugins.run.paper)
}

group = "pl.variant"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/releases/")
}

// Code that needs Paper-only API lives in its own source set. The main source set compiles
// against the Spigot API, so the compiler guarantees it runs on every supported platform.
val paper: SourceSet = sourceSets.create("paper") {
    compileClasspath += sourceSets.main.get().output
}

// Hooks into other plugins are compiled against those plugins in their own source set too, and are
// only loaded once the plugin they need is enabled.
val integrations: SourceSet = sourceSets.create("integrations") {
    compileClasspath += sourceSets.main.get().output
}

// The Paper API is published for Java 21. Compiling with --release 17 against it is fine, the
// server provides the API at runtime.
configurations.named(paper.compileClasspathConfigurationName) {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}

dependencies {
    compileOnly(libs.spigot.api)
    "paperCompileOnly"(libs.paper.api)
    "integrationsCompileOnly"(libs.spigot.api)
    "integrationsCompileOnly"(libs.worldguard.bukkit)
    "integrationsCompileOnly"(libs.placeholderapi)

    testImplementation(libs.paper.api)
    testImplementation(libs.mockbukkit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    // Java 17 bytecode keeps the jar loadable on 1.20 to 1.20.4 servers, which may still run Java 17.
    options.release = 17
    options.encoding = Charsets.UTF_8.name()
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.compileTestJava {
    // MockBukkit needs Java 21.
    options.release = 21
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
    from(integrations.output)
    archiveFileName = "ItemBlocker-${project.version}.jar"
}

tasks.test {
    useJUnitPlatform()
}

tasks.runServer {
    // ./gradlew runServer -Pmc=26.3 starts a different server version.
    minecraftVersion(providers.gradleProperty("mc").getOrElse("1.21.11"))
}
