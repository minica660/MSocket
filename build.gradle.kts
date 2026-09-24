plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("maven-publish")

}

group = "com.github.minica660"
version = "1.2"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }

}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.14.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec>().configureEach {
    defaultCharacterEncoding = "UTF-8"
}




//tasks {
//    build {
//        dependsOn(shadowJar)
//    }
//
//    runServer {
//        // Configure the Minecraft version for our task.
//        // This is the only required configuration besides applying the plugin.
//        // Your plugin's jar (or shadowJar if present) will be used automatically.
//        minecraftVersion("1.21.11")
//        jvmArgs("-Xms2G", "-Xmx2G")
//    }
//
//    processResources {
//        val props = mapOf("version" to version)
//        filesMatching("plugin.yml") {
//            expand(props)
//        }
//    }
//}
