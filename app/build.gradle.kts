plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.kidguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kidguard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}


configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
        eachDependency {
            if (requested.group == "com.intellij" && requested.name == "annotations") {
                useTarget("org.jetbrains:annotations:23.0.0")
            }
        }
    }
}

dependencies {


    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.google.firebase:firebase-analytics") {
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation("com.google.firebase:firebase-auth-ktx") {
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation("com.google.firebase:firebase-firestore-ktx") {
        exclude(group = "com.intellij", module = "annotations")
    }

    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.libraries.places:places:4.2.0")
    implementation("com.github.bumptech.glide:glide:4.15.1") {
        exclude(group = "com.intellij", module = "annotations")
    }

    implementation("org.jetbrains:annotations:23.0.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.room.compiler)
    implementation(libs.androidx.room.common.jvm)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
