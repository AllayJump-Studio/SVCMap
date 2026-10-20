plugins {
    bluemap.java
}

dependencies {
    api ( "de.bluecolored:bluemap-api" )

    api ( libs.aircompressor )
    api ( libs.bluenbt )
    api ( libs.caffeine ) {
        exclude ( group = "org.jspecify", module = "jspecify" )
    }
    api ( libs.commons.dbcp2 ) {
        exclude ( group = "jakarta.transaction", module = "jakarta.transaction-api" )
    }
    api ( libs.configurate.hocon ) {
        exclude ( group = "org.jspecify", module = "jspecify" )
    }
    api ( libs.configurate.gson ) {
        exclude ( group = "org.jspecify", module = "jspecify" )
    }
    api ( libs.lz4 )
}

tasks.register("zipResourceExtensions", type = Zip::class) {
    from(fileTree("src/main/resourceExtensions"))
    archiveFileName = "resourceExtensions.zip"
    destinationDirectory = file("src/main/resources/de/bluecolored/bluemap/")
}

tasks.processResources {
    dependsOn("zipResourceExtensions")

    // Gradle does not track the values passed to expand(), so without these the version.json
    // would keep whatever version was current the last time the resource was rewritten.
    inputs.property("pluginVersion", project.version.toString())
    inputs.property("pluginGitHash", gitHash() + if (gitClean()) "" else " (dirty)")

    from("src/main/resources") {
        include("de/bluecolored/bluemap/version.json")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
            "version" to project.version,
            "gitHash" to gitHash() + if (gitClean()) "" else " (dirty)",
        )
    }
}

tasks.getByName("sourcesJar") {
    dependsOn("zipResourceExtensions")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "bluemap-${project.name}"
            version = project.version.toString()

            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionResult()
                }
            }
        }
    }
}
