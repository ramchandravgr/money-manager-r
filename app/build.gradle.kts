import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android.plugin)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

val googleWebClientId: String =
    localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")?.trim().orEmpty()

android {
    namespace = "com.mmr.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mmr.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.mmr.app.HiltTestRunner"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        // Hilt @Inject on constructor params; flatMapLatest in ViewModels
        freeCompilerArgs.addAll(
            "-Xannotation-default-target=param-property",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.from(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
}

ktlint {
    version.set(libs.versions.ktlint.get())
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    baseline.set(rootProject.file("config/ktlint/baseline.xml"))
    filter {
        exclude("**/build/**")
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose.rules)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material")
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.fastexcel)
    implementation(libs.play.services.auth)
    implementation(libs.okhttp)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

val ktlintCliClasspath =
    configurations
        .detachedConfiguration(
            dependencies.create("com.pinterest.ktlint:ktlint-cli:${libs.versions.ktlint.get()}"),
        ).apply {
            isTransitive = true
        }

// AGP 9 + android.disallowKotlinSourceSets=false: ktlint-gradle only wires .kts tasks; run CLI on src/.
fun registerKtlintSrcTask(
    name: String,
    format: Boolean,
) = tasks.register<JavaExec>(name) {
    group = if (format) "formatting" else "verification"
    description = "ktlint ${if (format) "format" else "check"} on app/src/**/*.kt"
    classpath = ktlintCliClasspath
    mainClass.set("com.pinterest.ktlint.Main")
    workingDir = layout.projectDirectory.asFile
    val baseline = rootProject.file("config/ktlint/baseline.xml")
    val mode =
        buildList {
            if (format) add("-F")
            add("--baseline=${baseline.absolutePath}")
            add("src")
        }
    args(mode)
}

val ktlintCheckSources = registerKtlintSrcTask("ktlintCheckSources", format = false)
val ktlintFormatSources = registerKtlintSrcTask("ktlintFormatSources", format = true)

tasks.register<JavaExec>("ktlintGenerateSrcBaseline") {
    group = "verification"
    description = "Regenerate config/ktlint/baseline.xml from current app/src violations"
    classpath = ktlintCliClasspath
    mainClass.set("com.pinterest.ktlint.Main")
    workingDir = layout.projectDirectory.asFile
    isIgnoreExitValue = true
    val baseline = rootProject.file("config/ktlint/baseline.xml")
    args(
        "generateBaseline",
        "--baseline=${baseline.absolutePath}",
        "src",
    )
}

tasks.named("ktlintCheck") {
    dependsOn(ktlintCheckSources)
}

tasks.named("ktlintFormat") {
    dependsOn(ktlintFormatSources)
}

val adbExecutable =
    System.getenv("ANDROID_HOME")?.let { File(it, "platform-tools/adb.exe").absolutePath }
        ?: "${System.getenv("LOCALAPPDATA")}/Android/Sdk/platform-tools/adb.exe"

tasks.register<Exec>("pullUiTestScreenshots") {
    group = "verification"
    description = "Pull PNG UI traces from the connected device into app/build/ui-test-trace/"
    val dest = layout.buildDirectory.dir("ui-test-trace")
    val remote = "/sdcard/Android/data/com.mmr.app/files/ui-test-trace/"
    commandLine(adbExecutable, "pull", remote, dest.get().asFile.absolutePath)
    isIgnoreExitValue = true
}

afterEvaluate {
    tasks.findByName("connectedDebugAndroidTest")?.finalizedBy("pullUiTestScreenshots")
}
