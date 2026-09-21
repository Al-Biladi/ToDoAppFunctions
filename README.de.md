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

## 2. AppFunctions implementieren

Die für AppFunctions verwendeten Datentypen werden mit `@AppFunctionSerializable` definiert. 
Anschließend stellt ein `AppFunctionService` die gewünschten Funktionen mit `@AppFunction` bereit.

KDoc-Beschreibungen erläutern Funktionen, Parameter und Datenfelder in natürlicher Sprache. 
Mit `isDescribedByKDoc = true` übernimmt der Compiler diese Beschreibungen in die generierten Metadaten.

## 3. Service registrieren

Der generierte AppFunctions-Service wird im `AndroidManifest.xml` registriert. 

## 4. AppFunctions testen

Nach Build und Installation können die registrierten AppFunctions über ADB geprüft und direkt aufgerufen werden. 

Für natürlichsprachige Tests steht zusätzlich der AppFunctions Testing Agent zur Verfügung.





