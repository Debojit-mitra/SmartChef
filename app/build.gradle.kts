import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.bunny.ml.smartchef"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bunny.ml.smartchef"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            type = "String",
            name = "BASE_URL_CHAT",
            value = "\"${localProperties.getProperty("BASE_URL_CHAT", "")}\""
        )
        buildConfigField(
            type = "String",
            name = "API_BASE_URL",
            value = "\"${localProperties.getProperty("API_BASE_URL", "")}\""
        )
    }

    signingConfigs {
        create("release") {
            storeFile = file("D:\\Documents_And_Works\\ReleaseKeys\\SmartChef\\releaseKeys.jks")
            storePassword = "Debojit16@"
            keyAlias = "key0"
            keyPassword = "Debojit16@"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            buildFeatures {
                buildConfig = true
            }
            applicationIdSuffix = ".debug"
            isDebuggable = true
            resValue("string", "app_name", "SmartChef! Debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

//noinspection UseTomlInstead
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))

    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.work:work-runtime:2.10.0")
    implementation("com.google.guava:guava:32.1.3-android") //for update worker

    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0") {
        exclude(group = "glide-parent")
    }

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.airbnb.android:lottie:6.6.0")
    implementation("com.android.volley:volley:1.2.1")
}