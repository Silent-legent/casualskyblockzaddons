import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	id("maven-publish")
	id("org.jetbrains.kotlin.jvm")
}

val javaVersion: String = project.property("java_version") as String
val minecraftVersion: String = project.property("minecraft_version") as String
val minecraftDependency: String = project.property("minecraft_dependency") as String
val loaderVersion: String = project.property("loader_version") as String
val fabricApiVersion: String = project.property("fabric_api_version") as String
val clothConfigVersion: String = project.property("cloth_config_version") as String
val skyblockApiVersion: String = project.property("skyblock_api_version") as String
val skyblockApiCapability: String = project.property("skyblock_api_capability") as String

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
	exclusiveContent {
		forRepository {
			maven {
				name = "Modrinth"
				url = uri("https://api.modrinth.com/maven")
			}
		}
		filter {
			includeGroup("maven.modrinth")
		}
	}
	maven {
		name = "shedaniel"
		url = uri("https://maven.shedaniel.me/")
	}
	maven {
		name = "teamresourceful"
		url = uri("https://maven.teamresourceful.com/repository/maven-public/")
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

dependencies {
	minecraft("com.mojang:minecraft:$minecraftVersion")
	implementation("net.fabricmc:fabric-loader:$loaderVersion")
	implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
	implementation("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")
	implementation(kotlin("stdlib-jdk8"))
	implementation("tech.thatgravyboat:skyblock-api:$skyblockApiVersion") {
		capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$skyblockApiCapability") }
		exclude(group = "me.djtheredstoner")
	}
	include("tech.thatgravyboat:skyblock-api:$skyblockApiVersion") {
		capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$skyblockApiCapability") }
		exclude(group = "me.djtheredstoner")
	}
}

tasks.processResources {
	val ver = project.version
	val mcDep = minecraftDependency
	inputs.property("version", ver)
	inputs.property("minecraft_dependency", mcDep)
	filesMatching("fabric.mod.json") {
		expand("version" to ver, "minecraft_dependency" to mcDep)
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
	val projectName = rootProject.name
	inputs.property("projectName", projectName)
	archiveBaseName.set("casualskyblockzaddons-mc$minecraftVersion")
	from(rootProject.file("LICENSE")) {
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