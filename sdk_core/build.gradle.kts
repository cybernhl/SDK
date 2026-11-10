import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)//FIXME do not use!! can not access library !!
//    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.de.ktorfit)
    id("maven-publish")
}

group = "com.github.cybernhl.SDK"
version = "1.0-SNAPSHOT"

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, "seconds")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvm()
    android {
        namespace = "chat.im.trilivechat.chatedin"
        compileSdk = 36
        minSdk = 23

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        lint {
            warningsAsErrors = true
            abortOnError = true
            disable.addAll(
                listOf(
                    "MissingTranslation",
                    "ExtraTranslation",
                    "TypographyEllipsis",
                    "UnspecifiedImmutableFlag",
                    "UnusedResources",
                    "TypographyDashes",
                    "UseTomlInstead"
                )
            )
        }

        // 放在這裡
        packaging {
            resources {
                excludes += "META-INF/INDEX.LIST"
                //千萬不要在 library  庫模組中排除 *.kotlin_module 這會把 Kotlin 的頂層符號元資料全部濾掉 excludes += "META-INF/*.kotlin_module" 只能用在最終打包 App 的 APK 專案中防止重複檔案衝突，絕不能用在要發布給別人引用的 Library 專案中
//                excludes += "META-INF/*.kotlin_module"
                excludes += "META-INF/LICENSE"
                excludes += "META-INF/LICENSE.txt"
                excludes += "META-INF/NOTICE"
                excludes += "META-INF/io.netty.versions.properties"
                excludes += "META-INF/AL2.0"
                excludes += "META-INF/LGPL2.1"
            }
        }
    }


    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "sdk_core"
            isStatic = true
            binaryOption("bundleId", "chat.im.trilivechat.chatedin.sdkcore") // 明確指定 Bundle ID
        }
    }

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    sourceSets {

        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.koin.core)

            implementation(libs.de.ktorfit)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            api("com.github.cybernhl.GeneralModule:DI_TAGS:0.0.1")
            api("com.github.cybernhl.GeneralModule:utils:0.0.1")
            api("com.github.cybernhl.Entity:general:0.0.7")
            api("com.github.cybernhl.Entity:trilivechat:0.0.7")

            api("com.github.cybernhl.GeneralModule:network:0.0.1")
            api("com.github.cybernhl.GeneralModule:resfulclient:0.0.1")

        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)

        }

        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.jetbrains.compose.ui.tooling)
        }

        iosMain {
            dependencies{
                implementation(libs.ktor.client.darwin)
            }
        }

        jsMain.dependencies {
//            implementation(libs.wrappers.browser)
        }
    }
}

tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn(tasks.matching { it.name.startsWith("ksp") })
}

// ==============================================================================
// 🛡️ AAR / Library 全方位必要性與完整性自動驗證 Task (Complete Necessity Checker)
// ==============================================================================

tasks.register("verifyLibraryIntegrity") {
    group = "verification"
    description = "全方位檢查打包出的 AAR 是否包含 AndroidManifest、Class、資源與 Kotlin 元資料"
    dependsOn(tasks.matching { it.name.startsWith("assemble") })

    doLast {
        val aarFiles = fileTree(layout.buildDirectory.dir("outputs/aar")) {
            include("**/*.aar")
        }

        if (aarFiles.isEmpty) {
            println("⚠️ 未在 outputs/aar 找到 AAR 檔案，請先執行 assemble 任務。")
            return@doLast
        }

        val errors = mutableListOf<String>()

        aarFiles.forEach { aar ->
            println("\n==================================================")
            println("🔍 [開始檢驗 AAR 完整性]: ${aar.name}")
            println("==================================================")

            ZipFile(aar).use { zip ->
                val entries = zip.entries().asSequence().map { it.name }.toSet()

                // 1. 必要性檢查：AndroidManifest.xml
                if (!entries.contains("AndroidManifest.xml")) {
                    errors.add("${aar.name}: ❌ 缺少 AndroidManifest.xml！Host App 將無法讀取 AAR 的清單資訊。")
                } else {
                    println("  ✅ AndroidManifest.xml 存在")
                }

                // 2. 檢查 R.txt / 資源索引 (若有)
                if (entries.contains("R.txt")) {
                    println("  ✅ R.txt 資源索引檔存在")
                }

                // 3. 必要性檢查：classes.jar 及其內部結構
                val classesJarEntry = zip.getEntry("classes.jar")
                if (classesJarEntry == null) {
                    errors.add("${aar.name}: ❌ 缺少 classes.jar！Library 沒有包含任何可執行的代碼。")
                } else {
                    println("  ✅ classes.jar 存在")

                    // 解開 classes.jar 深入檢查 Bytecode 與 Kotlin Metadata
                    val tempJar = File.createTempFile("classes_check", ".jar")
                    try {
                        tempJar.writeBytes(zip.getInputStream(classesJarEntry).readBytes())
                        ZipFile(tempJar).use { classesZip ->
                            val classEntries = classesZip.entries().asSequence().map { it.name }.toList()

                            // (A) 必要性檢查：是否含有編譯後的 .class 檔
                            val classFiles = classEntries.filter { it.endsWith(".class") }
                            if (classFiles.isEmpty()) {
                                errors.add("${aar.name}: ❌ classes.jar 內部沒有任何 .class 檔案！代碼未成功編譯。")
                            } else {
                                println("  ✅ classes.jar 包含 ${classFiles.size} 個 .class 編譯檔")
                            }

                            // (B) 必要性檢查：是否含有 Kotlin 模組元資料 (*.kotlin_module)
                            val kotlinModules = classEntries.filter { it.contains(".kotlin_module") }
                            if (kotlinModules.isEmpty()) {
                                errors.add("${aar.name}: ❌ classes.jar 缺少 'META-INF/*.kotlin_module'！這會導致外部引用時發生 Unresolved reference (找不到頂層函數 App() 或 commonModule 等)。")
                            } else {
                                println("  ✅ Kotlin 模組元資料檔 (.kotlin_module) 存在 (${kotlinModules.size} 個)")
                                kotlinModules.forEach { println("      ↳ $it") }
                            }
                        }
                    } finally {
                        tempJar.delete()
                    }
                }
            }
        }

        // 總結產出結果
        if (errors.isNotEmpty()) {
            val failureReport = StringBuilder()
            failureReport.append("\n❌❌❌ [SDK AAR 完整性檢查未通過] ❌❌❌\n")
            errors.forEach { failureReport.append("  • $it\n") }
            failureReport.append("\n👉 請檢查 build.gradle.kts 內的 packaging / packagingOptions 配置，確保沒有誤刪關鍵資源與元資料。")
            throw GradleException(failureReport.toString())
        } else {
            println("\n🎉 [SUCCESS]: 所有 AAR 產物均通過黃金完整性檢驗（Manifest、Classes、Kotlin Metadata 均健全）！")
        }
    }
}

// 自動綁定：每次執行 assemble 或 publish 時自動執行全方位檢驗
tasks.matching { it.name.startsWith("publish") || it.name == "assemble" }.configureEach {
    finalizedBy("verifyLibraryIntegrity")
}

/**
 * 1. 【列出打包後全部資料 Task】
 * 執行 `./gradlew inspectAar` 即可詳細列出 outputs/aar 內部 Classes 與所有資源清單
 */
tasks.register("inspectAar") {
    group = "verification"
    description = "解包並列出 outputs/aar 內所有檔案與 classes.jar 內容"
    dependsOn(tasks.matching { it.name.startsWith("assemble") })

    doLast {
        val aarFiles = fileTree(layout.buildDirectory.dir("outputs/aar")) {
            include("**/*.aar")
        }

        if (aarFiles.isEmpty) {
            println("⚠️ 未找到 AAR 檔案，請先執行 assemble 任務。")
            return@doLast
        }

        aarFiles.forEach { aar ->
            println("\n==================================================")
            println("📦 [AAR 檔案名]: ${aar.name} (${aar.length() / 1024} KB)")
            println("==================================================")

            ZipFile(aar).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    println(" 📄 [AAR Root] ${entry.name}")
                }

                val classesJarEntry = zip.getEntry("classes.jar")
                if (classesJarEntry != null) {
                    println("\n 📁 [classes.jar 內部結構]:")
                    val tempJar = File.createTempFile("inspect_classes", ".jar")
                    try {
                        tempJar.writeBytes(zip.getInputStream(classesJarEntry).readBytes())
                        ZipFile(tempJar).use { classesZip ->
                            classesZip.entries().asSequence().forEach { classEntry ->
                                val prefix = if (classEntry.name.endsWith(".kotlin_module")) "⭐ [Kotlin Module] " else "    "
                                println("$prefix${classEntry.name}")
                            }
                        }
                    } finally {
                        tempJar.delete()
                    }
                }
            }
        }
    }
}

/**
 * 2. 【必要性自動檢查 Task】
 * 執行 `./gradlew verifyKotlinModule`
 * 只要 classes.jar 缺少 *.kotlin_module 檔，直接報錯中斷 Task！
 */
tasks.register("verifyKotlinModule") {
    group = "verification"
    description = "自動檢查 AAR 是否包含關鍵的 *.kotlin_module 元資料"
    dependsOn(tasks.matching { it.name.startsWith("assemble") })

    doLast {
        val aarFiles = fileTree(layout.buildDirectory.dir("outputs/aar")) {
            include("**/*.aar")
        }

        val missingModuleLibraries = mutableListOf<String>()

        aarFiles.forEach { aar ->
            ZipFile(aar).use { zip ->
                val classesJarEntry = zip.getEntry("classes.jar")
                if (classesJarEntry != null) {
                    val tempJar = File.createTempFile("verify_classes", ".jar")
                    try {
                        tempJar.writeBytes(zip.getInputStream(classesJarEntry).readBytes())
                        ZipFile(tempJar).use { classesZip ->
                            val hasKotlinModule = classesZip.entries().asSequence().any {
                                it.name.contains(".kotlin_module")
                            }
                            if (!hasKotlinModule) {
                                missingModuleLibraries.add(aar.name)
                            }
                        }
                    } finally {
                        tempJar.delete()
                    }
                }
            }
        }

        if (missingModuleLibraries.isNotEmpty()) {
            val errorMsg = """

                ❌❌❌ [SDK 打包必要性檢查失敗] ❌❌❌
                下列 AAR 產物缺少關鍵的 'META-INF/*.kotlin_module' 元資料檔：
                ${missingModuleLibraries.joinToString("\n") { "  - $it" }}

                👉 原因：這會導致外部引用此 SDK 時發生 Unresolved reference (找不到頂層函數 App() 與 commonModule 等)。
                👉 解法：請檢查 build.gradle.kts 中是否包含了 excludes += "META-INF/*.kotlin_module" 或 excludes += "META-INF/*"，並將其移除。
            """.trimIndent()

            throw GradleException(errorMsg)
        } else {
            println("\n✅ [VERIFICATION PASSED]: 所有 AAR 產物均成功包含 *.kotlin_module 元資料！")
        }
    }
}

// 綁定：每次執行 assemble 或 publish 時自動觸發驗證
tasks.matching { it.name.startsWith("publish") || it.name == "assemble" }.configureEach {
    finalizedBy("verifyKotlinModule")
}

//# 1. 檢查成功的   API Jar：
//jar tf $(find ~/.gradle/caches/9.4.1/transforms/ -name "shared-android-trilivechat.0.0.5-api.jar") | grep META-INF
//
//# 2. 檢查失敗的   API Jar：
//jar tf $(find ~/.gradle/caches/9.4.1/transforms/ -name "shared-android-trilivechat.0.0.a-api.jar") | grep META-INF

