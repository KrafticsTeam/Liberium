plugins {
    `java-library`
    `maven-publish`
    signing

    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val isRelease = !version.toString().contains("alpha") && !version.toString().contains("beta")

// Include build number in version
val build = System.getenv("BUILD")?.toString()
if (build != null) {
    version = "$version+$build"
}

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = rootProject.group
    version = rootProject.version
    description = rootProject.description

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    dependencies {
        implementation("org.jetbrains:annotations:23.0.0")
        compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")

        testImplementation("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    }

    tasks.getByName<Test>("test") {
        useJUnitPlatform()

        ignoreFailures = true
    }

    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // Add shadowJar to assemble task
    tasks.getByName("assemble") {
        dependsOn("shadowJar")
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    val baseName = "${rootProject.name}-$name"

    tasks.withType<Jar> {
        archiveBaseName.set(baseName)
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                artifactId = baseName
                groupId = project.group.toString()
                version = project.version.toString()

                pom {
                    description.set(project.description)
                    url.set("https://github.com/krafticsteam/liberium")

                    organization {
                        name.set("KrafticsTeam")
                        url.set("https://kraftics.com")
                    }

                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://github.com/krafticsteam/liberium/blob/master/LICENSE")
                            distribution.set("repo")
                        }
                    }

                    scm {
                        url.set("https://github.com/krafticsteam/liberium")
                        connection.set("scm:git:git://github.com/krafticsteam/liberium.git")
                        developerConnection.set("scm:git:ssh://git@github.com:krafticsteam/liberium.git")
                    }

                    developers {
                        developer {
                            url.set("https://panda885.github.io")
                            name.set("Panda885")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                url = if (isRelease) {
                    uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                } else {
                    uri("https://oss.sonatype.org/content/repositories/snapshots/")
                }

                val sonatypeUsername = findProperty("sonatypeUsername")?.toString()
                val sonatypePassword = findProperty("sonatypePassword")?.toString()

                if (sonatypeUsername != null && sonatypePassword != null) {
                    credentials {
                        username = sonatypeUsername
                        password = sonatypePassword
                    }
                }
            }
        }
    }

    if (isRelease) {
        signing {
            sign(publishing.publications["maven"])
        }
    }
}
