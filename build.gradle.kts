import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
	id("maven-publish")
	id("org.jetbrains.kotlin.jvm") version "2.3.0"
}

val javaVersion: String = project.property("java_version") as String

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
	maven {
		name = "shedaniel"
		url = uri("https://maven.shedaniel.me/")
	}
	mavenCentral()
}

loom {
	mods {
		register("casual-skyblockz-addons") {
			sourceSet(sourceSets.main.get())
		}
	}

	runs.configureEach {
		generateRunConfig.set(true)
	}
}

val minecraftVersion: String = project.property("minecraft_version") as String
val loaderVersion: String = project.property("loader_version") as String
val fabricApiVersion: String = project.property("fabric_api_version") as String
val clothConfigVersion: String = project.property("cloth_config_version") as String

dependencies {
	minecraft("com.mojang:minecraft:$minecraftVersion")
	implementation("net.fabricmc:fabric-loader:$loaderVersion")
	implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
	implementation("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")
	implementation(kotlin("stdlib-jdk8"))
}

tasks.processResources {
	val ver = project.version
	inputs.property("version", ver)
	filesMatching("fabric.mod.json") {
		expand("version" to ver)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(javaVersion.toInt())
}

tasks.withType<KotlinJvmCompile>().configureEach {
	compilerOptions {
		jvmTarget.set(JvmTarget.fromTarget(javaVersion))
	}
}

java {
	withSourcesJar()
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
	}
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)
	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

publishing {
	publications {
		create("mavenJava", MavenPublication::class.java) {
			from(components.getByName("java"))
		}
	}
	repositories { }
}
kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
	}
}