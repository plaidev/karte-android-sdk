<a href="https://karte.io"><img src="https://karte.io/assets/images/common/logo_black.svg" width="270" height="80"></img></a>

[![codecov](https://codecov.io/gh/plaidev/karte-android-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/plaidev/karte-android-sdk)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://github.com/plaidev/karte-android-sdk/blob/master/LICENSE)

KARTE is a real-time user analytics & action tool.
This repository holds the source code for the Android version of KARTE SDK.

## Getting Started
Please see the detailed instructions in our docs to add [KARTE Android SDK](https://developers.karte.io/docs/android-sdk-v2) to your Android Studio project.

## Requirements
To integrate the KARTE Android SDK, the following requirements must be met:

- Kotlin 1.8.10 or higher
- Gradle 8.0.0 or higher
- Android Gradle Plugin: 8.1.1 or higher
- Optimizer: R8
- minSdkVersion 21 or higher

### Requirements Update Policy

This section defines the policy for raising the minimum required versions of build tools and SDK dependencies.

#### targetSdk / compileSdk

**Selection criteria:** The latest stable Android SDK version

**Update frequency:** Approximately 6 months after the stable Android SDK release (typically January-February of the following year)

**Update process:**
- Begin impact analysis and internal testing during the beta phase
- Perform final validation after stable release

**Rationale:** Google Play Store requires apps to target recent API levels (typically within 12 months of release). Early adoption allows our customers sufficient time to update their applications.

#### Android Gradle Plugin (AGP) / Gradle / Kotlin / JDK

**Selection criteria:** 

Based on the selected AGP version, we determine compatible versions for related build tools:

- **AGP:** Stable version released approximately one year before selection
- **Gradle:** Default version specified in [AGP release notes](https://developer.android.com/build/releases/past-releases)
- **Kotlin:** Compatible version from the [AGP compatibility matrix](https://developer.android.com/build/kotlin-support) (or the previous major version if not explicitly listed)
- **JDK:** Default version specified in [AGP release notes](https://developer.android.com/build/releases/past-releases)

**Rationale:**
- Balances stability and security through the use of well-established versions
- Default versions ensure officially verified compatibility between tools

## Documentation
The developer guide is located at
- [Developer Portal - KARTE Android SDK](https://developers.karte.io/docs/android-sdk-v2)

The API references are located at
- [Core](https://plaidev.github.io/karte-sdk-docs/android/core/latest/index.html)
- [InAppMessaging](https://plaidev.github.io/karte-sdk-docs/android/inappmessaging/latest/index.html)
- [Notifications](https://plaidev.github.io/karte-sdk-docs/android/notifications/latest/index.html)
- [Variables](https://plaidev.github.io/karte-sdk-docs/android/variables/latest/index.html)
- [VisualTracking](https://plaidev.github.io/karte-sdk-docs/android/visualtracking/latest/index.html)
- [Debugger](https://plaidev.github.io/karte-sdk-docs/android/debugger/latest/index.html)
- [InAppFrame](https://plaidev.github.io/karte-sdk-docs/android/inappframe/latest/index.html)

## Getting Help
- **Have a bug to report?**
  [Open a GitHub issue](https://github.com/plaidev/karte-android-sdk/issues/new?assignees=&labels=&template=bug_report.md). If possible, include the OS version, SDK version and full logs.
- **Have a feature request?**
  [Open a GitHub issue](https://github.com/plaidev/karte-android-sdk/issues/new?assignees=&labels=&template=feature_request.md). Tell us what the feature should do and why you want the feature.

## Contributing

Please follow our guidelines.
 - [Contribution Guideline](https://github.com/plaidev/karte-android-sdk/blob/master/CONTRIBUTING.md)
 - [Code of Conduct](https://github.com/plaidev/karte-android-sdk/blob/master/CODE_OF_CONDUCT.md)

## License
KARTE Android SDK is published under the Apache 2.0 License.

Your use of KARTE is governed by the [KARTE Terms of Use](https://karte.io/legal/terms-of-use-en.html).
