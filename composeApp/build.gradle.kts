import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.org.apache.commons.io.output.ByteArrayOutputStream
import java.nio.charset.Charset

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

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
            implementation(compose.material)
            implementation(compose.preview)
            implementation("br.com.devsrsouza.compose.icons:feather:1.1.1") // https://feathericons.com/
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("com.google.code.gson:gson:2.10.1")
        }
//        commonTest.dependencies {
//            implementation(libs.kotlin.test)
//        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

tasks.register("desktopRun2") {
    group = "application"
    description = "Runs the desktop application"
    dependsOn("run")
}



compose.desktop {
    application {
        mainClass = "ua.in.ios.devopstools.MainKt"

        jvmArgs += listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
        )

        buildTypes.release.proguard {
            isEnabled.set(false)
            //configurationFiles.from("proguard-rules.pro")
            jvmArgs += "-DbuildVersion=$buildVersion"

        }

        nativeDistributions {
            jvmArgs += listOf(
                "--add-modules=java.naming",
                "-DbuildVersion=$buildVersion"
            )
            modules(
                "java.naming",
                "java.security.jgss",
                "java.security.sasl",
                "jdk.naming.dns",
                "java.management",
                "java.net.http",
                "jdk.management"
            )
            //targetFormats(TargetFormat.Deb)
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "devopstools"
            packageVersion = version.toString()
            description = "DevOps Tools Installer"
            linux {
                iconFile.set(project.file("../devopstools.png"))
                menuGroup = "Development;System"
                shortcut = true
                debMaintainer = "Serhii Rudenko <sr@ios.in.ua> (https://github.com/redacid/devopstools)"
                copyright = "Copyright (c) 2025 Serhii Rudenko (https://github.com/redacid/devopstools)"
                //appCategory = "System"
                //appRelease = buildNumber
                appRelease = "1"
                debPackageVersion = packageVersion
                rpmPackageVersion = packageVersion
                rpmLicenseType = "Apache-2.0"
                appCategory = "Development/Tools"
            }
        }
    }
}
