package openems

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

class BundleAggregatesPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		if (target != target.rootProject) {
			return
		}

		val isEdgeBundle: (Project) -> Boolean = {
			it.name.matches(Regex("io\\.openems\\.(common|core|edge|oem|shared|wrapper).*"))
		}
		val isBackendBundle: (Project) -> Boolean = {
			it.name.matches(Regex("io\\.openems\\.(backend|common|core|oem|shared|wrapper).*"))
		}

		registerAggregateTask(
			target,
			"checkstyleEdge",
			"OpenEMS-Test",
			"Checkstyle Edge bundles",
			isEdgeBundle,
			listOf("checkstyleMain", "checkstyleTest")
		)
		registerAggregateTask(
			target,
			"checkstyleBackend",
			"OpenEMS-Test",
			"Checkstyle Backend bundles",
			isBackendBundle,
			listOf("checkstyleMain", "checkstyleTest")
		)

		target.tasks.register("checkstyleAll") {
			group = "OpenEMS-Test"
			description = "Checkstyle all bundles"
			dependsOn(target.tasks.named("checkstyleEdge"))
			dependsOn(target.tasks.named("checkstyleBackend"))
		}

		registerAggregateTask(
			target,
			"testEdge",
			"OpenEMS-Test",
			"Run JUnit tests for all Edge-Bundles",
			isEdgeBundle,
			listOf("test")
		)
		registerAggregateTask(
			target,
			"testBackend",
			"OpenEMS-Test",
			"Run JUnit tests for all Backend-Bundles",
			isBackendBundle,
			listOf("test")
		)

		registerAggregateTask(
			target,
			"cleanEdge",
			"OpenEMS-Build",
			"Clean all Edge-Bundles",
			isEdgeBundle,
			listOf("clean")
		)
		registerAggregateTask(
			target,
			"cleanBackend",
			"OpenEMS-Build",
			"Clean all Backend-Bundles",
			isBackendBundle,
			listOf("clean")
		)

		registerAggregateTask(
			target,
			"assembleEdge",
			"OpenEMS-Build",
			"Assemble all Edge-Bundles",
			isEdgeBundle,
			listOf("assemble")
		)
		registerAggregateTask(
			target,
			"assembleBackend",
			"OpenEMS-Build",
			"Assemble all Backend-Bundles",
			isBackendBundle,
			listOf("assemble")
		)

		registerAggregateTask(
			target,
			"jacocoEdgeReport",
			"verification",
			"Jacoco reports for Edge projects",
			isEdgeBundle,
			listOf("jacocoTestReport")
		)
		registerAggregateTask(
			target,
			"jacocoBackendReport",
			"verification",
			"Jacoco reports for Backend projects",
			isBackendBundle,
			listOf("jacocoTestReport")
		)
	}

	private fun registerAggregateTask(
		target: Project,
		name: String,
		group: String,
		description: String,
		bundleFilter: (Project) -> Boolean,
		taskNames: List<String>
	): TaskProvider<Task> {
		val aggregateTask = target.tasks.register(name) {
			this.group = group
			this.description = description
		}

		target.gradle.projectsEvaluated {
			target.subprojects
				.filter(bundleFilter)
				.forEach { project ->
					taskNames.forEach { taskName ->
						project.tasks.findByName(taskName)?.let { bundleTask ->
							aggregateTask.configure {
								dependsOn(bundleTask)
							}
						}
					}
				}
		}

		return aggregateTask
	}
}