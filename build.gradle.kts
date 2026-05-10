plugins {
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
}

allprojects {
    group = "com.circleguard"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:5.2.1")
        "testRuntimeOnly"("com.h2database:h2")
    }

    // ── Integration Test Source Set ──────────────────────────
    val sourceSets = the<SourceSetContainer>()
    val configs = configurations

    val integrationTestSourceSet = sourceSets.create("integrationTest") {
        java.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
    }
    integrationTestSourceSet.compileClasspath +=
        sourceSets["main"].output + configs["testRuntimeClasspath"]
    integrationTestSourceSet.runtimeClasspath +=
        integrationTestSourceSet.output + integrationTestSourceSet.compileClasspath

    val integrationTestImplementation by configurations.getting {
        extendsFrom(configurations["testImplementation"])
    }
    val integrationTestRuntimeOnly by configurations.getting {
        extendsFrom(configurations["testRuntimeOnly"])
    }

    configurations.named("integrationTestRuntimeClasspath") {
        resolutionStrategy.force(
            "org.testcontainers:testcontainers:1.21.4",
            "org.testcontainers:junit-jupiter:1.21.4",
            "org.testcontainers:postgresql:1.21.4",
            "org.testcontainers:neo4j:1.21.4",
            "com.github.docker-java:docker-java-api:3.4.2",
            "com.github.docker-java:docker-java-transport:3.4.2",
            "com.github.docker-java:docker-java-transport-zerodep:3.4.2"
        )
    }

    dependencies {
        "integrationTestImplementation"("org.testcontainers:testcontainers:1.21.4")
        "integrationTestImplementation"("org.testcontainers:junit-jupiter:1.21.4")
        "integrationTestImplementation"("org.testcontainers:postgresql:1.21.4")
        "integrationTestImplementation"("org.testcontainers:neo4j:1.21.4")
    }

    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests with Testcontainers"
        group = "verification"
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter(tasks.named("test"))
        environment("DOCKER_HOST", "unix:///var/run/docker.sock")
        environment("DOCKER_API_VERSION", "1.54")
        systemProperties(mapOf(
            "docker.host" to "unix:///var/run/docker.sock",
            "docker.api.version" to "1.54",
            "testcontainers.docker.api.version" to "1.54",
            "testcontainers.ryuk.disabled" to "true"
        ))
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

tasks.register("e2eTest") {
    description = "Runs E2E tests against a deployed CircleGuard instance (defaults to localhost ports)"
    group = "verification"
    dependsOn(":tests:e2e:test")
}
