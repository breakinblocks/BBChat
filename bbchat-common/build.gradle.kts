apply plugin: 'com.github.johnrengelman.shadow'

dependencies {
    compile 'org.jetbrains:annotations:16.0.2'
    compile 'com.google.code.findbugs:jsr305:3.0.2'
    compile 'com.google.guava:guava:21.0'
    compile 'org.apache.logging.log4j:log4j-api:2.11.2'
    compile 'org.apache.logging.log4j:log4j-core:2.11.2'

    compile("net.dv8tion:JDA:${jda_version}") {
        exclude module: 'opus-java'
        exclude module: 'jsr305'
    }
}

shadowJar {
    dependencies {
        include(dependency('net.dv8tion:JDA'))

        include(dependency('com.fasterxml.jackson.core:jackson-databind'))
        include(dependency('com.fasterxml.jackson.core:jackson-annotations'))
        include(dependency('com.fasterxml.jackson.core:jackson-core'))

        include(dependency('com.neovisionaries:nv-websocket-client'))

        include(dependency('com.squareup.okhttp3:okhttp'))
        include(dependency('com.squareup.okio:okio'))

        include(dependency('net.sf.trove4j:trove4j'))

        include(dependency('org.apache.commons:commons-collections4'))

        include(dependency('org.jetbrains:annotations'))

        include(dependency('org.slf4j:slf4j-api'))
    }

    ext.relocatePackage = { p -> relocate(p, 'com.breakinblocks.bbchat.shadow.' + p) }

    // net.dv8tion:JDA
    relocatePackage('net.dv8tion')
    relocatePackage('com.iwebpp.crypto')

    // com.fasterxml.jackson.core:jackson-databind
    relocatePackage('com.fasterxml.jackson.databind')
    relocatePackage('com.fasterxml.jackson.annotation')
    relocatePackage('com.fasterxml.jackson.core')

    // com.neovisionaries:nv-websocket-client
    relocatePackage('com.neovisionaries.ws.client')

    // com.squareup.okhttp3:okhttp
    relocatePackage('okhttp3')
    relocatePackage('okio')

    // net.sf.trove4j:trove4j
    relocatePackage('gnu.trove')

    // org.apache.commons:commons-collections4
    relocatePackage('org.apache.commons.collections4')

    // org.jetbrains:annotations
    relocatePackage('org.intellij.lang.annotations')
    relocatePackage('org.jetbrains.annotations')

    // org.slf4j:slf4j-api
    relocatePackage('org.slf4j')

    exclude 'META-INF/**'

    exclude 'module-info.class' // Java 9 feature
}

build.dependsOn(shadowJar)
