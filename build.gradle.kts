plugins {
    id("java")
}

group = "ro.serbantudor04"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    implementation("commons-io:commons-io:2.21.0")
}

tasks.test {
    useJUnitPlatform()
}