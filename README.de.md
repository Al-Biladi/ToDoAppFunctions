# AppFunctions zu einer bestehenden App hinzufügen
## Erweiterung einer To-do-App um AppFunctions.
## 1. Projekt vorbereiten

In `gradle/libs.versions.toml` stehen die Einträge für AppFunctions und KSP:

```toml
[versions] 
appfunctions = "1.0.0-alpha11" 
ksp = "2.3.10"

[libraries]
androidx-appfunctions = { module = "androidx.appfunctions:appfunctions", version.ref = "appfunctions" }
androidx-appfunctions-compiler = { module = "androidx.appfunctions:appfunctions-compiler", version.ref = "appfunctions" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Die projektweite build.gradle.kts stellt das KSP-Plug-in für die Module bereit:

```kotlin
plugins {
  alias(libs.plugins.ksp) apply false
}
```

In der app/build.gradle.kts wird KSP aktiviert und AppFunctions eingebunden:

```kotlin
plugins {
  alias(libs.plugins.ksp)
}

android {
  compileSdk = 37

  defaultConfig {
    minSdk = 26
    targetSdk = 36
  }
}

dependencies {
  implementation(libs.androidx.appfunctions)
  ksp(libs.androidx.appfunctions.compiler)
}
```
