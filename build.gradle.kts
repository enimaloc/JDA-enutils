plugins {
    id("java")
    id("maven-publish")
}

group = "fr.enimaloc"
version = "0.4.2"

repositories {
    mavenCentral()
    maven("https://repository.aspose.com/repo/")
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude("club.minnced", "opus-java")
    }

    testImplementation("org.slf4j:slf4j-simple:2.0.3")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    testImplementation("com.aspose:aspose-imaging:23.9:jdk16")
}

var isRelease = !(version as String).startsWith("0.") && !(version as String).endsWith("SNAPSHOT")

publishing {
    println("Publishing version $version")
    println("Is snapshot: ${!isRelease}")
    println("Publishing to ${if (isRelease) "releases" else "snapshots"}")
    repositories {
        maven {
            url = uri("http://m2.enimaloc.fr/${if (isRelease) "releases" else "snapshots"}")
            credentials {
                username = System.getenv("MAVEN_NAME") ?: property("mavenUsername").toString()
                password = System.getenv("MAVEN_TOKEN") ?: property("mavenPassword").toString()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
            isAllowInsecureProtocol = true
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = group as String
            artifactId = project.name
            version = version
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}