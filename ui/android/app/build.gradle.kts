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

fun Project.stringGradleProperty(name: String): String = providers.gradleProperty(name).get()

fun Project.intGradleProperty(name: String): Int = stringGradleProperty(name).toInt()

fun getVersionCode(): Int {
	val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
	return dateStr.toInt()
}

fun resolveVersion(): OpenemsVersion {
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

val version = resolveVersion()

val androidCompileSdk = intGradleProperty("android.compileSdkVersion")
val androidMinSdk = intGradleProperty("android.minSdkVersion")
val androidTargetSdk = intGradleProperty("android.targetSdkVersion")
val androidxAppCompatVersion = stringGradleProperty("androidx.appCompatVersion")
val androidxCoordinatorLayoutVersion = stringGradleProperty("androidx.coordinatorLayoutVersion")
val coreSplashScreenVersion = stringGradleProperty("androidx.coreSplashScreenVersion")
val junitVersion = stringGradleProperty("test.junitVersion")
val androidxJunitVersion = stringGradleProperty("test.androidxJunitVersion")
val androidxEspressoCoreVersion = stringGradleProperty("test.androidxEspressoCoreVersion")

android {
	compileSdk = androidCompileSdk

	defaultConfig {
		minSdk = androidMinSdk
		targetSdk = androidTargetSdk
		versionCode = version.code
		versionName = version.name
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		androidResources {
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

dependencies {
	implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
	implementation("androidx.appcompat:appcompat:$androidxAppCompatVersion")
	implementation("androidx.coordinatorlayout:coordinatorlayout:$androidxCoordinatorLayoutVersion")
	implementation("androidx.core:core-splashscreen:$coreSplashScreenVersion")
	implementation(project(":capacitor-android"))
	testImplementation("junit:junit:$junitVersion")
	androidTestImplementation("androidx.test.ext:junit:$androidxJunitVersion")
	androidTestImplementation("androidx.test.espresso:espresso-core:$androidxEspressoCoreVersion")
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