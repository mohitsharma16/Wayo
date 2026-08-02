# Fix KSP Plugin Not Found Error

Update the KSP plugin version to a compatible version for Kotlin 2.2.10 and enable KSP2 for better performance and compatibility.

## Proposed Changes

### Build Configuration

#### [MODIFY] [build.gradle.kts](file:///C:/Users/mohit/Downloads/Wayo/Wayo/build.gradle.kts)
- Update KSP plugin version from `2.2.10-1.0.29` to `2.2.10-2.0.2`.

#### [MODIFY] [gradle.properties](file:///C:/Users/mohit/Downloads/Wayo/Wayo/gradle.properties)
- Add `ksp.useKSP2=true` to enable KSP2 as recommended for Kotlin 2.2+.

## Verification Plan

### Automated Tests
- Run Gradle sync to verify the plugin is found and the project syncs successfully.
- Run `./gradlew assembleDebug` to ensure the build works with KSP2.
