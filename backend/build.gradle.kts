plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.beentteum"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

val jjwtVersion = "0.13.0"

dependencies {
	// Web(REST API) / 입력값 검증
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// 인증/인가: Spring Security + JWT(jjwt)
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

	// DB: PostgreSQL + PostGIS (hibernate-spatial = PostGIS geometry 타입 매핑, 버전은 Boot BOM 관리)
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.hibernate.orm:hibernate-spatial")
	runtimeOnly("org.postgresql:postgresql")

	// DB Migration: Flyway
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")

	// Cache: Redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val cafeSearchPostgisTask = "cafeSearchPostgisTest"

tasks.withType<Test>().matching { it.name != cafeSearchPostgisTask }.configureEach {
    useJUnitPlatform {
        excludeTags("db")
    }
}
tasks.register<Test>(cafeSearchPostgisTask) {
    description = "Runs cafe search integration tests against a dedicated PostgreSQL/PostGIS database."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("com.beentteum.cafe.repository.CafeSearchPostgisTest")
    }
    useJUnitPlatform {
        includeTags("db")
    }
    outputs.upToDateWhen { false }
}
