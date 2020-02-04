apply {
    plugin("maven-publish")
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(kotlin("stdlib-jdk8"))
    testImplementation(project(":testApi"))
}

publishing {
    publications {
        register<MavenPublication>("mathjaxPlugin") {
            artifactId = "mathjax-plugin"
            from(components["java"])
        }
    }
}