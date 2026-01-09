plugins {
    kotlin("jvm") version "2.2.21"
}

group = "org.example"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}