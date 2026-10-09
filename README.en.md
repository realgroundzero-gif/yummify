# Yummify

[Deutsch](README.md) · **English**

Yummify is an Android app for recipes, weekly meal planning, shopping lists and food inventory. It connects your Notion recipe and inventory databases to an interface built with Kotlin and Jetpack Compose. The interface is mostly German; the appearance settings and grid cards are also available in English.

## Releases

| Version | Date | Summary |
|---|---|---|
| v1.4.0 | planned (no tag yet) | Settings with sub menus: Appearance, Data care, Data and backup (export, import, storage, delete) |

No release tag has been published yet. A row is added once the Git tag `vX.Y.Z` and its APK exist.

## Features

### Recipes

- Load recipes from Notion, including all database results through pagination, page covers, categories, cuisines and tags. Recipes are loaded once per connection and shared by all screens; reload them with “Erneut versuchen” (retry) or the synchronization in the settings.
- The last loaded recipes are stored on the device. Without a network the app shows them with an offline notice; loading errors show their cause and a retry button instead of an empty list.
- Search titles, descriptions and ingredients; filter by categories from existing recipes. Filters stay visible while scrolling.
- Scale ingredient quantities by changing the number of servings. The parser understands decimals (“1,5 kg”), fractions (“1/2 TL”, “1 1/2 EL”, “½ Bund”), ranges (“2–3 Zehen”, the larger value counts) and common units. When Notion lists one ingredient per line, additions such as “1 Zwiebel, gewürfelt” stay together.
- Display instructions from Notion page content, with support for headings, lists, quotes, callouts and text formatting. Long pages with more than 100 blocks are loaded completely.
- Time, calories, difficulty and cost appear only when the matching Notion properties are filled in (see the table below). Recipes without a cover show a neutral placeholder.
- Save recipes with the bookmark in the recipe view. Saved recipes are stored on the device (not in Notion); in the recipe overview the “Gespeichert” (saved) chip next to the categories shows only them. The cards and the search bar no longer have a bookmark.
- Share recipes: the share icon on the cover image opens the Android share sheet with a recipe card (an image 1080 pixels wide showing title, time, servings, category and ingredients) and a text with ingredients and instructions. Quantities match the servings currently selected. Chat apps such as WhatsApp and Telegram usually show the text as a caption; how an app presents image and text together is up to the receiving app. Apps that only accept text (for example notes) receive the plain text. Details without a Notion property (such as the time) are left out. The Notion link is not shared because it is private. The image file exists only in the app cache and is deleted the next time you share.
- Write ratings of one to five stars back to Notion. If saving fails, the previous rating is restored and the reason is shown.
- Create recipes directly in Notion using the plus button: name, description, servings, category, ingredients, preparation steps and an optional HTTPS cover. The form validates inputs and the database schema; errors preserve the draft.
- Sample recipes are available without a configured Notion connection. This is not a complete offline synchronization of the recipe database.

### Weekly planner and widget

Schedule recipes for a date and meal slot: breakfast, lunch, dinner or snack. The planner is stored locally and supports browsing weeks and marking meals as cooked. A new planner starts empty; sample entries from earlier versions are removed on first start.

For connected Notion recipes, the planned date is written back to the `Geplant am` date property when it exists. Because Notion holds one date per recipe, it shows the next planned date from today (otherwise the most recent past one). Removing a recipe's last plan clears the property. If Notion cannot be reached, the local plan stays saved and the app reports that the Notion date was not updated. The planner does not show a daily nutrition summary at the bottom.

The home screen widget shows Monday through Sunday with the calendar week, dates and planned meals. It highlights today and labels empty days as having no planned meals. Tapping it opens the planner. The widget supports a minimum size of 4×2, resizing and a background transparency setting saved separately for each widget. Its configuration displays the actual layout with current plans. Changes to the plan, date and time zone update the display.

### Shopping list widget

A second home screen widget displays the current shopping list in the dark Yummify design with terracotta accents. Its header counts open and total items; the vertically scrolling list shows names, quantities and categories. Ingredients for today’s planned, uncooked recipes appear first with a today badge. Completed items move to the bottom and are crossed out. Tapping the circle checks or reopens an item; tapping the row or header opens the shopping list in the app. Changes in the app, shopping list and meal plan update the widget. It supports 4×2, 4×3 and larger sizes, with background transparency saved per widget. Configuration previews up to seven actual items.

### Inventory

- Manage food with quantity, unit, category, storage location, minimum stock, best-before date, barcode, notes and image.
- Use compact search, a storage location selector and filters for low stock, approaching dates and expired items.
- Browse alphabetically sorted, collapsible category sections with item counts and contrasting headings that stay visible while scrolling. Uncategorized items have their own section; items within each group are sorted by best-before date and name.
- Edit items in a full-screen form with a quantity stepper, adjustable increments, selection options from the Notion schema and a date picker. Default increments are 1 for pieces and packages, 50 for g/ml and 0.1 for kg/l.
- Swipe left or right and confirm to delete an item. The delete action is also available to screen readers.
- Plus buttons in the inventory and recipe list account for the actual bottom navigation height, including larger font sizes. While scrolling, the header (logo, search, status) and the bottom navigation follow your finger: they slide away when you scroll down and return as soon as you scroll back; when you let go they snap to shown or hidden. Category chips stay put; with a screen reader running, or when the list is short, everything stays visible.

**Item details:** An existing item opens read-only: the name is the heading at the very top, followed by image, the stock with plus/minus (quantity changes are saved immediately, even outside edit mode), the selected categories and storage location (no choice lists), best-before date, product information and notes. Barcode fields only appear while editing. Tapping the heading (or the pencil) switches to editing with all fields; new items start there directly.

### Barcodes and automatic product data

A long press on the plus button in the inventory opens a small menu with “Barcode scannen” (scan barcode): it creates a new item and opens the barcode scanner right away; a short tap opens the empty form as before. The Google Code Scanner reads barcodes using the camera. It requires Google Play services; the scanner module is downloaded before the first scan if necessary. Manual barcode entry is also available.

When adding an item, scanning automatically loads data from [Open Food Facts](https://world.openfoodfacts.org). After manual entry, use the product data lookup button. Requests use the public API with a German language preference and a Yummify User-Agent; Notion credentials are not sent to this service.

Empty fields are filled with the product name and brand, a matching existing category, notes and a product image. Scanned items default to **1 piece** (`Stück`), even if no product is found. Automatically generated notes contain only **package contents and brand**. Ingredients, nutrition and the product link are stored in separate fields. Previously saved automatic notes are shortened once the corresponding product fields are available; manual additions are preserved. Storage location and best-before date are entered manually. Check stock quantity and package unit before saving.

Nutrition, ingredients and the product URL are also mapped to existing Notion properties. Nutrition values are **per 100 g or 100 ml**, rather than per package or total stock. Barcodes already stored locally lead to the existing item; duplicate new items with the same barcode are prevented. Unknown products and network errors allow manual entry. Saving first stores the item locally; inventory synchronization then transfers it to Notion.

### Photos and product images

Existing Notion covers appear in item details. The image action explicitly offers the gallery or taking a new photo. User images are prepared in private app storage as JPEGs with corrected orientation and a maximum of 1600 pixels on the longer side. Synchronization uploads them through the Notion file upload API and sets them as page covers. Open Food Facts images are stored as external HTTPS covers. Pending images survive offline use and transfer errors; normal field edits do not replace an existing cover.

### Stock, cooking and shopping lists

Recipe ingredients show available inventory. Missing ingredients can be added to the shopping list based on selected servings and compatible units (g/kg, ml/l, pieces). If the same ingredient is already open on the list, the quantity is added up (for example 200 g + 0.3 kg tomatoes = 500 g) and the recipe is appended instead of creating a duplicate entry. The aisle (fruit and vegetables, dairy, meat and fish, spices and oils, pantry) is derived from the name; short words such as “Ei” (egg) only count as whole words, so “Reis” (rice) or “Rindfleisch” (beef) are sorted correctly. A new shopping list starts empty. Expired stock does not count as available. Names are compared without any model or network: case, umlauts, plural endings, word order, quantities, units, packaging (“Dose”, “Bund”) and preparation words (“frisch”, “gehackt”, text in parentheses) do not matter. So “Rote Paprika” and “Paprika rot”, “Passierte Tomaten (Dose)” and “Tomaten passiert”, as well as “Frühlingszwiebeln, gehackt” and “Lauchzwiebeln” (a small synonym list, also covering for example Möhre/Karotte, Sahne/Schlagsahne, Quark/Topfen) count as the same ingredient. The comparison is deliberately strict: “Paprika” does not match “Rote Paprika”, “Milch” does not match “Kokosmilch”, “Butter” does not match “Erdnussbutter”. A missed match only leaves one more line on the shopping list; a wrong match would make a missing ingredient disappear. The same rule applies to missing quantities, deduction when cooking, the shopping list and the “in stock” display.

In a recipe's ingredient list each ingredient shows how the inventory covers it: “Im Vorrat” (in stock), “Fehlt 200 g” (partly covered), “Andere Einheit” (other unit) with “Vorrat: 3 Stück” under the name (the same article exists in a unit that cannot be compared with the recipe's, such as pieces instead of grams; it is not counted as available), “Ähnlich” (similar) with the article name under the ingredient (no safe match, only a hint) or “Kaufen” (buy). Spelling variants of units (Dose/Dosen, Packung/Pck./Päckchen, Stk./Stück, Zehe/Zehen) count as equal. An ingredient without an amount such as “Salz” only counts as in stock if the article is in the inventory.

**Single ingredient to the shopping list:** In a recipe's ingredient list each ingredient has a shopping cart on the right. One tap puts exactly that ingredient on the list: the missing amount (for “Andere Einheit”, “Ähnlich” and “Kaufen” the full amount for the selected servings, and the recipe amount if the stock is sufficient). If the ingredient is already on the list, the button shows a check mark.

**Swiping in the shopping list:** Swiping right shows “Ins Inventar” (to inventory) and “Löschen” (delete), plus a button to cancel. “Ins Inventar” books the item with its amount into the inventory and removes it from the list; if the amount is not numeric it stays and the app says so. Swiping left sends the item to Home Assistant (Bring) and removes it from the list, but only once Home Assistant has accepted it; on an error it slides back and the message names the reason. Swiping left is only available when Home Assistant is set up.

**Shopping list from the meal plan:** “Aus Wochenplan hinzufügen” (next 7 days, rest of this week or next week) collects the ingredients of all meals in the range that are not cooked yet, adds up identical ingredients (also g and kg), subtracts unexpired inventory, and subtracts what is already on the list (open or checked). Running it twice therefore adds nothing twice. Partially covered ingredients go on the list with the missing quantity and the note “Vorrat deckt …”. Ingredients that the inventory fully covers appear in the **“Bereits im Vorrat”** section; “Doch kaufen” moves one back to the list with a tap, “Ausblenden” clears the section. Ingredients without a quantity (“Salz”) count as covered if the inventory holds any amount. Quantities apply to the servings stated in the recipe, because the meal plan stores no serving count. Meals whose recipe no longer exists in Notion are named in the message.

**“Vielleicht schon da” (maybe already there):** If the shopping list has an open entry that the matching does not recognise as stock, but the inventory holds something similar (unexpired, quantity above 0), such as “Hähnchenbrustfilet” on the list and “Hähnchenbrust” in the inventory, the app asks. “Ist vorrätig” takes the entry off the list and puts it under “Bereits im Vorrat”; “Doch kaufen” undoes it. “Nein, kaufen” hides the hint for exactly that name pair permanently, whatever the quantity. The app never changes the list without your answer. No embedding model is used: measuring 134 ingredient pairs (`ingredient_pairs.tsv`) showed that the tested models do not separate German ingredients cleanly (for example Öl ~ Olivenöl), while the rule matching finds 48 of 58 equal pairs and merges no different pair.

**Shopping list to Home Assistant (Bring):** Under Settings › Verbindungen (connections) enter the address of your Home Assistant (for example `http://homeassistant.local:8123` or the Nabu Casa address) and a long-lived access token (in Home Assistant under Profile › Security). “Verbinden & Listen laden” checks both and shows the to-do lists (`todo.*`), Bring integration lists first; after saving, the shopping list shows an “An Home Assistant senden” (send to Home Assistant) button. It sends the open, unchecked items through the `todo.add_item` service: the name becomes the Bring article and the amount goes into the description (shown as the note in Bring). The app remembers what it sent and only sends new or changed items next time; “Auch bereits gesendete erneut senden” forces everything. Items are only sent towards Home Assistant; checking an item off in Bring does not come back. Plain HTTP is only allowed for home-network addresses (`.local`, private IP ranges, hostnames without a dot, Tailscale); everything else needs HTTPS. Like the Notion token, the token stays on the device, is excluded from Android backup, and is only part of an export with explicit consent. Requires Home Assistant 2023.11 or newer with the Bring integration set up. Not tested against a real Home Assistant, only against a simulated server.

The cooking action deducts the required quantities after confirmation, using batches with the earliest best-before date first. If an ingredient is missing, nothing is deducted. Low-stock items can be added to the shopping list without duplicate open entries.

Checked shopping items can be transferred with their quantities to the inventory and removed from the shopping list. A stored transfer ID prevents duplicate bookings after an interruption. Non-numeric quantities require manual entry; storage location and best-before date can be added afterwards.

## Settings

The menu opens seven sub menus:

- **Profil:** display name (stays on this device) and auto-sync for the inventory synchronization.
- **Darstellung** (Appearance): color scheme, dynamic colors, start page, recipe view and language (see below).
- **Verbindungen** (Connections): Notion token, recipe and inventory database IDs, connection test and synchronization (see [Set up Notion](#set-up-notion)).
- **Datenpflege** (Data care): extend the dropdown lists of the Notion databases (see below).
- **Daten und Backup** (Data and backup): export, import, storage and deletion of local data (see [Data and backup](#data-and-backup)).
- **Info** and **Rechtliches** (legal): the sub menus exist but have no content yet.

In the English app only the Appearance entry is translated; the other menu labels stay German.

**Darstellung (Appearance)**

- **Color scheme:** Light, Dark or System (follows the device mode). The choice takes effect without a restart. Anyone who used the old dark mode switch keeps their choice (on = Dark, off = Light; adopted once). Without an earlier setting the app is Light. The switch in the profile is gone.
- **Dynamic colors** (Android 12+): on = Material You colors matching the wallpaper, off = the fixed Yummify colors. On Android 11 and older the option is disabled ("Available from Android 12 only").
- **Start page:** Recipes (default), Meal plan, Shopping list or Inventory. It opens when the app is started from the launcher. Widgets and links that carry a target take precedence; rotating the device or returning from the background does not jump back to the start page.
- **Recipe view:** List (default) or Grid. The grid uses columns of at least 150 dp width below the "Recipe of the day" card. The choice applies to the recipe overview only, not to the inventory.
- **Language:** System, Deutsch or English, independent of the system language. From Android 13 also under "App languages" in the system settings. Translated are the appearance settings, the "Darstellung" menu entry and the grid cards; all other screens stay German.

All values survive a restart. Not yet checked on a device: Material You colors and the language switch on Android 12 and older.

**Datenpflege (Data care):** Here you extend the dropdown lists (select and multi-select, such as category, cuisine, storage location or unit) of the connected Notion databases. Type a new entry and add it; the app creates it in Notion immediately and it is available in the app afterwards. Existing entries are sent back with their IDs unchanged; nothing is renamed or deleted. Duplicates (ignoring case) are not created again. Entries may be at most 100 characters long and must not contain a comma. The integration needs permission to update content; if it is missing, the app shows Notion's message. Text and status columns cannot be extended this way, and the app does not convert columns. New inventory entries are available in the inventory's dropdowns right away.

## Set up Notion

1. Create a Notion integration with read, insert and update permissions, and share the required databases or parent pages with it.
2. Enter the integration token and recipe database ID under Settings › Verbindungen, save and test the connection.
3. Optionally enter an inventory database ID or use the inventory database creation action. The app creates or reuses `Yummify Inventar` under the recipe database's parent page. That page must be shared with the integration.
4. Test the inventory connection and synchronize. Inventory uses the same token as recipes.

As database ID the app accepts the plain ID (with or without dashes) and any Notion link, including the form `notion.so/workspace/Rezepte-<id>?v=…`.

All Notion requests use API version `2025-09-03`. Inventory integration and recipe creation require a database with exactly one data source; reading recipes includes all data sources. The existing schema is checked before writing. When Notion rate-limits requests (HTTP 429), the app waits for the time Notion specifies and retries up to three times.

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
| `Bewertung` | `select` or `number` | Star rating, e.g. `★` through `★★★★★` or 1 through 5; the type is read from the schema |
| `Geplant am` | `date` | Optional write-back of the planned date |
| `Zeit`, `Zubereitungszeit`, `Kochzeit` or `Dauer` | `number` or text, e.g. “30 Min.” | Optional: preparation time in minutes |
| `Kalorien` or `kcal`, `Protein`, `Kohlenhydrate`, `Fett` | `number` or text | Optional: nutrition per serving |
| `Schwierigkeit` or `Aufwand` | `select` or text | Optional: shown in the recipe view |
| `Kosten` or `Preis` | `select` or text | Optional: shown in the recipe view |
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

Inventory changes are saved locally first. Pending edits, deletions and images are retained until successfully transferred. The inventory stays usable during synchronization: saving no longer waits for Notion. If an item changes while it is being uploaded, it stays pending and is sent again in the next round. If the local inventory storage is damaged, the app still starts, keeps the raw data as a backup and loads the state from Notion at the next synchronization. Own photos that are no longer needed are removed after an item's photo is replaced or the item is deleted. With automatic synchronization enabled, the app synchronizes on opening and after changes; pending changes are retried about once a minute while the app process is running. Manual synchronization is also available. With automatic synchronization disabled, transfers run manually only.

Inventory is separated by database. Initially local inventory is adopted when configuring a database for the first time. Unsynchronized local changes take priority in conflicts; changes from other devices are loaded during the next synchronization. The meal plan, shopping list and favorites are stored locally.

## Data and backup

Settings → "Daten und Backup" (the menu label stays German in the English app) collects everything about local data. No storage permission is needed (only internet and network state are requested); files are picked with the Android file picker.

- **Export data:** Creates a ZIP file with the recipe cache, favorites, meal plan, shopping list (including "already in stock" and dismissed suggestions), inventory, settings, the local product images and a manifest (format version, app version, timestamp, whether a token is included). The success message shows how many items were saved. If the file cannot be written completely, the app reports an error and deletes the partial file. If the partial file cannot be deleted, the app warns that it must not be used.
- **Notion token:** The token is not part of an export by default. "Include Notion token in export" is always off when the screen opens and needs an explicit confirmation. The file is not encrypted; anyone who has it can read the token.
- **Import data:** The file is fully checked first (valid ZIP, manifest, readable JSON files, known format version). Nothing changes until you have seen the summary and confirmed. A damaged file, a foreign format or a newer format version aborts the import with a message and your data stays untouched. If applying fails halfway, the previous state is restored. Exports from older versions without newer fields are completed on load. If the file contains inventory from another Notion database, links to Notion pages are removed.
- **Merge** (default): Items are combined by ID, nothing is lost. Single items have no change time, so conflicts are decided per area: if the export is newer than the last local change of that area, the file wins, otherwise (also on a tie) the local state wins. Unsynced local inventory changes always beat an unchanged file entry. Inventory entries taken from the file are marked as pending and uploaded to Notion by the normal sync. Deletions are not transferred.
- **Replace existing data:** After a safety prompt, replaces the local data with the file content. Unsynchronized local inventory changes are lost in the process. Notion is not changed.
- **Token on import:** If the file contains a token, the app asks before applying it. Without consent the current token stays. Database IDs are only taken over if none is set locally.
- **Automatic backup:** Switch, on by default. When on, Android backs up the app data according to the backup rules, always without the Notion token. When off, Android backs up no app data (cloud backup and device transfer). Existing backups remain and can be deleted in the Android settings. The switch applies per device and is not part of the export.
- **Storage:** Shows separately how much space data (JSON/settings), local product images and cache use. "Bildcache leeren" (clear image cache) only clears the cache; recipes, inventory and product images stay.
- **Delete all local data:** Removes the recipe cache, favorites, meal plan, shopping list, inventory including local product images, and the cache from this device. After a warning (including a note about unsynced inventory changes) and typing "LÖSCHEN". Nothing is written to or deleted in Notion; token, database IDs and settings stay, widgets are updated.

Not yet checked on a device: the Android file picker and the behavior of the backup agent in the system (cloud backup and device transfer).

## Privacy and security

- The Notion token is stored only on the device and is excluded from Android cloud backups and device transfers. After switching devices, enter it again.
- Network logging exists only in debug and preview builds; it contains headers only, with the `Authorization` header redacted. Release builds do not log requests.
- Open Food Facts receives only the barcode, never Notion credentials.

## Develop, build and install

Version name: `versionName` comes from the latest Git tag `vX.Y.Z`. As long as no tag exists, the fallback **1.3.0** from `app/build.gradle.kts` applies. Minimum Android **8.0 (API 26)**; compile/target SDK **36** (Android 16).

You need JDK 17 or newer, Android SDK 36 and the Android build tools. Android Studio can be used for development. Configure the SDK path through `local.properties` (`sdk.dir=…`) or `ANDROID_HOME`.

```bash
# Build the debug APK
./gradlew assembleDebug

# Install and launch on a device connected through USB
adb -d install -r app/build/outputs/apk/debug/app-debug.apk
adb -d shell am start -n de.yummify.app/.MainActivity

# Run tests and Android Lint
./gradlew testDebugUnitTest lintDebug

# Build the “Yummify Preview” test build
./gradlew assemblePreview
adb -d install -r app/build/outputs/apk/preview/app-preview.apk
```

**Preview build:** `assemblePreview` creates `app-preview.apk` with the package ID `de.yummify.app.preview` and the name “Yummify Preview”. It installs next to the regular Yummify app, does not replace it and shares no data with it, so new versions can be tried safely; enter the Notion token and database IDs there once. The APK is signed with the debug key and intended for testing only.

Tests cover quantity calculations, expiration, atomic recipe stock deduction, date handling, pagination, Notion property mapping including `kcal`, recipe creation and reading, the ingredient parser, Notion links, rate limiting, shopping categories and merging, error handling, interrupted transfer recovery, damaged storage, offline storage, synchronization without blocking edits, duplicate shopping transfers and widget rendering. They also cover export and import (checks, merge, replace, token handling), the appearance settings, the data care options and the backup rules.

A release build (`./gradlew assembleRelease`) is signed only when the key and its passwords are configured locally. The passwords are no longer in the repository; put them in `local.properties` (not tracked) or environment variables (`YUMMIFY_KEYSTORE`, `YUMMIFY_STORE_PW`, `YUMMIFY_KEY_ALIAS`, `YUMMIFY_KEY_PW`):

```properties
yummify.storeFile=release.jks
yummify.storePassword=…
yummify.keyAlias=yummify
yummify.keyPassword=…
```

`yummify.storeFile` is relative to the `app/` folder and defaults to `release.jks`. Releases are made with `scripts/release.sh <VERSION>`: it checks branch, milestone and tests, sets the tag `vX.Y.Z`, builds the signed APK and stores it with a checksum in `releases/`. `versionName` comes from the latest tag, `versionCode` is the number of commits. The backlog lives in GitHub issues; setup and workflow of the agent team are described in [docs/agent-team.md](docs/agent-team.md) (German). Without these values the release APK is unsigned. APKs, Gradle caches, SDK paths and signing keys are not tracked in Git.

### Automated checks (GitHub Actions)

Every push to `main` or a `claude/…` branch and every pull request runs unit tests, Android Lint and the preview build (`.github/workflows/android.yml`). The resulting `app-preview.apk` is available for 14 days under **Actions → Android → Artifacts → `yummify-preview-apk`** (unzip, then open the APK on the device).

### Technology and source code

Kotlin 2.2 with the Compose compiler plugin, Android Gradle Plugin 8.13, Gradle 8.14, Jetpack Compose (BOM 2025.12) with Material 3, Coroutines and StateFlow, OkHttp 4.12, Gson, Coil, SharedPreferences, Google Code Scanner and Android RemoteViews. Tests use JUnit, MockWebServer and Robolectric.

- [App entry point and navigation](app/src/main/java/de/yummify/app/MainActivity.kt)
- [Models, local storage, APIs and repositories](app/src/main/java/de/yummify/app/data)
- [Screens and styling](app/src/main/java/de/yummify/app/ui)
- [Widget](app/src/main/java/de/yummify/app/widget)
- [Tests](app/src/test/java/de/yummify/app)

## Data source and licenses

Open Food Facts provides product data under [ODbL](https://opendatacommons.org/licenses/odbl/1-0/), individual database contents under the [Database Contents License](https://opendatacommons.org/licenses/dbcl/1-0/) and product images under [CC BY-SA](https://creativecommons.org/licenses/by-sa/3.0/). Product sources and license notes are visible in the item detail product information; the product link is stored in the separate Notion `URL` property. These data licenses apply to the integrated product data and images; the repository currently contains no separate license file for the app source code.
