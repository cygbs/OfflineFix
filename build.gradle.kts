plugins {
    java
    `maven-publish`
}

group = "net.ypixel.offlinefix"
version = "1.0.0"
description = "Velocity plugin: make TAB list heads/skins display for offline players, using PacketEvents."

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.1.0")
    annotationProcessor("com.velocitypowered:velocity-api:4.1.0")
    // PacketEvents is provided at runtime by the separate packetevents-velocity plugin ("packetevents").
    compileOnly("com.github.retrooper:packetevents-velocity:2.13.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 25 // velocity-api 4.1.0 requires JVM 25+
}

// Minimal runnable plugin jar (nothing shaded; all deps are compileOnly/provided).
tasks.jar {
    archiveBaseName.set("OfflineFix")
    archiveVersion.set("")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
