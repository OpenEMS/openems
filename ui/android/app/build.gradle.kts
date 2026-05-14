import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
	id("com.android.application")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
	load(FileInputStream(keystorePropertiesFile))
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

data class OpenemsVersion(val name: String, val code: Int)

fun getVersionCode(): Int {
	val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
	return dateStr.toInt()
}

fun getVersion(): OpenemsVersion {
	val versionEnv = System.getenv("VERSION")
	if (versionEnv == null) {
		logger.warn("WARNING: environment VERSION not specified! using `SNAPSHOT` instead.")
		return OpenemsVersion(name = "SNAPSHOT", code = 1)
	}
	val v = versionEnv.split("-")[0]
	val c = getVersionCode()
	logger.lifecycle("OpenEMS-Version: $v($c)")
	return OpenemsVersion(name = v, code = c)
}

val version = getVersion()

android {
	compileSdk = rootProject.extra["compileSdkVersion"] as Int

	defaultConfig {
		minSdk = rootProject.extra["minSdkVersion"] as Int
		targetSdk = rootProject.extra["targetSdkVersion"] as Int
		versionCode = version.code
		versionName = version.name
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		aaptOptions {
			// Files and dirs to omit from the packaged assets dir, modified to accommodate modern web apps.
			// Default: https://android.googlesource.com/platform/frameworks/base/+/282e181b58cf72b6ca770dc7ca5f91f135444502/tools/aapt/AaptAssets.cpp#61
			ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~"
		}
		multiDexEnabled = true
	}

	flavorDimensions += "version"
	productFlavors {
		create("example") {
			applicationId = "io.example.ui"
			namespace = "io.example.ui"
			dimension = "version"
		}
	}

	sourceSets {
		getByName("example") {
			res.srcDirs("src/example/res")
			java.srcDirs("src/example/java")
		}
	}

	signingConfigs {
		create("release") {
			keyAlias = keystoreProperties["keyAlias"] as String
			keyPassword = keystoreProperties["keyPassword"] as String
			storeFile = file(keystoreProperties["storeFile"] as String)
			storePassword = keystoreProperties["storePassword"] as String
		}
	}

	buildTypes {
		getByName("release") {
			signingConfig = signingConfigs.getByName("release")
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
		}
	}
}

repositories {
	flatDir {
		dirs("../capacitor-cordova-android-plugins/src/main/libs", "libs")
	}
}

dependencies {
	implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
	implementation("androidx.appcompat:appcompat:${rootProject.extra["androidxAppCompatVersion"]}")
	implementation("androidx.coordinatorlayout:coordinatorlayout:${rootProject.extra["androidxCoordinatorLayoutVersion"]}")
	implementation("androidx.core:core-splashscreen:${rootProject.extra["coreSplashScreenVersion"]}")
	implementation(project(":capacitor-android"))
	testImplementation("junit:junit:${rootProject.extra["junitVersion"]}")
	androidTestImplementation("androidx.test.ext:junit:${rootProject.extra["androidxJunitVersion"]}")
	androidTestImplementation("androidx.test.espresso:espresso-core:${rootProject.extra["androidxEspressoCoreVersion"]}")
	implementation(project(":capacitor-cordova-android-plugins"))
}

apply(from = "capacitor.build.gradle")

try {
	val servicesJson = file("google-services.json")
	if (servicesJson.exists() && servicesJson.readText().isNotBlank()) {
		apply(plugin = "com.google.gms.google-services")
	}
} catch (_: Exception) {
	logger.info("google-services.json not found, google-services plugin not applied. Push Notifications won't work")
}