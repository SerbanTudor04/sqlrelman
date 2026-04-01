plugins {
    id("java")
    id("application")
}

group = "ro.serbantudor04"
version = "1.0.1"

repositories {
    mavenCentral()
}

application {
    mainClass.set("ro.serbantudor04.sqlrelman.SqlRelMan")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "-Djline.log.level=OFF",
        "-Dorg.jline.terminal.jna=false"
    )
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    implementation("commons-io:commons-io:2.16.1")
    implementation("org.jline:jline-terminal:3.26.3")
    implementation("org.jline:jline-reader:3.26.3")
    implementation("org.jline:jline-terminal-ffm:3.26.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-Djline.log.level=OFF",
        "-Dorg.jline.terminal.jna=false"
    )
    standardInput = System.`in`
}

// ─── Fat JAR ────────────────────────────────────────────────────────────────

val fatJar = tasks.register<Jar>("fatJar") {
    group = "distribution"
    description = "Assembles a self-contained executable JAR with all dependencies."
    archiveBaseName.set("sqlrelman")
    archiveClassifier.set("")   // no "-all" suffix — output is build/libs/sqlrelman-1.0.jar
    archiveVersion.set(project.version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Main-Class"       to "ro.serbantudor04.sqlrelman.SqlRelMan",
            "Implementation-Title"   to "sqlrelman",
            "Implementation-Version" to project.version
        )
    }

    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

// ─── Install task ────────────────────────────────────────────────────────────
// Copies the fat JAR + wrapper script to ~/.local/bin  (or /usr/local/bin with sudo)

tasks.register("install") {
    group = "distribution"
    description = "Installs sqlrelman to ~/.local/bin (run with -Psystem to install to /usr/local/bin)."
    dependsOn(fatJar)

    doLast {
        val system  = project.hasProperty("system")
        val binDir  = if (system) file("/usr/local/bin") else file("${System.getProperty("user.home")}/.local/bin")
        val libDir  = if (system) file("/usr/local/lib/sqlrelman") else file("${System.getProperty("user.home")}/.local/lib/sqlrelman")

        binDir.mkdirs()
        libDir.mkdirs()

        // Copy JAR
        val jar = fatJar.get().archiveFile.get().asFile
        val destJar = file("${libDir}/sqlrelman.jar")
        jar.copyTo(destJar, overwrite = true)
        println("  JAR  → $destJar")

        // Write wrapper script
        val wrapper = file("${binDir}/sqlrelman")
        wrapper.writeText("""
            #!/usr/bin/env sh
            exec java \
              --enable-native-access=ALL-UNNAMED \
              -Djline.log.level=OFF \
              -Dorg.jline.terminal.jna=false \
              -jar "${libDir}/sqlrelman.jar" "$@"
        """.trimIndent() + "\n")
        wrapper.setExecutable(true)
        println("  BIN  → $wrapper")
        println("")
        println("  Done! Make sure ${binDir} is on your PATH.")
        if (!system) println("  Add this to your shell profile if needed:")
        if (!system) println("    export PATH=\"\$HOME/.local/bin:\$PATH\"")
    }
}

// ─── Uninstall task ──────────────────────────────────────────────────────────

tasks.register("uninstall") {
    group = "distribution"
    description = "Removes sqlrelman installed by the install task."

    doLast {
        val system = project.hasProperty("system")
        val binDir = if (system) file("/usr/local/bin") else file("${System.getProperty("user.home")}/.local/bin")
        val libDir = if (system) file("/usr/local/lib/sqlrelman") else file("${System.getProperty("user.home")}/.local/lib/sqlrelman")

        listOf(file("${binDir}/sqlrelman"), libDir).forEach { f ->
            if (f.exists()) { f.deleteRecursively(); println("  Removed $f") }
            else            { println("  Not found: $f") }
        }
    }
}