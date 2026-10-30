// Usage: ./gradlew -p .github/compat writeClasspath -Papi=io.papermc.paper:paper-api:<version>
plugins {
    java
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation(providers.gradleProperty("api").get())
}

tasks.register("writeClasspath") {
    val classpath: FileCollection = configurations.runtimeClasspath.get()
    val output = layout.buildDirectory.file("api-classpath.txt")
    inputs.files(classpath)
    outputs.file(output)
    doLast {
        output.get().asFile.writeText(classpath.files.joinToString(File.pathSeparator))
    }
}
