pluginManagement {
	includeBuild("gradle/build-logic")

	val bndVersion = providers.gradleProperty("bnd_version").get()
	val bndReleases = providers.gradleProperty("bnd_releases").get()

	val nodeVersion = providers.gradleProperty("node_plugin.version").get()
	val jacocologVersion = providers.gradleProperty("jacocolog_plugin.version").get()
	val sonarqubeVersion = providers.gradleProperty("sonarqube_plugin.version").get()

	plugins {
		id("biz.aQute.bnd.workspace") version bndVersion
		id("biz.aQute.bnd") version bndVersion
		id("com.github.node-gradle.node") version nodeVersion
		id("org.barfuin.gradle.jacocolog") version jacocologVersion
		id("org.sonarqube") version sonarqubeVersion
	}
	repositories {
		maven {
			name = "Central Portal Snapshots"
			url = uri("https://central.sonatype.com/repository/maven-snapshots/")
			content {
				includeModule("io.openems", "j2mod")
			}
		}
		maven {
			name = "BndRepo"
			url = uri(bndReleases)
			content {
				includeGroup("biz.aQute.bnd")
				includeGroup("biz.aQute.bnd.workspace")
			}
		}
		mavenLocal()
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("biz.aQute.bnd.workspace")
}

rootProject.name = "openems"

include(
	":io.openems.backend.application",
	":io.openems.backend.edge.application",
	":doc"
)