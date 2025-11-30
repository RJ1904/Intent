# Project Knowledge: Intent (formerly Control Buddy)

## Project Overview

**Intent** (previously "Control Buddy") is a digital wellbeing Android application designed to help users become more mindful of their app usage. It uses accessibility services to monitor app activity and intervenes with pop-ups to prompt reflection.

## Core Features

### 1. Intervention Pop-ups

* **Triggers**:
  * **On Open**: Triggered immediately when a user opens a monitored application.
  * **Periodic**: Re-appears after a set duration of continuous use (e.g., every 10 minutes).
* **Interaction**:
  * Prompts the user to select their current **Mood** from predefined options.
  * Includes a **Notes** field to record the intention or trigger for opening the app.
  * **Timers**: Enforces a delay before dismissal. Options include 5 seconds, 10 seconds, and a user-defined **Custom Timer**.

### 2. App Selection & Management

* **Selection**: Users can toggle monitoring for specific installed applications.
* **Filtering**:
  * System applications are excluded to focus on user-installed apps.
  * Uninstalled apps are automatically removed from the list.
* **Performance**: The `AppSelectionScreen` has been optimized for smooth scrolling, addressing previous frame-drop issues.

### 3. Dashboard & History

* **Usage Stats**: Displays total screen time and session counts.
* **History**: Logs every intervention, storing the app name, timestamp, recorded mood, and user notes.
* **Navigation**: Optimized back-navigation behavior (e.g., returning to Dashboard from details instead of exiting).

### 4. Home Screen Widget

* **Type**: Material 3 Expressive large widget.
* **Metrics**: Focuses on **Screen Time** and **Sessions**.
* **Design**:
  * Removed "Top 3 Apps" section for a cleaner look.
  * Supports dynamic resizing and layout adjustments.
  * Themed for both Light and Dark modes.
  * Includes a refresh button and the Intent logo.

## Technical Architecture

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose
* **Architecture**: MVVM (Model-View-ViewModel)
* **Dependency Injection**: Hilt
* **Local Storage**: Room Database (for persisting usage logs, moods, and notes)
* **Core Mechanism**: `AccessibilityService` (used to detect foreground application changes and display overlays)
* **Theme**: `Theme.Intent` (migrated from `Theme.ControlBuddy`)

## Development Timeline & Key Events

### Phase 1: Inception & Core Features (Mindful App / Control Buddy)

* Established PRD: Goals set for local storage, accessibility service, and mood tracking.
* Implemented core database and service layers.

### Phase 2: Renaming & Refactoring (Nov 29)

* **Renaming**: Project renamed from "Control Buddy" to "**Intent**".
* **Refactoring**: Updated package names, manifest entries, and theme references.
* **Recovery**: Resolved issues with source file loss during renaming via Local History.

### Phase 3: Refinement & Optimization (Nov 30)

* **Widget Overhaul**: Refined layout, fixed compilation errors (`setPorterDuffColorFilter`), and simplified content (removed Top 3 apps).
* **Bug Fixes**:
  * Fixed random popup triggers.
  * Filtered out uninstalled apps.
  * Fixed `AndroidManifest.xml` theme errors.
* **Code Quality**: Cleaned up unused parameters in `AppSelectionScreen` and `DashboardScreen`.

## Known Issues & Recent Fixes

* **Fixed**: Scrolling performance in App List.
* **Fixed**: Compilation errors in `StatsWidgetProvider`.
* **Fixed**: Popups not appearing for some apps (resolved by debugging accessibility service and permissions).
