plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "cl.retail"
version = "1.0.0"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Aquí fijo la última versión que validé con Spring Boot 3.4; 2.8.15+ rompe el parser MVC de esta línea.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
    implementation("com.google.cloud:google-cloud-firestore:3.33.0")
    implementation("com.google.cloud:google-cloud-pubsub:1.136.0")
    implementation("com.google.cloud:google-cloud-bigquery:2.46.0")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
