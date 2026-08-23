plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace="com.meurrumo.app"
 compileSdk=36
 defaultConfig {
  applicationId="com.meurrumo.app"
  minSdk=28
  targetSdk=36
  versionCode=4
  versionName="4.0"
 }
}
dependencies {
 implementation("androidx.core:core-ktx:1.16.0")
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
 implementation("androidx.compose.ui:ui:1.8.3")
 implementation("androidx.compose.ui:ui-tooling-preview:1.8.3")
 implementation("androidx.compose.material3:material3:1.3.2")
 implementation("androidx.health.connect:connect-client:1.1.0")
 debugImplementation("androidx.compose.ui:ui-tooling:1.8.3")
}


// JVM 17
kotlinOptions { jvmTarget = "17" }
