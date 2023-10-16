plugins {
    id("java")
}

group = "fr.enimaloc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-beta.15") {
        exclude("club.minnced", "opus-java")
    }

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
}

tasks.test {
    useJUnitPlatform()
}