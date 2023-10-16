plugins {
    id("java")
    id("maven-publish")
}

group = "fr.enimaloc"
version = "0.1.1"

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

var isRelease = !(version as String).startsWith("0.") && !(version as String).endsWith("SNAPSHOT")

publishing {
    println("Publishing version $version")
    println("Is snapshot: ${!isRelease}")
    println("Publishing to ${if (isRelease) "releases" else "snapshots"}")
    repositories {
        maven {
            url = uri("https://m2.enimaloc.fr/${if (isRelease) "releases" else "snapshots"}")
            credentials {
                username = System.getenv("MAVEN_NAME") ?: property("mavenUsername").toString()
                password = System.getenv("MAVEN_TOKEN") ?: property("mavenPassword").toString()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
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