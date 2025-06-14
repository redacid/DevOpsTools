import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.org.apache.commons.io.output.ByteArrayOutputStream
import java.nio.charset.Charset

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

//fun getGitHash(): String {
//    return try {
//        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
//            .start()
//
//        process.inputStream.bufferedReader().use { it.readText().trim() }
//    } catch (e: Exception) {
//        "unknown $e"
//    }
//}

abstract class GitCommitHash : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = output
        }
        return String(output.toByteArray(), Charset.defaultCharset()).trim()
    }
}

val gitCommitHashProvider = providers.of(GitCommitHash::class) {}
val buildNumber = gitCommitHashProvider.get()

//val buildNumber = getGitHash()
version = env.RELEASE_VERSION.value
val buildVersion = "$version-$buildNumber"

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "ua.in.ios.devopstools.MainKt"

        nativeDistributions {
            jvmArgs += listOf(
                "--add-modules=java.naming",
                "-DbuildVersion=$buildVersion"
            )
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "ua.in.ios.devopstools"
            packageVersion = version.toString()
        }
    }
}
