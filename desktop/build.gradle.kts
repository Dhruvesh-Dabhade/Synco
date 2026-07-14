plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.remoteaudiosync.desktop"
version = "1.0.0"

application {
    mainClass.set("com.remoteaudiosync.desktop.MainKt")
    applicationName = "Synco"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp)
    implementation(libs.bouncycastle)
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation("io.javalin:javalin:5.6.3")
    implementation("org.slf4j:slf4j-simple:2.0.7")
}

sourceSets {
    main {
        java {
            srcDirs("../app/src/main/java")
            include("**/protocol/**")
            include("**/crypto/**")
            include("**/network/**")
            include("**/desktop/**")
            include("**/manager/**")
            
            exclude("**/IdentityKeyStore.kt")
            exclude("**/TrustedDeviceManager.kt")
            exclude("**/PairingManager.kt")
            exclude("**/manager/MediaManager.kt")
            exclude("**/manager/AndroidCallManager.kt")
            exclude("**/manager/AndroidNotificationManager.kt")
            exclude("**/manager/BluetoothOwnershipManager.kt")
            exclude("**/manager/BluetoothDeviceMonitor.kt")
        }
        kotlin {
            srcDirs("src/main/kotlin")
        }
    }
}

tasks.getByName<Sync>("installDist") {
    destinationDir = file("../build/install/Synco")
    finalizedBy("createRootLauncher")
}

val targetInstallDir = file("../build/install/Synco")

tasks.register("createRootLauncher") {
    val dir = targetInstallDir
    doLast {
        val batFile = File(dir, "Synco.bat")
        batFile.writeText(
            """@echo off
call "%~dp0bin\Synco.bat" %*
"""
        )
        println("[INFO] Created redirecting Windows launcher at: ${batFile.absolutePath}")

        val packageBat = File(dir, "package_jpackage.bat")
        packageBat.writeText(
            """@echo off
echo =============================================================
echo                     Synco - Native Windows packager
echo =============================================================
echo.
echo This script compiles a native Synco.exe installer using jpackage.
echo Prerequisite: A Windows machine with JDK 17+ installed.
echo.

jpackage --type exe ^
  --name "Synco" ^
  --app-version "1.0.0" ^
  --vendor "Synco Company" ^
  --description "Synco Desktop Client" ^
  --input "lib" ^
  --main-jar "desktop-1.0.0.jar" ^
  --main-class "com.remoteaudiosync.desktop.MainKt" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser

if %errorlevel% neq 0 (
    echo [ERROR] Native packaging failed. Ensure jpackage is on your PATH.
    pause
    exit /b %errorlevel%
)

echo [SUCCESS] Native package created successfully! Check your current folder.
pause
"""
        )
        println("[INFO] Created jpackage packaging batch script at: ${packageBat.absolutePath}")
    }
}
