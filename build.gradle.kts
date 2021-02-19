allprojects {
    group = 'com.breakinblocks.bbchat'
    version = "${mod_version}"
}

subprojects {
    buildscript {
        repositories {
            jcenter()
            mavenCentral()
        }
        dependencies {
            classpath 'com.github.jengelman.gradle.plugins:shadow:4.0.4'
        }
    }

    apply plugin: 'java'
    apply plugin: 'eclipse'
    apply plugin: 'idea'

    sourceCompatibility = targetCompatibility = compileJava.sourceCompatibility = compileJava.targetCompatibility = '1.8'

    repositories {
        jcenter()
        mavenCentral()
    }
}
