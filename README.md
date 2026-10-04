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


## Inventar (Version 1.1.0)

Der neue Inventar-Tab verwaltet Lebensmittel mit Menge, Einheit, Kategorie, Lagerort,
Mindestbestand, Ablaufdatum, Barcode und Notizen. Suche und Filter zeigen niedrige
Bestände, bald fällige und abgelaufene Artikel. Artikel können ergänzt, bearbeitet,
verbraucht und gelöscht werden.

### Notion einrichten

Unter Einstellungen denselben Integration-Token wie für Rezepte verwenden. Bei
leerem Inventar-Datenbank-Feld „Inventar-Datenbank in Notion anlegen“ wählen.
Die App erstellt oder verwendet „Yummify Inventar“ unter der übergeordneten Seite
der Rezept-Datenbank und speichert deren ID. Die Integration benötigt Lese-,
Einfüge- und Änderungsrechte auf dieser Seite. Alternativ die ID einer bestehenden
Datenbank mit genau einer Datenquelle eintragen, speichern und
„Inventar-Verbindung prüfen“ wählen. Die Felder der bestehenden „Inventar“-Tabelle
werden in beide Richtungen wie folgt zugeordnet:

| Notion-Feld | Notion-Typ | Feld in Yummify |
|---|---|---|
| Artikel | title | Artikelname |
| Bestand | number | Menge |
| nächstes MHD | date | Ablaufdatum |
| Kategorie | multi_select | Kategorie (mehrere Werte bleiben beim Abgleich erhalten) |
| Einheit | select | Einheit, z. B. Dose(n), Packung |
| Lagerort | select | Lagerort, z. B. Keller |

Leere Mengen werden als 0 gelesen; leere Datums- und Auswahlfelder bleiben leer.
Die Checkboxen links im Screenshot sind Notions Zeilenauswahl und kein
zusätzliches Inventar-Feld. Beim Bearbeiten einer Kategorie kann ein neuer Wert
angegeben werden; er ersetzt die bisherige Auswahl. Packung und Dose(n) bleiben
eigenständige Einheiten und werden nicht in Gramm oder Stück umgerechnet.

Die bisherige Zuordnung `Name`, `Menge`, `Ablaufdatum` und Textfelder für Einheit,
Kategorie und Lagerort bleibt unterstützt. Auswahlfelder können auch als
`select` / `multi_select` vorliegen. Neu angelegte Datenbanken verwenden das oben
gezeigte Schema. Fehlende Zusatzfelder `Mindestbestand` (number), `Barcode`,
`Notizen` und `Artikel-ID` (je rich_text) ergänzt die App beim Verbindungscheck oder
Synchronisieren automatisch, ohne bestehende Spalten umzubenennen oder deren
Typen zu ändern. Die Integration benötigt dafür Änderungsrechte am Schema.

Die Inventar-Anbindung verwendet die versionierte Notion-API 2025-09-03 mit
Datenquellen und vollständiger Pagination. Das Schema wird vor dem Schreiben
geprüft. Artikel-ID bitte unverändert lassen: Sie verhindert doppelte Artikel
nach einem unterbrochenen Request. Vorhandene Notion-Artikel ohne Artikel-ID
verwenden zunächst ihre Notion-Seiten-ID als stabile Kennung.

### Offline und Synchronisation

Alle Änderungen werden zuerst lokal gespeichert. Nicht übertragene Änderungen
und Löschungen bleiben bis zum erfolgreichen Abgleich erhalten. Bei aktivem
Auto-Sync erfolgt der Abgleich beim Öffnen, nach Änderungen und bei wartenden
Änderungen etwa jede Minute, solange der App-Prozess läuft. Zusätzlich ist ein
manueller Abgleich im Inventar möglich. Bei deaktiviertem Auto-Sync werden nur
manuelle Abgleiche ausgeführt. Das Inventar ist je Datenbank getrennt; ein zunächst
lokal erfasstes Inventar wird bei der ersten Einrichtung übernommen.

Bei gleichzeitigen Änderungen desselben Artikels haben noch nicht synchronisierte
lokale Änderungen Vorrang. Auf anderen Geräten vorgenommene Änderungen werden
beim nächsten Abgleich geladen. Der Barcode kann manuell erfasst oder direkt mit der Kamera gescannt werden.

### Rezepte und Einkaufsliste

Rezeptzutaten zeigen den aktuellen Vorrat. „Fehlende Zutaten zur Einkaufsliste“
berücksichtigt gewählte Portionen und kompatible Einheiten (g/kg, ml/l, Stück).
Abgelaufene Vorräte zählen nicht als verfügbar. Der Zutatenabgleich verwendet
identische Namen ohne Unterschiede in Groß-/Kleinschreibung und Leerzeichen;
unterschiedliche Bezeichnungen werden nicht automatisch gleichgesetzt.
„Gekocht · Zutaten vom Vorrat abbuchen“ benötigt eine Bestätigung und bucht zuerst
Chargen mit dem frühesten Ablaufdatum ab. Fehlt eine Zutat, wird nichts abgebucht.

Abgehakte Einkaufsartikel können mit ihren Mengen ins Inventar übertragen werden.
Eine dauerhaft gespeicherte Übernahme-ID verhindert Doppelbuchungen nach einem
Abbruch. Nicht numerische Einkaufspositionen müssen manuell erfasst werden. Den
Lagerort und das Ablaufdatum anschließend im Inventar ergänzen. Niedrige Bestände
lassen sich ohne doppelte offene Einträge auf die Einkaufsliste setzen.

### Widget und Rezepteübersicht

Das Widget zeigt die aktuelle Woche von Montag bis Sonntag als ruhige vertikale
Liste: yummify-Wortmarke, Kalenderwoche, Tageskürzel, Datum und geplante Gerichte.
Heute erhält eine dezente Terrakotta-Markierung. Ohne Planung steht „Noch nichts
geplant“ in der betreffenden Zeile. Mehrere Gerichte eines Tages werden in der
Reihenfolge der Mahlzeiten angezeigt. Es gibt keine Rezeptbilder, Zähler oder
Aktionsschaltflächen; Antippen öffnet den Wochenplan. Die kompakte Größe ist 4×2,
bei größeren Widgets erhalten lange Titel mehr Platz. Die Implementierung nutzt
Android RemoteViews, und die Konfiguration zeigt dasselbe tatsächliche Layout
mit den aktuellen Planungen statt einer fiktiven Vorschau.

Geplante Gerichte haben vollständige Datumsangaben. Alte Datensätze ohne Monat
und Jahr werden nur innerhalb der aktuellen Woche zugeordnet. Änderungen am
Wochenplan, Datum und Zeitzone aktualisieren das Widget. Hintergrundtransparenz
wird je Widget gespeichert, ohne die Lesbarkeit der Texte zu verändern.

In der Rezepteübersicht scrollen Titel und Suche aus dem Bild. Filter-Chips bleiben
oben stehen. Die Navigation wird beim Scrollen ausgeblendet und erscheint am
Listenanfang wieder.

### Inventarübersicht und Navigation

Die Inventarübersicht beginnt direkt mit einer kompakten Suche ohne Kopfleiste.
Lagerorte stehen in einem Auswahlmenü, Bestandsfilter in einer horizontalen
Chip-Leiste. Material-3-Karten heben Mengen und MHD hervor. Synchronisationsdetails
und Einrichtung sind über den kompakten Status erreichbar. Die untere Navigation
verwendet kurze, einzeilige Beschriftungen für gleichmäßig ausgerichtete Icons.
Im Inventar bleibt sie beim Scrollen sichtbar. „Rezepte“ führt zurück zum Anfang
der Rezepteübersicht, auch wenn zuvor eine gespeicherte Liste gescrollt war.

Die gefilterten Inventarartikel stehen in alphabetisch sortierten, ein- und
ausklappbaren Kategorieabschnitten mit Artikelanzahl. Artikel ohne Kategorie
erscheinen unter „Ohne Kategorie“. Innerhalb eines Abschnitts bleibt die Sortierung
nach MHD und Name erhalten. Zum Löschen einen Artikel nach links oder rechts
wischen und den Löschdialog bestätigen; Abbrechen erhält den Artikel. Für
Screenreader steht dieselbe Aktion als „Artikel löschen“ zur Verfügung.
Die Nährwert-Tagesübersicht am Ende des Planers entfällt.

### Artikeldetails, Kamera und Bilder

Die Artikeldetails öffnen im Vollbild. Der Artikelname erscheint als Überschrift
und wird erst nach Antippen zum Eingabefeld. Die Menge wird über einen Plus-/Minus-
Stepper geändert. Die Schrittweite ist auswählbar; Standardwerte sind 1 für Stück
und Packungen, 50 für g/ml und 0,1 für kg/l. Dezimalwerte werden präzise addiert,
und die Menge kann nicht negativ werden. Kategorien werden als Chips
(Mehrfachauswahl) bzw. Dropdown, Lagerort und Einheit als Dropdown angeboten.
Die Auswahl stammt aus dem Notion-Schema und bleibt für Offline-Nutzung gespeichert;
bisherige Datenbanken mit Textfeldern behalten freie Eingabe. Das MHD wird mit dem
Material-Datepicker ausgewählt und lässt sich entfernen.

„Mit Kamera scannen“ öffnet den Google-Code-Scanner. Er benötigt Google
Play-Dienste; das Scanner-Modul wird bei Bedarf vor dem ersten Scan heruntergeladen.
Manuelle Barcode-Eingabe bleibt verfügbar. Scannen verändert den Artikel erst
beim Speichern.

Das Notion-Headerbild wird angezeigt. Beim Öffnen eines Artikels startet weder
Kamera noch Bildauswahl. Die Bildaktion öffnet zunächst eine explizite Auswahl
zwischen Galerie und neuer Fotoaufnahme. Über diese Auswahl kann ein neues Foto
aufgenommen oder ein Bild ausgewählt werden. Bilder werden in privatem App-Speicher
als JPEG mit korrigierter Ausrichtung und maximal 1600 Pixeln auf der langen Seite
vorbereitet. Beim nächsten Inventarabgleich lädt die App das Bild über die Notion
File-Upload-API hoch und setzt es als Seiten-Cover. Offline oder nach einem Fehler
bleiben Bild und ausstehende Änderung erhalten. Normale Feldänderungen überschreiben
ein bestehendes Notion-Cover nicht. Ohne Auto-Sync ist ein manueller Abgleich nötig.

### Automatische Produktdaten per Barcode

Beim Anlegen eines Artikels lädt die App nach dem Kamerascan automatisch
Produktdaten von [Open Food Facts](https://world.openfoodfacts.org). Alternativ
Barcode eintippen und „Produktdaten laden“ wählen. Verwendet wird die öffentliche
API v2 mit deutschem Sprachwunsch und einem Yummify-User-Agent mit Repository-Kontakt.
Die Verbindung enthält keine Notion-Zugangsdaten.

Leere Felder werden mit deutschem Produktnamen/Marke,
einer passenden vorhandenen Kategorie sowie dem Produktbild ergänzt. Gescannte
Produkte werden standardmäßig als 1 Stück erfasst, auch wenn kein Produkt gefunden
wurde; der Bestand zählt die Gebinde. Packungsgewicht oder -volumen erscheint
als „Packungsinhalt“ in den Notizen. Zutaten,
verfügbare Nährwerte und Quellenangabe erscheinen in den Notizen. Eigene Eingaben
bleiben erhalten. Mehrfachpackungen ohne eindeutige Gesamtmenge werden nicht
erraten. Bestand und Gebinde vor dem Speichern prüfen und gegebenenfalls anpassen. Lagerort und MHD bleiben manuell.

Bereits lokal gespeicherte Barcodes führen zum vorhandenen Artikel, dessen Bestand
angepasst werden kann. Die Speicherung verhindert doppelte neue Artikel mit
demselben Barcode. Unbekannte Produkte, Zeitüberschreitungen und Netzwerkfehler
lassen die manuelle Erfassung zu. Die Suche verändert weder Open Food Facts noch
Notion; erst Speichern legt die lokale Änderung an, der übliche Inventarabgleich
überträgt sie nach Notion. Produktbilder werden als externes HTTPS-Cover gespeichert.
Eine bestehende Notion-Spalte „Status“ (select, status oder rich_text) erhält bei
positivem Bestand „Vorhanden“. Bei 0 wird „Aufgebraucht“ gesetzt, sofern eine
Status-Spalte diese Option unterstützt. Ohne diese Spalte bleibt der numerische
Bestand maßgeblich; es wird keine neue Status-Spalte angelegt.

Die vorhandenen Notion-Spalten werden zusätzlich direkt befüllt und wieder eingelesen:

| Notion-Feld | Typ | Open-Food-Facts-Wert |
|---|---|---|
| Kalorien | number | energy-kcal_100g (kcal je 100 g/ml) |
| Fett | number | fat_100g (g je 100 g/ml) |
| Kohlenhydrate | number | carbohydrates_100g (g je 100 g/ml) |
| Protein | number | proteins_100g (g je 100 g/ml) |
| Zutaten | rich_text | ingredients_text_de, alternativ ingredients_text |
| URL | url (auch rich_text unterstützt) | Produktlink bei Open Food Facts |

Die Spalten werden nur verwendet, wenn sie bereits existieren, und nicht neu angelegt
oder umgewandelt. Fehlende Nährwerte bleiben unbekannt; echte Nullwerte werden als 0
übertragen. Bei lokalen Artikeln ohne Produktdaten bleiben bestehende Notion-Angaben
unverändert. Die Angaben bleiben offline und nach Bestandsänderungen gespeichert.
Lange Zutatenlisten werden ohne Abschneiden in Notion-Textabschnitte aufgeteilt.

Datenquelle: Open Food Facts, Daten unter
[ODbL](https://opendatacommons.org/licenses/odbl/1-0/), einzelne Inhalte unter
[Database Contents License](https://opendatacommons.org/licenses/dbcl/1-0/),
Produktbilder unter [CC BY-SA](https://creativecommons.org/licenses/by-sa/3.0/).
Die Produktquelle bleibt über die Notizen auch in Notion nachvollziehbar.

### Neue Rezepte in der App

Der Plus-Button der Rezeptübersicht öffnet ein Formular für Rezeptname,
Beschreibung, Portionen, Kategorie, Zutaten (eine pro Zeile), Zubereitung
(ein Schritt pro Zeile) und eine optionale HTTPS-Bildadresse. Speichern legt das
Rezept mit allen Schritten in einem Request in der verbundenen Notion-Datenbank an.
Die Integration benötigt Einfügerechte. Das vorhandene Schema wird vorher geprüft:
Titelfeld, „Portionen“ (number), „Zutaten“ bzw. „Lebensmittel“ (rich_text) und,
sofern ausgefüllt, „Beschreibung“ (rich_text) und „Kategorie“
(select/multi_select/rich_text). Bei Fehlern bleiben die Eingaben im Formular;
bei Erfolg erscheint das Rezept direkt in der Übersicht. Ohne Notion-Verbindung
muss diese zuerst in den Einstellungen eingerichtet werden.

Die Kategorieüberschriften des Inventars sind kontrastreich hervorgehoben und
bleiben beim Scrollen oben sichtbar. Die Plus-Buttons in Inventar und Rezepten
stehen oberhalb der tatsächlich gemessenen Footer-Höhe, auch bei größerer Schrift.

### Prüfungen

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug
```

Regressionstests prüfen Mengen, Haltbarkeit, atomare Rezept-Abbuchung, vollständige
Datumszuordnung, Pagination, Notion-Payloads, Fehlerbehandlung, Wiederaufnahme
unterbrochener Anlage/Löschung, Offline-Speicherung, Einkaufs-Doppelbuchungen und
RemoteViews-Rendering auf einer simulierten Android-34-Umgebung.

### Repository und lokale Dateien

Quellcode, Ressourcen, Tests und diese README bilden den aktuellen App-Stand.
Gradle-Caches, Build-Ausgaben/APKs, lokale SDK-Pfade und Signierschlüssel werden
nicht versioniert. Für einen Release-Build wird lokal `app/release.jks` mit dem
in `app/build.gradle.kts` konfigurierten Alias benötigt. GitHub-Zugangsdaten
gehören ausschließlich in die lokale Git-Credential-Konfiguration, niemals in
App-Code, README oder Remote-URLs.
