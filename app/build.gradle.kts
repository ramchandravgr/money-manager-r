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

fun propOrDefault(
    key: String,
    default: String,
): String = localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: default

// Google sample IDs — debug / until real units are set in local.properties
val admobAppId =
    propOrDefault(
        "ADMOB_APP_ID",
        "ca-app-pub-3940256099942544~3347511713",
    )
val admobUnitHomeBanner =
    propOrDefault(
        "ADMOB_UNIT_HOME_BANNER",
        "ca-app-pub-3940256099942544/9214589741",
    )
val admobUnitHomeMrec =
    propOrDefault(
        "ADMOB_UNIT_HOME_MREC",
        "ca-app-pub-3940256099942544/6300978111",
    )
val admobUnitStatsNative =
    propOrDefault(
        "ADMOB_UNIT_STATS_NATIVE",
        "ca-app-pub-3940256099942544/2247696110",
    )

android {
    namespace = "com.rrparallelstudio.miapp"
    compileSdk = 36

    signingConfigs {
        val uploadStoreFile =
            localProperties.getProperty("UPLOAD_STORE_FILE")?.trim()?.takeIf { it.isNotEmpty() }
        val uploadStorePassword = localProperties.getProperty("UPLOAD_STORE_PASSWORD")?.trim()
        val uploadKeyAlias = localProperties.getProperty("UPLOAD_KEY_ALIAS")?.trim()
        val uploadKeyPassword = localProperties.getProperty("UPLOAD_KEY_PASSWORD")?.trim()
        require(
            uploadStoreFile != null &&
                uploadStorePassword != null &&
                uploadKeyAlias != null &&
                uploadKeyPassword != null,
        ) {
            "Play requires a release upload keystore. Set UPLOAD_STORE_FILE, UPLOAD_STORE_PASSWORD, " +
                "UPLOAD_KEY_ALIAS, and UPLOAD_KEY_PASSWORD in local.properties " +
                "(see local.properties.example). Debug signing is rejected by Play."
        }
        create("release") {
            storeFile = rootProject.file(uploadStoreFile!!)
            storePassword = uploadStorePassword
            keyAlias = uploadKeyAlias
            keyPassword = uploadKeyPassword
        }
    }

    defaultConfig {
        applicationId = "com.rrparallelstudio.miapp"
        minSdk = 24
        targetSdk = 36
        // Play versionCode: major*10000 + minor*100 + patch (0.10.1 -> 1001). Bump every upload.
        versionCode = 1117
        versionName = "0.11.17"

        testInstrumentationRunner = "com.rrparallelstudio.miapp.HiltTestRunner"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField(
            "boolean",
            "ADS_DEBUG_PLACEHOLDERS",
            if (localProperties.getProperty("ADS_DEBUG_PLACEHOLDERS")?.trim() == "true") "true" else "false",
        )
        buildConfigField(
            "boolean",
            "CRASHLYTICS_DEBUG",
            if (localProperties.getProperty("CRASHLYTICS_DEBUG")?.trim() == "true") "true" else "false",
        )
        buildConfigField("String", "ADMOB_UNIT_HOME_BANNER", "\"$admobUnitHomeBanner\"")
        buildConfigField("String", "ADMOB_UNIT_HOME_MREC", "\"$admobUnitHomeMrec\"")
        buildConfigField("String", "ADMOB_UNIT_STATS_NATIVE", "\"$admobUnitStatsNative\"")
        manifestPlaceholders["admobAppId"] = admobAppId
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // Initial Play releases: minify off (easier release debugging; see PROJECT_MEMORY §6 Release builds, audit §12 RELEASE-R8).
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
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        lintConfig = file("lint.xml")
        baseline = file("lint-baseline.xml")
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
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose.rules)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation(libs.androidx.core.splashscreen)
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
    implementation(libs.play.services.ads)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.crashlytics)
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
    val srcRoots =
        listOf(
            "src/main/java",
            "src/test/java",
            "src/androidTest/java",
        )
    val mode =
        buildList {
            if (format) add("-F")
            add("--baseline=${baseline.absolutePath}")
            addAll(srcRoots)
        }
    args(*mode.toTypedArray())
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
    val generateArgs =
        buildList {
            add("generateBaseline")
            add("--baseline=${baseline.absolutePath}")
            add("src/main/java")
            add("src/test/java")
            add("src/androidTest/java")
        }
    args(*generateArgs.toTypedArray())
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
    val remote = "/sdcard/Android/data/com.rrparallelstudio.miapp/files/ui-test-trace/"
    commandLine(adbExecutable, "pull", remote, dest.get().asFile.absolutePath)
    isIgnoreExitValue = true
}

afterEvaluate {
    tasks.findByName("connectedDebugAndroidTest")?.finalizedBy("pullUiTestScreenshots")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
