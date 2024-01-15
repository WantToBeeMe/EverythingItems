plugins {
    id("java")
    kotlin("jvm") version "1.8.0"
    `maven-publish`

}

group = "com.github.WantToBeeMe"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        // here you can find the latest's spigot versions
        // https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
