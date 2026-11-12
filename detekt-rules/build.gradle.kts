plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:${property("detektVersion")}")
}

group = "com.rrparallelstudio.miapp"
version = "1.0.0"
