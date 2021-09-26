import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask as OpenApiGenerateTask
import nu.studer.gradle.jooq.JooqEdition
import org.jooq.meta.jaxb.Property as JooqProperty
import org.jooq.meta.jaxb.Logging as JooqLogging

group = "app.todos"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_14
java.targetCompatibility = JavaVersion.VERSION_14

plugins {
    val kotlinVersion = "1.5.30"

    id("idea")

    id("org.openapi.generator") version "5.2.1"
    id("org.springframework.boot") version "2.4.10"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("nu.studer.jooq") version "6.0.1"

    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion

}

val openApiSpecPath = "$projectDir/src/main/resources/static/schema"
val openApiGeneratedSourceFolder = "main/kotlin"

val generatedSourcesDir = "$buildDir/generated/sources"

repositories {
    mavenCentral()
}

extra["kotlinVersion"] = "1.5.30"
extra["springBootVersion"] = "2.4.10"
extra["jooqVersion"] = "3.15.2"
extra["liquibaseVersion"] = "3.8.0"

dependencies {
    implementation(kotlin("stdlib"))

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-validation:${property("springBootVersion")}")
    implementation("org.springframework.boot:spring-boot-starter-web:${property("springBootVersion")}")
    // Schema Exposure
    implementation("org.springdoc:springdoc-openapi-ui:1.5.10")
    //DB
    implementation("org.springframework.boot:spring-boot-starter-jooq:${property("springBootVersion")}")
    runtimeOnly("com.h2database:h2")
    implementation("org.liquibase:liquibase-core:${property("liquibaseVersion")}")
    jooqGenerator("com.h2database:h2")
    jooqGenerator("org.jooq:jooq-meta-extensions-liquibase:${property("jooqVersion")}")
    jooqGenerator("org.jooq:jooq-codegen:${property("jooqVersion")}")
    jooqGenerator("org.jooq:jooq:${property("jooqVersion")}")
    jooqGenerator("org.liquibase:liquibase-core:${property("liquibaseVersion")}")
    jooqGenerator("org.yaml:snakeyaml:1.26")
    jooqGenerator("org.slf4j:slf4j-simple:1.7.30")

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.0.11")


    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$embeddedKotlinVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${property("springBootVersion")}") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure:${property("springBootVersion")}") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
    testImplementation("com.ninja-squad:springmockk:3.0.1")
    testImplementation("org.awaitility:awaitility-kotlin:4.1.0")
}

sourceSets.getByName("main") {
    java.srcDir(
        "$generatedSourcesDir/$openApiGeneratedSourceFolder"
    )
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_12.majorVersion
    }
    dependsOn("generateSchemaApis")
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("FAILED")
        setExceptionFormat("full")
    }
}

val openApiDefaultConfig = mapOf(
    "sourceFolder" to openApiGeneratedSourceFolder,
    "enumPropertyNaming" to "original",
    "useTags" to "true",
    "gradleBuildFile" to "false",
    "exceptionHandler" to "false",
    "useBeanValidation" to "true",
    "interfaceOnly" to "false",
    "serializationLibrary" to "jackson",
    "dateLibrary" to "string",
    "library" to "jvm-retrofit2",
    "gradleBuildFile" to "false",
    "serviceInterface" to "true"
)

tasks.create("openApiToDosSpec", OpenApiGenerateTask::class) {
    generatorName.set("kotlin-spring")
    outputDir.set(generatedSourcesDir)
    inputSpec.set("$openApiSpecPath/todos.yaml")
    apiPackage.set("app.todos.api")
    modelPackage.set("app.todos.model")
    configOptions.set(openApiDefaultConfig)
    doFirst {
        delete("$generatedSourcesDir/$openApiGeneratedSourceFolder/app/todos")
    }
}


tasks.create("generateSchemaApis") {
    dependsOn(
        "openApiToDosSpec"
    )
    doLast {
        delete("$generatedSourcesDir/$openApiGeneratedSourceFolder/org")
    }
}

jooq {
    version.set("${property("jooqVersion")}")
    edition.set(JooqEdition.OSS)

    configurations {

        create("main") {

            generateSchemaSourceOnCompilation.set(true)
            jooqConfiguration.apply {
                logging = JooqLogging.ERROR
                generator.apply {
                    name = "org.jooq.codegen.JavaGenerator"
                    database.apply {
                        name = "org.jooq.meta.extensions.liquibase.LiquibaseDatabase"
                        jdbc.apply {
                            driver = "org.h2.Driver"
                            url = "jdbc:h2:mem:gradle"
                            user = "sa"
                            password = ""
                        }
                        properties.add(
                            JooqProperty().withKey("scripts")
                                .withValue("$projectDir/src/main/resources/db/changelog/master.yaml")
                        )
                        properties.add(
                            JooqProperty().withKey("includeLiquibaseTables")
                                .withValue("false")
                        )
                    }
                    generate.apply {
                        isRelations = true
                        isRecords = true
                    }
                    target.apply {
                        packageName = "app.todos.repository"
                    }
                    strategy.apply {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                }
            }
        }
    }
}