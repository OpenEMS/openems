plugins {
	`kotlin-dsl`
}

repositories {
	gradlePluginPortal()
	mavenCentral()
}

gradlePlugin {
	plugins {
		register("bundleAggregates") {
			id = "openems.bundle-aggregates"
			implementationClass = "openems.BundleAggregatesPlugin"
		}
	}
}