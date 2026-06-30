import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "io.jsonlviewer"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        instrumentationTools()
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.3")
    testRuntimeOnly("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            // No upper bound: the plugin only uses stable FileEditorProvider +
            // EditorFactory + JSON file-type APIs, so allow it to load in newer
            // IDEs (e.g. Rider 2026.1 / build 261).
            untilBuild = provider { null }
        }
    }

    // Compatibility check against real IDE builds: `./gradlew verifyPlugin`.
    // Downloads the JetBrains Plugin Verifier and the matching IDE builds, then
    // reports API problems against each — run before every Marketplace upload.
    // Pinned to released builds: recommended() pulled an unreleased version the
    // repository can't resolve. The plugin uses only stable APIs and has no
    // until-build cap, so newer IDEs load it too (verified empirically in-IDE).
    pluginVerification {
        ides {
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
        }
    }

    // Marketplace upload: `INTELLIJ_PUBLISH_TOKEN=... ./gradlew publishPlugin`.
    // Create the token at https://plugins.jetbrains.com/author/me/tokens
    // (kept out of the build script — supplied via environment variable).
    publishing {
        token = providers.environmentVariable("INTELLIJ_PUBLISH_TOKEN")
    }

    // Optional but recommended by JetBrains: sign the plugin before publishing.
    // Generate a key/cert (https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
    // and set these env vars; without them the plugin publishes unsigned.
    // signing {
    //     certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
    //     privateKey = providers.environmentVariable("PRIVATE_KEY")
    //     password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    // }
}

kotlin {
    jvmToolchain(21)
}
