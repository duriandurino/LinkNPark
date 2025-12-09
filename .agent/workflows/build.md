---
description: Build and test LinkNPark Android app
---

# LinkNPark Development Workflow

## Build Debug APK
// turbo
1. Run `.\gradlew assembleDebug` in the project root

## Clean Build
// turbo
2. Run `.\gradlew clean assembleDebug` in the project root

## Check for Errors Only (faster)
// turbo
3. Run `.\gradlew compileDebugKotlin` in the project root

## Install on Device
4. Run `.\gradlew installDebug` in the project root

## Run Tests
// turbo
5. Run `.\gradlew testDebugUnitTest` in the project root
