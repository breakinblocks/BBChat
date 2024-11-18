@file:Suppress("PropertyName")

val night_config_version: String by project
val jda_version: String by project

plugins {
    `java-library`
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
}

base.archivesName.set("bbchat-core")

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

dependencies {
    implementation("org.jetbrains:annotations:26.0.1")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.guava:guava:21.0")
    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("com.electronwill.night-config:core:${night_config_version}")
    implementation("com.electronwill.night-config:toml:${night_config_version}")
    implementation("net.dv8tion:JDA:${jda_version}") {
        exclude(module = "opus-java")
        exclude(module = "jsr305")
    }
}

tasks.shadowJar {
    dependencies {
        include(dependency("com.electronwill.night-config:core"))
        include(dependency("com.electronwill.night-config:toml"))
        include(dependency("net.dv8tion:JDA"))

        include(dependency("com.fasterxml.jackson.core:jackson-databind"))
        include(dependency("com.fasterxml.jackson.core:jackson-annotations"))
        include(dependency("com.fasterxml.jackson.core:jackson-core"))

        include(dependency("com.neovisionaries:nv-websocket-client"))

        include(dependency("com.squareup.okhttp3:okhttp"))
        include(dependency("com.squareup.okio:okio"))
        include(dependency("com.squareup.okio:okio-jvm"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib-common"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))

        include(dependency("net.sf.trove4j:trove4j"))

        include(dependency("org.apache.commons:commons-collections4"))

        include(dependency("org.jetbrains:annotations"))
    }

    val relocatePackage = { p: String -> relocate(p, "com.breakinblocks.bbchat.shadow.$p") }

    // com.electronwill.night-config:core
    // com.electronwill.night-config:toml
    relocatePackage("com.electronwill.nightconfig")

    // net.dv8tion:JDA
    relocatePackage("net.dv8tion")
    relocatePackage("com.iwebpp.crypto")

    // com.fasterxml.jackson.core:jackson-databind
    relocatePackage("com.fasterxml.jackson.databind")
    relocatePackage("com.fasterxml.jackson.annotation")
    relocatePackage("com.fasterxml.jackson.core")

    // com.neovisionaries:nv-websocket-client
    relocatePackage("com.neovisionaries.ws.client")

    // com.squareup.okhttp3:okhttp
    relocatePackage("okhttp3")
    // com.squareup.okio:okio and com.squareup.okio:okio-jvm
    relocatePackage("okio")
    // org.jetbrains.kotlin:kotlin-stdlib
    // org.jetbrains.kotlin:kotlin-stdlib-common
    relocatePackage("kotlin")

    // net.sf.trove4j:trove4j
    relocatePackage("gnu.trove")

    // org.apache.commons:commons-collections4
    relocatePackage("org.apache.commons.collections4")

    // org.jetbrains:annotations
    relocatePackage("org.intellij.lang.annotations")
    relocatePackage("org.jetbrains.annotations")

    exclude { fte ->
        // FileTreeElement returns null for file when it comes from a jar file (dependency).
        // This allows us to avoid excluding META-INF from our project's resources.
        // Might need to fix this in the future since we are relying on a bug...?
        // It's marked as non-nullable and the documentation says that it "Never returns null".
        @Suppress("UNNECESSARY_SAFE_CALL", "SENSELESS_COMPARISON")
        fte.file == null && fte.relativePath.startsWith("META-INF/")
    }

    exclude("module-info.class") // Java 9 feature
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
