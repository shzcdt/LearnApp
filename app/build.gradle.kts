plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.compose")
	id("org.jetbrains.kotlin.plugin.serialization")
	id("com.google.devtools.ksp")
}

android {
	namespace = "ai.learnharness.mobile"
	compileSdk = 35

	defaultConfig {
		applicationId = "ai.learnharness.mobile"
		minSdk = 26
		targetSdk = 35
		versionCode = 1
		versionName = "0.1.0"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		jvmTarget = "17"
	}
	buildFeatures {
		compose = true
	}
}

dependencies {
	implementation("androidx.core:core-ktx:1.15.0")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
	implementation("androidx.activity:activity-compose:1.9.3")
	implementation("androidx.navigation:navigation-compose:2.8.5")
	implementation("androidx.datastore:datastore-preferences:1.1.1")

	implementation(platform("androidx.compose:compose-bom:2024.12.01"))
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.material:material-icons-core")

	// Network: REST + SSE
	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
	implementation("com.squareup.retrofit2:retrofit:2.11.0")
	implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

	// Local cache (source of truth stays the server)
	implementation("androidx.room:room-runtime:2.6.1")
	implementation("androidx.room:room-ktx:2.6.1")
	ksp("androidx.room:room-compiler:2.6.1")

	// Markdown rendering for transcripts
	implementation("io.noties.markwon:core:4.6.2")

	testImplementation("junit:junit:4.13.2")
	testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
