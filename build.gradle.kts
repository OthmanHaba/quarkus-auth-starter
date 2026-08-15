import java.sql.DriverManager
import java.util.Properties

buildscript {
    repositories { mavenCentral() }
    dependencies {
        // Driver for the dbCreate task, so it needs no psql on PATH.
        classpath("org.postgresql:postgresql:42.7.7")
        // Flyway 10+ keeps database support in separate modules; the Gradle
        // tasks resolve it from this classpath.
        classpath("org.flywaydb:flyway-database-postgresql:12.0.0")
    }
}

plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.allopen") version "2.4.0"
    id("io.quarkus")
    // Pinned to the flyway-core version Quarkus 3.38.1 ships, so the schema
    // history written by the Gradle tasks and by migrate-at-start cannot drift.
    id("org.flywaydb.flyway") version "12.0.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId = providers.gradleProperty("quarkusPlatformGroupId").get()
val quarkusPlatformArtifactId = providers.gradleProperty("quarkusPlatformArtifactId").get()
val quarkusPlatformVersion = providers.gradleProperty("quarkusPlatformVersion").get()

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-security")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-elytron-security-common")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.quarkus:quarkus-arc")
    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.rest-assured:rest-assured")
}

// ---------------------------------------------------------------------------
// Database tasks
//
// Credentials come from application.properties so there is exactly one place to
// change them, with environment overrides for CI. The %dev profile is the
// target: %test uses Dev Services and %prod reads its own environment.
// ---------------------------------------------------------------------------

val appProperties = Properties().apply {
    file("src/main/resources/application.properties").inputStream().use { load(it) }
}

fun devSetting(key: String, env: String): String =
    System.getenv(env)
        ?: appProperties.getProperty("%dev.quarkus.datasource.$key")
        ?: error("Missing %dev.quarkus.datasource.$key in application.properties (or \$$env)")

val dbUrl: String get() = devSetting("jdbc.url", "DB_URL")
val dbUser: String get() = devSetting("username", "DB_USERNAME")
val dbPassword: String get() = devSetting("password", "DB_PASSWORD")

flyway {
    url = dbUrl
    user = dbUser
    password = dbPassword
    locations = arrayOf("filesystem:src/main/resources/db/migration")
    // flywayClean drops every object in the schema, so it stays opt-in.
    cleanDisabled = !project.hasProperty("allowClean")
}

tasks.register("dbCreate") {
    group = "database"
    description = "Creates the development role and database if they do not exist."
    doLast {
        // Connect to the maintenance database; CREATE DATABASE needs an existing one.
        val adminUrl = dbUrl.substringBeforeLast('/') + "/postgres"
        val adminUser = System.getenv("DB_ADMIN_USERNAME") ?: "postgres"
        val adminPassword = System.getenv("DB_ADMIN_PASSWORD") ?: ""
        val database = dbUrl.substringAfterLast('/').substringBefore('?')

        DriverManager.getConnection(adminUrl, adminUser, adminPassword).use { connection ->
            connection.createStatement().use { statement ->
                // Postgres has no CREATE ... IF NOT EXISTS for roles or databases,
                // so check first. That also keeps the task safely re-runnable.
                val roleExists = statement
                    .executeQuery("SELECT 1 FROM pg_roles WHERE rolname = '$dbUser'")
                    .use { it.next() }
                if (roleExists) {
                    logger.lifecycle("Role '$dbUser' already exists.")
                } else {
                    statement.executeUpdate("CREATE ROLE \"$dbUser\" LOGIN PASSWORD '$dbPassword'")
                    logger.lifecycle("Created role '$dbUser'.")
                }

                val databaseExists = statement
                    .executeQuery("SELECT 1 FROM pg_database WHERE datname = '$database'")
                    .use { it.next() }
                if (databaseExists) {
                    logger.lifecycle("Database '$database' already exists.")
                } else {
                    statement.executeUpdate("CREATE DATABASE \"$database\" OWNER \"$dbUser\"")
                    logger.lifecycle("Created database '$database' owned by '$dbUser'.")
                }
            }
        }
        logger.lifecycle("Ready. Run ./gradlew flywayMigrate or ./gradlew quarkusDev.")
    }
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        javaParameters = true
        freeCompilerArgs.add("-Xemit-jvm-type-annotations")
    }
}
