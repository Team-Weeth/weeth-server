import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.jpa") version "2.1.0"

    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

group = "weeth"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

// 버전 변수
val springdocVersion = "2.8.15"
val jjwtVersion = "0.13.0"
val awsSdkBomVersion = "2.41.29"
val testcontainersBomVersion = "2.0.3"
val kotestVersion = "5.9.1"
val mockkVersion = "1.13.14"
val springmockkVersion = "4.0.2"
val lombokVersion = "1.18.36"
val mapstructVersion = "1.6.3"

dependencies {
    // --- Lombok (temporary, will be removed during Kotlin migration) ---
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // --- MapStruct (temporary, will be removed during Kotlin migration) ---
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // --- Kotlin ---
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // --- Spring Boot ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Actuator + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // --- JWT ---
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // --- DB ---
    runtimeOnly("com.mysql:mysql-connector-j")

    // --- Swagger (springdoc) ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // --- AWS SDK v2 (BOM) ---
    implementation(platform("software.amazon.awssdk:bom:$awsSdkBomVersion"))
    implementation("software.amazon.awssdk:s3")

    // --- Test (starter-test는 유지하되 mockito는 제외) ---
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Kotest
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")

    // MockK
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")

    // Testcontainers (BOM)
    testImplementation(platform("org.testcontainers:testcontainers-bom:$testcontainersBomVersion"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Kotlin 컴파일 옵션 (Kotlin 2.x 권장 방식)
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xjvm-default=all",
        )
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}

// plain jar 파일 생성 방지 (bootJar는 그대로)
tasks.jar {
    enabled = false
}

// ktlint 설정
ktlint {
    version.set("1.8.0")
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}
