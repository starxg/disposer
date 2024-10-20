plugins {
    id("java")
    id("maven-publish")

}

group = "com.starxg"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}



dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jetbrains:annotations:26.0.1")
}


java {
    withSourcesJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}