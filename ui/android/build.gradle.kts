import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Delete

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
	val agpVersion = project.providers.gradleProperty("android.agpVersion").get()
	val googleServicesVersion = project.providers.gradleProperty("android.googleServicesVersion").get()

	repositories {
		google()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:$agpVersion")
		classpath("com.google.gms:google-services:$googleServicesVersion")

		// NOTE: Do not place your application dependencies here; they belong
		// in the individual module build.gradle files
	}
}

allprojects {
	repositories {
		google()
		mavenCentral()
	}
}

tasks.register<Delete>("clean") {
	delete(rootProject.layout.buildDirectory)
}

tasks.register<DefaultTask>("bundleThemeRelease") {
	group = "Build"
	description = "Assembles aab for THEME variant"

	val theme = (System.getenv("THEME") ?: "none").lowercase()

	if (theme != "none") {
		dependsOn(tasks.getByPath(":app:bundle${theme.replaceFirstChar(Char::titlecase)}Release"))
	}

	doLast {
		if (theme == "none") {
			throw Exception("Environment variable THEME not set!")
		}

		val source = file("app/build/outputs/bundle/${theme}Release/app-${theme}-release.aab")
		val output = file("target/$theme.aab")

		output.delete()
		copy {
			from(source)
			into(output.parentFile)
			rename(source.name, output.name)
		}
		println("Built $output!")
	}
}

tasks.register<DefaultTask>("assembleThemeRelease") {
	group = "Build"
	description = "Assembles apk for THEME variant"

	val theme = (System.getenv("THEME") ?: "none").lowercase()

	if (theme != "none") {
		dependsOn(tasks.getByPath(":app:assemble${theme.replaceFirstChar(Char::titlecase)}"))
	}

	doLast {
		if (theme == "none") {
			throw Exception("Environment variable THEME not set!")
		}

		val source = file("app/build/outputs/apk/$theme/release/app-${theme}-release.apk")
		val output = file("target/$theme.apk")

		output.delete()
		copy {
			from(source)
			into(output.parentFile)
			rename(source.name, output.name)
		}
		println("Built $output!")
	}
}

tasks.register<DefaultTask>("buildThemeRelease") {
	group = "Build"
	description = "Build aab and apk for THEME variant"

	dependsOn(tasks.named("bundleThemeRelease"))
	dependsOn(tasks.named("assembleThemeRelease"))
}