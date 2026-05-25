import com.github.gradle.node.task.NodeTask

plugins {
	id("com.github.node-gradle.node")
}

node {
	nodeProjectDir.set(layout.projectDirectory.dir("build"))
}

/*
 * Build Antora docs
 */
val buildAntoraDocs by tasks.registering(NodeTask::class) {
	dependsOn(tasks.named("npmInstall"))
	script.set(layout.projectDirectory.file("build/node_modules/@antora/cli/bin/antora"))
	args.set(listOf("--log-format=pretty", "site.yml"))

	val nojekyll = layout.projectDirectory.file("build/.nojekyll")
	val source = layout.projectDirectory.file("build/CNAME")
	val output = layout.projectDirectory.dir("build/www")

	doLast {
		copy {
			from(nojekyll, source)
			into(output)
		}
	}
}