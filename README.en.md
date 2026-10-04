# Yummify

[Deutsch](README.md) · **English**

Yummify is an Android app for recipes, weekly meal planning, shopping lists and food inventory. It connects your Notion recipe and inventory databases to an interface built with Kotlin and Jetpack Compose. The app interface is currently in German.

## Features

### Recipes

- Load recipes from Notion, including all database results through pagination, page covers, categories, cuisines and tags.
- Search titles, descriptions and ingredients; filter by categories from existing recipes. Filters stay visible while scrolling.
- Scale ingredient quantities by changing the number of servings. The parser supports decimal numbers, fractions and common units.
- Display instructions from Notion page content, with support for headings, lists, quotes, callouts and text formatting.
- Manage favorites and write ratings of one to five stars back to Notion.
- Create recipes directly in Notion using the plus button: name, description, servings, category, ingredients, preparation steps and an optional HTTPS cover. The form validates inputs and the database schema; errors preserve the draft.
- Sample recipes are available without a configured Notion connection. This is not a complete offline synchronization of the recipe database.

### Weekly planner and widget

Schedule recipes for a date and meal slot: breakfast, lunch, dinner or snack. The planner is stored locally and supports browsing weeks and marking meals as cooked. For connected Notion recipes, the planned date is written back to the `Geplant am` date property when it exists and is writable. The planner does not show a daily nutrition summary at the bottom.

The home screen widget shows Monday through Sunday with the calendar week, dates and planned meals. It highlights today and labels empty days as having no planned meals. Tapping it opens the planner. The widget supports a minimum size of 4×2, resizing and a background transparency setting saved separately for each widget. Its configuration displays the actual layout with current plans. Changes to the plan, date and time zone update the display.

### Inventory

- Manage food with quantity, unit, category, storage location, minimum stock, best-before date, barcode, notes and image.
- Use compact search, a storage location selector and filters for low stock, approaching dates and expired items.
- Browse alphabetically sorted, collapsible category sections with item counts and contrasting headings that stay visible while scrolling. Uncategorized items have their own section; items within each group are sorted by best-before date and name.
- Edit items in a full-screen form with a quantity stepper, adjustable increments, selection options from the Notion schema and a date picker. Default increments are 1 for pieces and packages, 50 for g/ml and 0.1 for kg/l.
- Swipe left or right and confirm to delete an item. The delete action is also available to screen readers.
- Plus buttons in the inventory and recipe list account for the actual bottom navigation height, including larger font sizes. Navigation stays visible while scrolling the inventory.

### Barcodes and automatic product data

The Google Code Scanner reads barcodes using the camera. It requires Google Play services; the scanner module is downloaded before the first scan if necessary. Manual barcode entry is also available.

When adding an item, scanning automatically loads data from [Open Food Facts](https://world.openfoodfacts.org). After manual entry, use the product data lookup button. Requests use the public API with a German language preference and a Yummify User-Agent; Notion credentials are not sent to this service.

Empty fields are filled with the product name and brand, a matching existing category, notes and a product image. Scanned items default to **1 piece** (`Stück`), even if no product is found. Package weight or volume is recorded separately in the notes. Storage location and best-before date are entered manually. Check stock quantity and package unit before saving.

Nutrition, ingredients and the product URL are also mapped to existing Notion properties. Nutrition values are **per 100 g or 100 ml**, rather than per package or total stock. Barcodes already stored locally lead to the existing item; duplicate new items with the same barcode are prevented. Unknown products and network errors allow manual entry. Saving first stores the item locally; inventory synchronization then transfers it to Notion.

### Photos and product images

Existing Notion covers appear in item details. The image action explicitly offers the gallery or taking a new photo. User images are prepared in private app storage as JPEGs with corrected orientation and a maximum of 1600 pixels on the longer side. Synchronization uploads them through the Notion file upload API and sets them as page covers. Open Food Facts images are stored as external HTTPS covers. Pending images survive offline use and transfer errors; normal field edits do not replace an existing cover.

### Stock, cooking and shopping lists

Recipe ingredients show available inventory. Missing ingredients can be added to the shopping list based on selected servings and compatible units (g/kg, ml/l, pieces). Expired stock does not count as available. Matching uses identical names, ignoring case and whitespace; different product names are not automatically treated as equivalent.

The cooking action deducts the required quantities after confirmation, using batches with the earliest best-before date first. If an ingredient is missing, nothing is deducted. Low-stock items can be added to the shopping list without duplicate open entries.

Checked shopping items can be transferred with their quantities to the inventory and removed from the shopping list. A stored transfer ID prevents duplicate bookings after an interruption. Non-numeric quantities require manual entry; storage location and best-before date can be added afterwards.

## Set up Notion

1. Create a Notion integration with read, insert and update permissions, and share the required databases or parent pages with it.
2. Enter the integration token and recipe database ID in the app settings, save and test the connection.
3. Optionally enter an inventory database ID or use the inventory database creation action. The app creates or reuses `Yummify Inventar` under the recipe database's parent page. That page must be shared with the integration.
4. Test the inventory connection and synchronize. Inventory uses the same token as recipes.

Inventory integration and recipe creation use Notion API version `2025-09-03` and require a database with exactly one data source. The existing schema is checked before writing.

### Recipe database

The following names are actual Notion property names. Keep them unchanged even when using this English documentation.

| Notion property | Type | Purpose |
|---|---|---|
| Title property, e.g. `Name`, `Titel` or `Artikel` | `title` | Recipe name; required for new recipes |
| `Portionen` | `number` | Default servings; required for new recipes |
| `Zutaten` | `rich_text` | Ingredients, one per line; new recipes also accept `Lebensmittel` instead |
| `Beschreibung` | `rich_text` | Summary; required if filled in during creation |
| `Kategorie` | `select`, `multi_select` or `rich_text` | Category; required if filled in during creation |
| `Küche` | `select` or `multi_select` | Additional filters when reading |
| `Tags` | `multi_select` | Additional filters when reading |
| `Bewertung` | `select` or `number` | Star rating, e.g. `★` through `★★★★★` or 1 through 5 |
| `Geplant am` | `date` | Optional write-back of the planned date |
| Page content | Notion blocks | Instructions; new recipes store each step as a numbered list block |
| Page cover | External or uploaded image | Recipe image |

### Inventory database

| Preferred Notion property | Supported alternative | Type |
|---|---|---|
| `Artikel` | `Name` | `title` |
| `Bestand` | `Menge` | `number` |
| `nächstes MHD` | `Ablaufdatum` | `date` |
| `Kategorie` | — | `multi_select`, `select` or `rich_text` |
| `Einheit` | — | `select`, `multi_select` or `rich_text` |
| `Lagerort` | — | `select`, `multi_select` or `rich_text` |

New inventory databases use `Artikel`, `Bestand`, `nächstes MHD`, multiple selection for categories and single selection for units and storage locations. The app automatically adds missing metadata properties during connection testing or synchronization: `Mindestbestand` (`number`), `Barcode`, `Notizen` and `Artikel-ID` (each `rich_text`). This requires schema update permissions. Existing properties are not renamed or converted. Keep `Artikel-ID` unchanged because it supports recovery from interrupted transfers.

Multiple categories are preserved during synchronization. Empty quantities are read as 0; empty date and selection properties stay empty. Package units such as `Packung` and `Dose(n)` are not converted to grams. An optional `Status` property (`select`, `status` or `rich_text`) receives `Vorhanden` for positive stock or `Aufgebraucht` for 0, provided the respective option is supported.

### Product data from Open Food Facts

These additional properties are **used only if they already exist**; the app does not create them automatically.

| Notion property | Type | Source / reference amount |
|---|---|---|
| `Kalorien` or `kcal` | `number` | `energy-kcal_100g`, kcal per 100 g/ml |
| `Fett` | `number` | `fat_100g`, g per 100 g/ml |
| `Kohlenhydrate` | `number` | `carbohydrates_100g`, g per 100 g/ml |
| `Protein` | `number` | `proteins_100g`, g per 100 g/ml |
| `Zutaten` | `rich_text` | `ingredients_text_de`, falling back to `ingredients_text` |
| `URL` | `url` or `rich_text` | Open Food Facts product link |

If both `Kalorien` and `kcal` exist, `Kalorien` takes priority. Missing nutrition values remain unknown; actual zero values are transferred as 0. Items without product data do not overwrite existing Notion product details. Product data is retained locally across restarts and stock changes; long ingredient lists are split into Notion text chunks.

## Offline use and synchronization

Inventory changes are saved locally first. Pending edits, deletions and images are retained until successfully transferred. With automatic synchronization enabled, the app synchronizes on opening and after changes; pending changes are retried about once a minute while the app process is running. Manual synchronization is also available. With automatic synchronization disabled, transfers run manually only.

Inventory is separated by database. Initially local inventory is adopted when configuring a database for the first time. Unsynchronized local changes take priority in conflicts; changes from other devices are loaded during the next synchronization. The meal plan and shopping list are stored locally.

## Develop, build and install

Current app version: **1.1.0**, Android **8.0+ (API 26)**; compile/target SDK **34**.

You need JDK 17, Android SDK 34 and the Android build tools. Android Studio can be used for development. Configure the SDK path through `local.properties` (`sdk.dir=…`) or `ANDROID_HOME`.

```bash
# Build the debug APK
./gradlew assembleDebug

# Install and launch on a device connected through USB
adb -d install -r app/build/outputs/apk/debug/app-debug.apk
adb -d shell am start -n de.yummify.app/.MainActivity

# Run tests and Android Lint
./gradlew testDebugUnitTest lintDebug
```

Tests cover quantity calculations, expiration, atomic recipe stock deduction, date handling, pagination, Notion property mapping including `kcal`, recipe creation, error handling, interrupted transfer recovery, offline storage, duplicate shopping transfers and widget rendering.

A release build (`./gradlew assembleRelease`) requires the locally configured signing key `app/release.jks`. APKs, Gradle caches, SDK paths and signing keys are not tracked in Git.

### Technology and source code

Kotlin 1.9.23, Jetpack Compose with Material 3, Coroutines and StateFlow, OkHttp 4.12, Gson, Coil, SharedPreferences, Google Code Scanner and Android RemoteViews. Tests use JUnit, MockWebServer and Robolectric.

- [App entry point and navigation](app/src/main/java/de/yummify/app/MainActivity.kt)
- [Models, local storage, APIs and repositories](app/src/main/java/de/yummify/app/data)
- [Screens and styling](app/src/main/java/de/yummify/app/ui)
- [Widget](app/src/main/java/de/yummify/app/widget)
- [Tests](app/src/test/java/de/yummify/app)

## Data source and licenses

Open Food Facts provides product data under [ODbL](https://opendatacommons.org/licenses/odbl/1-0/), individual database contents under the [Database Contents License](https://opendatacommons.org/licenses/dbcl/1-0/) and product images under [CC BY-SA](https://creativecommons.org/licenses/by-sa/3.0/). Product sources and license notes remain available in item notes. These data licenses apply to the integrated product data and images; the repository currently contains no separate license file for the app source code.
