plugins {
 id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace="vn.cameracoach.app"; compileSdk=35
 defaultConfig { applicationId="vn.cameracoach.app"; minSdk=26; targetSdk=35; versionCode=5; versionName="0.5.0" }
 buildFeatures { compose=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies {
 val cameraX="1.4.1"
 implementation("androidx.core:core-ktx:1.15.0"); implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.compose.ui:ui:1.7.8"); implementation("androidx.compose.ui:ui-tooling-preview:1.7.8"); implementation("androidx.compose.material3:material3:1.3.1")
 debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")
 implementation("androidx.camera:camera-core:$cameraX"); implementation("androidx.camera:camera-camera2:$cameraX"); implementation("androidx.camera:camera-lifecycle:$cameraX"); implementation("androidx.camera:camera-view:$cameraX"); implementation("androidx.camera:camera-video:$cameraX")
 implementation("com.google.mlkit:object-detection:17.0.2")
 implementation("com.google.mlkit:face-detection:16.1.7")
}