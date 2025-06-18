# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **KARTE Android SDK** - a real-time user analytics and engagement platform. The SDK provides event tracking, in-app messaging, push notifications, A/B testing variables, visual tracking, and various user engagement features for Android applications.

## Development Commands

### Code Quality & Testing
```bash
# Format code with ktlint
./gradlew ktlintFormat

# Run ktlint checks
./gradlew ktlint

# Run tests with coverage report
./gradlew jacocoMerge

# Build all modules
./gradlew build

# Run tests for specific module
./gradlew core:test
./gradlew inappmessaging:testDebugUnitTest
```

### Development Workflow
- **Main branch**: `develop` (not master)
- **Testing**: Unit tests required for code changes, use Robolectric for Android components
- **Code style**: ktlint enforced, run `./gradlew ktlintFormat` before commits
- **Commits**: Atomic commits (one feature per commit)

## Architecture

### Module Structure
- **`core`** - Event tracking, SDK foundation, visitor management
- **`inappmessaging`** - In-app messaging and campaign delivery
- **`notifications`** - Push notification handling and tracking  
- **`variables`** - A/B testing and configuration variables
- **`visualtracking`** - UI interaction tracking and event capture
- **`inbox`** - Message inbox functionality
- **`inappframe`** - In-app display components with Compose support
- **`debugger`** - Development tools for event debugging
- **`gradle-plugin`** - Custom Gradle plugin for bytecode transformation
- **`test_lib`** - Shared testing utilities across modules

### Key Architecture Patterns

**Plugin-Based Library System:**
- Uses `ServiceLoader` for automatic library discovery
- Each module implements `Library` interface
- Modular design allows selective feature inclusion

**Event-Driven Architecture:**
- Central event tracking through `TrackingService`
- Automatic lifecycle event tracking
- Circuit breaker patterns for robust event processing

**Module System:**
- `Module` interface for extensible functionality
- Specialized modules: `ActionModule`, `CommandModule`, `DeepLinkModule`, `NotificationModule`
- Command URI processing for cross-module communication

### Entry Points
- **KarteApp** - Main SDK entry point and application manager
- **Config.Builder** - SDK configuration and setup
- Module-specific APIs: `Variables.fetch()`, `InAppMessaging`, etc.

## Development Guidelines

### Contribution Policy
- Feature PRs not currently accepted
- Bug fixes considered case-by-case
- Contact account manager before major changes

### Testing Strategy
- Unit tests with Robolectric for Android components
- Integration tests for cross-module functionality
- Shared test utilities in `test_lib` module
- MockWebServer for API testing

### Technology Stack
- **Language**: Kotlin 1.8.10 with Java interoperability
- **Build**: Gradle with Kotlin DSL, Android Gradle Plugin 8.1.1
- **Min SDK**: 21, Target SDK: 34
- **Testing**: JUnit, Robolectric, MockK, Truth
- **Code Quality**: ktlint 0.36.0, Jacoco coverage

## Common Development Patterns

### SDK Initialization
```kotlin
val config = Config.Builder().apply {
    appKey = "YOUR_APP_KEY"
    apiKey = "YOUR_API_KEY"
}.build()
KarteApp.setup(application, config)
```

### Module Registration
Modules auto-register via `ServiceLoader` and implement `Library` interface for configuration.

### Event Tracking
Core event system handles automatic lifecycle tracking, with circuit breaker and rate limiting for robust processing.