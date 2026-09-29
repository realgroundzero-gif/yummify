# 🍲 Yummify - Smart Notion-Powered Recipe & Meal Planner

**Yummify** is a modern, beautiful Android application built with **Kotlin** and **Jetpack Compose (Material 3)**. It seamlessly connects with your **Notion Database** to turn your custom recipe collection into an interactive, beautifully styled digital cookbook and meal planning hub.

---

## ✨ Features

### 📖 1. Full Notion Database Sync & Pagination
* **Complete Database Sync**: Fetches **all** recipes from your Notion Database using Notion API v1 pagination (`has_more` & `next_cursor` handling).
* **Cover Header Images**: Uses Notion page cover headers (external or uploaded files) as high-resolution recipe hero images.
* **Offline Fallback**: Features built-in sample data when Notion is disconnected or unconfigured.

### 📝 2. Rich Markdown Instructions
* **Notion Page Block Parser**: Fetches page children blocks (`GET /v1/blocks/$pageId/children`) to retrieve body content.
* **Full Formatting Preservation**:
  * **Rich Text Formatting**: **Bold**, *Italic*, `Code`, and ~~Strikethrough~~.
  * **Structured Blocks**: Headings (`#`, `##`, `###`), bullet lists (`•`), numbered lists, quotes (`>`), and callouts (`💡`).
  * Rendered cleanly with custom `MarkdownText` Jetpack Compose components.

### ⭐ 3. Notion Star Rating & Real-time Write-Back
* **Dropdown & Numeric Translation**: Reads Notion rating properties (`select` dropdowns with star emojis `★`–`★★★★★` or numeric values) and translates them into a 1–5 star rating scale.
* **Interactive Write-Back**: Tap stars in the recipe detail screen to update your rating—automatically PATCHing the change back to Notion with fallback handling.

### 👥 4. Interactive Portions Stepper & Ingredient Scaling
* **Smart Ingredient Line Parser**: Automatically parses ingredient quantities (integers, decimals `1.5`, fractions `1/2`), units (`g`, `kg`, `ml`, `l`, `EL`, `TL`, `Stk.`), and ingredient names.
* **Reactive Multiplier**: Adjusting the portions stepper (e.g. from 1 to 2 servings) dynamically scales all ingredient amounts in real time (`200 g Mehl` → `400 g Mehl`, `2 Eier` → `4 Eier`).

### 📅 5. Meal Planner ("Für Datum planen")
* **Date & Slot Planning**: Plan any recipe for a specific date (*Heute*, *Morgen*, *Übermorgen*, or custom dates) and meal slot (*Frühstück*, *Mittagessen*, *Abendessen*, *Snack*).
* **Essensplan Tab Integration**: Planned recipes instantly appear in the **Essensplan** (Meal Planner) tab and persist locally across sessions.

### 🏷️ 6. Dynamic Categories & Search
* **Existing Category Extraction**: Automatically extracts real categories and tags (`Kategorie`, `Küche`, `Tags`) from your Notion database entries—no static quickfilters!
* **Instant Search**: Filter recipes by title, description, or ingredients in real time.

### ⚙️ 7. Settings & Notion Integration Hub
* Easily configure Notion Integration Token and Database ID.
* Features connection status badges and a manual sync trigger.

---

## 🛠️ Architecture & Tech Stack

* **Language**: Kotlin 1.9 / 2.0
* **UI Framework**: Jetpack Compose with Material 3 Design Tokens
* **Async & State**: Kotlin Coroutines & `StateFlow` / `MutableStateFlow`
* **Network**: OkHttp 4 (with HttpLoggingInterceptor) & Gson
* **Image Loading**: Coil Compose
* **Local Storage**: SharedPreferences with reactive `StateFlow` wrappers

---

## 📋 Notion Database Schema

To connect Yummify to your Notion database, create a Notion Database with the following properties:

| Property Name | Property Type | Description |
|---|---|---|
| **Name** / **Titel** | `Title` | Recipe title |
| **Cover** | Page Cover Image | Recipe cover image (Notion Page Header) |
| **Beschreibung** | `Rich Text` | Short recipe summary |
| **Bewertung** | `Select` / `Number` | Rating options (`★`, `★★`, `★★★`, `★★★★`, `★★★★★` or numbers `1`–`5`) |
| **Portionen** | `Number` | Default number of servings (e.g. `2`) |
| **Kategorie** | `Select` / `Multi-select` | Recipe categories (e.g., *Hauptgericht*, *Dessert*) |
| **Küche** | `Select` / `Multi-select` | Cuisine type (e.g., *Italienisch*, *Asiatisch*) |
| **Tags** | `Multi-select` | Extra tags (e.g., *Vegetarisch*, *Schnell*) |
| **Zutaten** | `Rich Text` | Ingredient list line-by-line (e.g., `200g Mehl\n2 Eier`) |
| **Page Body** | Notion Page Content | Instructions written directly in the Notion page body |

---

## 🚀 Getting Started

### 1. Prerequisites
* **Android Studio** (Hedgehog or newer)
* **JDK 17**
* **Android SDK** (API 26+)

### 2. Build & Install via CLI
```bash
# Set environment variables
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk

# Build Debug APK
./gradlew assembleDebug

# Install and Launch on connected device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n de.yummify.app/.MainActivity
```

---

## 📱 License & Usage

Created for Yummify Recipe Management. Designed with ❤️ using Google DeepMind Antigravity guidelines.
