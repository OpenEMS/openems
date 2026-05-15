import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
	id("base")
	id("java")
	id("com.github.node-gradle.node")
	id("org.barfuin.gradle.jacocolog")
	id("org.sonarqube")
	id("openems.bundle-aggregates")
}

repositories {
	mavenLocal()
	mavenCentral()
	gradlePluginPortal()
}

fun Project.isEdgeBundle(): Boolean =
	name.matches(Regex("io\\.openems\\.(common|core|edge|oem|shared|wrapper).*"))

fun Project.isBackendBundle(): Boolean =
	name.matches(Regex("io\\.openems\\.(backend|common|core|oem|shared|wrapper).*"))

fun Project.registerAppJarBuildTask(
	taskName: String,
	taskDescription: String,
	assembleTaskName: String,
	appProjectPath: String,
	appName: String,
	generatedJarRelativePath: String,
	outputProperty: String,
	outputEnvVar: String,
	defaultOutputFileName: String
) {
	tasks.register(taskName) {
		group = "OpenEMS-Build"
		description = taskDescription

		val assemble = tasks.named(assembleTaskName)
		val export = tasks.getByPath("$appProjectPath:export.$appName")
		val resolve = tasks.getByPath("$appProjectPath:resolve.$appName")

		dependsOn(assemble)
		dependsOn(export)
		dependsOn(resolve)

		resolve.mustRunAfter(assemble)
		export.mustRunAfter(resolve)

		/* force rebuild */
		export.outputs.upToDateWhen { false }
		outputs.upToDateWhen { false }

		doLast {
			val source = file(generatedJarRelativePath)
			val output = providers.gradleProperty(outputProperty)
				.orElse(providers.environmentVariable(outputEnvVar))
				.orElse(layout.buildDirectory.file(defaultOutputFileName).map { it.asFile.path })
				.map { file(it) }
				.get()

			output.delete()
			copy {
				from(source)
				into(output.parentFile)
				rename(source.name, output.name)
			}
			println("Built $output!")
		}
	}
}

val javaSource = providers.gradleProperty("java_source").get().toInt()
val javaTarget = providers.gradleProperty("java_target").get().toInt()
val isCI = providers.environmentVariable("CI").isPresent

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaSource))
	}
}

if (isCI) {
	println("CI environment detected - suppressing all compiler warnings")
}

/*
 * Setup multi-projects:
 * - setup bnd tasks
 * - disable report generation
 * - setup parallel execution
 */
subprojects {
	repositories {
		mavenCentral()
	}

	sonarqube {
		val scope = providers.gradleProperty("sonar.scope").orNull
		isSkipProject =
			(scope == "edge" && !project.isEdgeBundle()) ||
				(scope == "backend" && !project.isBackendBundle())
	}

	tasks.withType<JavaCompile>().configureEach {
		options.release.set(javaTarget)
		options.encoding = "UTF-8"
		options.isFork = true
		options.isIncremental = true

		if (isCI) {
			options.compilerArgs.addAll(listOf("-Xlint:none", "-nowarn"))
			options.isDeprecation = false
		}
	}

	tasks.withType<Test>().configureEach {
		useJUnitPlatform()
		maxParallelForks = (Runtime.getRuntime().availableProcessors() * 0.66).toInt().coerceAtLeast(1)
		reports {
			html.required.set(false)
			junitXml.required.set(false)
		}

		// Avoid Mockito inline self-attach warning on newer JDKs by passing Mockito as javaagent.
		doFirst {
			val mockitoAgentJar = classpath.files.firstOrNull { file ->
				file.name.startsWith("mockito-core-") && file.name.endsWith(".jar")
			}
			if (mockitoAgentJar != null) {
				val agentArg = "-javaagent:${mockitoAgentJar.absolutePath}"
				if (!(jvmArgs ?: emptyList()).contains(agentArg)) {
					jvmArgs(agentArg)
				}
			}
		}

		if (isCI) {
			testLogging {
				events("failed")
				exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
				showExceptions = true
				showStackTraces = true
				showCauses = true
			}

			addTestListener(object : TestListener {
				override fun beforeSuite(suite: TestDescriptor) = Unit
				override fun beforeTest(testDescriptor: TestDescriptor) = Unit
				override fun afterSuite(desc: TestDescriptor, result: TestResult) {
					if (desc.parent == null) {
						val shortName = path.takeLast(36)
						val padded = String.format("%36s", shortName)
						val icon = when (result.resultType) {
							TestResult.ResultType.SUCCESS -> "✅"
							TestResult.ResultType.FAILURE -> "❌"
							TestResult.ResultType.SKIPPED -> "ℹ️"
						}
						println(
							"> [$padded] Test summary: ${result.resultType} $icon " +
								"(${result.testCount} tests, " +
								"${result.successfulTestCount} passed, " +
								"${result.failedTestCount} failed, " +
								"${result.skippedTestCount} skipped)"
						)
					}
				}

				override fun afterTest(descriptor: TestDescriptor, result: TestResult) {
					if (result.failedTestCount > 0) {
						println("\n${descriptor.className}.${descriptor.name} FAILED")
						result.exception?.printStackTrace(System.out)
					}
				}
			})
		}
	}

	pluginManager.withPlugin("biz.aQute.bnd") {
		pluginManager.apply("checkstyle")
		pluginManager.apply("jacoco")

		tasks.withType<JacocoReport>().configureEach {
			reports {
				xml.required.set(true)
				csv.required.set(false)
				html.required.set(false)
			}
			// Exclude com.dalsemi.onewire
			classDirectories.setFrom(
				files(classDirectories.files.map {
					fileTree(it) {
						exclude("**/com/dalsemi/onewire/**")
					}
				})
			)
		}

		extensions.configure<CheckstyleExtension>("checkstyle") {
			toolVersion = "11.1.0"
			configFile = file("${rootDir}/cnf/checkstyle.xml")
			maxWarnings = 0
			isIgnoreFailures = false
		}

		tasks.withType<Checkstyle>().configureEach {
			reports {
				xml.required.set(false)
				html.required.set(false)
				sarif.required.set(false)
			}
			minHeapSize = "512m"
			maxHeapSize = "2048m"
			// Exclude com.dalsemi.onewire
			exclude("**/com/dalsemi/onewire/*")
		}
	}
}

/*
 * Build OpenEMS Edge and Backend Components
 */
tasks.register("buildComponents") {
	subprojects.forEach { proj ->
		if (proj.tasks.names.contains("compileJava")) {
			dependsOn(proj.tasks.named("compileJava"))
			proj.tasks.withType<Test>().configureEach {
				testLogging {
					events("passed", "skipped", "failed", "standardOut", "standardError")
				}
			}
		}
	}
}

registerAppJarBuildTask(
	taskName = "buildEdge",
	taskDescription = "Build a Fat-Jar for the OpenEMS-Edge into build/openems-edge.jar",
	assembleTaskName = "assembleEdge",
	appProjectPath = ":io.openems.edge.application",
	appName = "EdgeApp",
	generatedJarRelativePath = "io.openems.edge.application/generated/distributions/executable/EdgeApp.jar",
	outputProperty = "oems.edge.output",
	outputEnvVar = "OEMS_EDGE_OUTPUT",
	defaultOutputFileName = "openems-edge.jar"
)

registerAppJarBuildTask(
	taskName = "buildBackend",
	taskDescription = "Build a Fat-Jar for the OpenEMS-Backend into build/openems-backend.jar",
	assembleTaskName = "assembleBackend",
	appProjectPath = ":io.openems.backend.application",
	appName = "BackendApp",
	generatedJarRelativePath = "io.openems.backend.application/generated/distributions/executable/BackendApp.jar",
	outputProperty = "oems.backend.output",
	outputEnvVar = "OEMS_BACKEND_OUTPUT",
	defaultOutputFileName = "openems-backend.jar"
)

registerAppJarBuildTask(
	taskName = "buildBackendEdge",
	taskDescription = "Build a Fat-Jar for the OpenEMS-Backend-Edge-App into build/openems-backend-edge.jar",
	assembleTaskName = "assembleBackend",
	appProjectPath = ":io.openems.backend.edge.application",
	appName = "BackendEdgeApp",
	generatedJarRelativePath = "io.openems.backend.edge.application/generated/distributions/executable/BackendEdgeApp.jar",
	outputProperty = "oems.backend.edge.output",
	outputEnvVar = "OEMS_BACKEND_EDGE_OUTPUT",
	defaultOutputFileName = "openems-backend-edge.jar"
)

/*
 * Javadoc
 */
tasks.register<Javadoc>("buildAggregatedJavadocs") {
	description = "Generate javadocs from all child projects as if it was a single project"
	group = "Documentation"
	destinationDir = layout.buildDirectory.dir("www/javadoc").get().asFile
	title = "OpenEMS Javadoc"

	subprojects.forEach { proj ->
		proj.tasks.withType<Javadoc>().forEach { javadocTask ->
			(javadocTask.options as StandardJavadocDocletOptions).encoding = "UTF-8"
			(options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
			source(javadocTask.source)
			classpath += javadocTask.classpath
			exclude(javadocTask.excludes)
			include(javadocTask.includes)
		}
	}
}

/*
 * Copies Bundle readme.adoc files to doc.
 */
tasks.register("copyBundleReadmes") {
	doLast {
		// define target files
		val targets = mapOf(
			"core" to "edge/core.d",
			"controller" to "edge/controller.d",
			"scheduler" to "edge/scheduler.d",
			"nature" to "edge/nature.d",
			"bridge" to "edge/bridge.d",
			"deviceService" to "edge/device_service.d",
			"predictor" to "edge/predictor.d",
			"tariff" to "edge/tariff.d",
			"timedata" to "edge/timedata.d",
			"backendService" to "backend/service.d"
		)

		// initialize target files and directories
		val basePath = "${projectDir.path}/doc/modules/ROOT/pages/"
		targets.values.forEach { target ->
			delete(fileTree("$basePath$target") {
				include("**/*.adoc")
			})
		}

		// Collect entries for all targets
		val targetEntries = mutableMapOf<String, MutableList<Map<String, String>>>()

		subprojects.forEach { proj ->
			// in each subproject (= bundle)...
			proj.file(".").listFiles()?.forEach { sourceFile ->
				// find the 'readme.adoc' file
				if (sourceFile.name.equals("readme.adoc", ignoreCase = true)) {
					val title = sourceFile.bufferedReader().use { r ->
						r.readLine().replaceFirst(Regex("^= "), "")
					}

					val bundle = sourceFile.parentFile.name
					if (bundle in listOf("io.openems.edge.core", "io.openems.core.referencetarget")) {
						return@forEach
					}

					var targetKey: String? = null

					if (bundle.startsWith("io.openems.edge.")) {
						val edgeBundle = bundle.removePrefix("io.openems.edge.")
						targetKey = when {
							edgeBundle in listOf(
								"energy",
								"evcs.ocpp.common",
								"ess.cluster",
								"evcs.cluster",
								"evcs.core",
								"evse.electricvehicle",
								"ess.generic",
								"evcs.ocpp.server",
								"simulator",
								"pvinverter.cluster"
							) -> "core"
							edgeBundle.endsWith(".api") -> "nature"
							edgeBundle.startsWith("controller.") -> "controller"
							edgeBundle.startsWith("scheduler.") -> "scheduler"
							edgeBundle.startsWith("bridge.") -> "bridge"
							edgeBundle.startsWith("predictor.") -> "predictor"
							edgeBundle.startsWith("timeofusetariff.") -> "tariff"
							edgeBundle.startsWith("timedata.") -> "timedata"
							else -> "deviceService"
						}
					} else if (bundle.startsWith("io.openems.backend.")) {
						val backendBundle = bundle.removePrefix("io.openems.backend.")
						targetKey = when {
							backendBundle.startsWith("timedata.") -> "backendService"
							backendBundle.startsWith("metadata.") -> "backendService"
							else -> null
						}
						if (targetKey == null) {
							return@forEach
						}
					} else if (bundle.startsWith("io.openems.common.")) {
						val commonBundle = bundle.removePrefix("io.openems.common.")
						if (commonBundle.startsWith("bridge.")) {
							return@forEach
						}
					} else if (bundle.startsWith("io.openems.wrapper") || bundle.startsWith("io.openems.oem.")) {
						return@forEach
					}

					if (targetKey == null) {
						throw Exception("Bundle type is unknown for $bundle")
					}

					val target = targets.getValue(targetKey)

					// copy the bundle readme file to target folder
					val readmeFile = "$bundle.adoc"
					copy {
						from(sourceFile.toPath())
						into("$basePath$target")
						rename { readmeFile }
					}

					// collect entries for target
					targetEntries.getOrPut(targetKey) { mutableListOf() }
						.add(mapOf("path" to "$target/$readmeFile", "title" to title))
				}
			}
		}

		// Sort and write entries for all targets
		targetEntries.forEach { (targetKey, entries) ->
			val target = targets.getValue(targetKey)
			val nav = File("$basePath$target/_nav.adoc")
			nav.writeText("")
			entries.sortedBy { it.getValue("title").lowercase() }.forEach { entry ->
				nav.appendText("* xref:${entry.getValue("path")}[${entry.getValue("title")}]\n")
			}
		}
	}
}

/*
 * Build Antora docs
 */
tasks.register("buildAntoraDocs") {
	group = "Documentation"
	dependsOn(":doc:buildAntoraDocs")
	dependsOn(":copyBundleReadmes")

	mustRunAfter(":copyBundleReadmes")

	val source = file("doc/build/www")
	val output = layout.buildDirectory.dir("www").get().asFile

	doLast {
		output.deleteRecursively()
		copy {
			from(source)
			into(output)
		}
		source.deleteRecursively()

		println("Built $output!")
	}
}