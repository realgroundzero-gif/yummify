# Yummify

**Deutsch** · [English](README.en.md)

Yummify ist eine Android-App für Rezepte, Wochenplanung, Einkaufsliste und Lebensmittelvorräte. Sie verbindet deine Notion-Rezept- und Inventardatenbanken mit einer Oberfläche aus Kotlin und Jetpack Compose. Die App-Oberfläche ist derzeit deutschsprachig.

## Funktionen

### Rezepte

- Rezepte aus Notion laden, einschließlich aller Ergebnisse über Datenbank-Pagination, Seiten-Cover, Kategorien, Küche und Tags.
- Nach Titel, Beschreibung und Zutaten suchen; Kategorien aus den vorhandenen Rezepten filtern. Filter bleiben beim Scrollen sichtbar.
- Zutatenmengen durch Ändern der Portionen skalieren. Der Parser unterstützt unter anderem Dezimalzahlen, Brüche und übliche Einheiten.
- Zubereitung aus Notion-Seiteninhalten anzeigen, mit Unterstützung für Überschriften, Listen, Zitate, Hinweise und Textformatierung.
- Favoriten verwalten und Bewertungen von einem bis fünf Sternen nach Notion zurückschreiben.
- Über den Plus-Button neue Rezepte direkt in Notion erstellen: Name, Beschreibung, Portionen, Kategorie, Zutaten, Zubereitungsschritte und optionales HTTPS-Cover. Das Formular prüft Eingaben und Datenbankschema; bei Fehlern bleibt der Entwurf erhalten.
- Ohne eingerichtete Notion-Verbindung stehen Beispielrezepte zur Verfügung. Das ist kein vollständiger Offline-Abgleich der Rezeptdatenbank.

### Wochenplan und Widget

Rezepte lassen sich für ein Datum und eine Mahlzeit planen: Frühstück, Mittagessen, Abendessen oder Snack. Der Wochenplan wird lokal gespeichert und unterstützt den Wechsel zwischen Wochen sowie das Markieren gekochter Gerichte. Bei verbundenen Notion-Rezepten wird das geplante Datum in das Datumsfeld `Geplant am` zurückgeschrieben, sofern es vorhanden und beschreibbar ist. Eine Nährwert-Tagesübersicht am Ende des Planers wird nicht angezeigt.

Das Startbildschirm-Widget zeigt Montag bis Sonntag mit Kalenderwoche, Datum und geplanten Gerichten. Der aktuelle Tag ist hervorgehoben; leere Tage zeigen „Noch nichts geplant“. Antippen öffnet den Wochenplan. Das Widget ist ab 4×2 nutzbar, lässt sich vergrößern und bietet eine je Widget gespeicherte Hintergrundtransparenz. Seine Konfiguration zeigt das tatsächliche Layout mit den aktuellen Planungen. Änderungen am Plan, Datum und Zeitzone aktualisieren die Anzeige.

### Inventar

- Lebensmittel mit Menge, Einheit, Kategorie, Lagerort, Mindestbestand, Mindesthaltbarkeitsdatum (MHD), Barcode, Notizen und Bild verwalten.
- Kompakte Suche, Lagerortauswahl und Filter für niedrige Bestände, bald fällige und abgelaufene Artikel.
- Alphabetisch sortierte, ein- und ausklappbare Kategorieabschnitte mit Artikelanzahl und kontrastreichen Überschriften, die beim Scrollen sichtbar bleiben. Artikel ohne Zuordnung erscheinen unter „Ohne Kategorie“; innerhalb der Gruppen wird nach MHD und Name sortiert.
- Artikel im Vollbild bearbeiten: Mengen-Stepper mit auswählbarer Schrittweite, Auswahlfelder aus dem Notion-Schema und Datumsauswahl. Die Standardschritte sind 1 für Stück und Packungen, 50 für g/ml und 0,1 für kg/l.
- Durch Wischen nach links oder rechts mit anschließender Bestätigung löschen. Für Screenreader ist die Löschaktion ebenfalls verfügbar.
- Plus-Buttons in Inventar und Rezeptübersicht berücksichtigen die tatsächliche Höhe der unteren Navigation, auch bei größerer Schrift. Im Inventar bleibt die Navigation beim Scrollen sichtbar.

### Barcode und automatische Produktdaten

Der Google-Code-Scanner erfasst Barcodes mit der Kamera. Dafür sind Google Play-Dienste erforderlich; das Scanner-Modul wird bei Bedarf vor dem ersten Scan heruntergeladen. Alternativ lässt sich der Barcode manuell eingeben.

Beim Anlegen eines Artikels lädt ein Scan automatisch Daten von [Open Food Facts](https://world.openfoodfacts.org). Nach manueller Eingabe kann „Produktdaten laden“ gewählt werden. Die öffentliche API wird mit deutschem Sprachwunsch und einem Yummify-User-Agent aufgerufen; Notion-Zugangsdaten werden dabei nicht übertragen.

Leere Felder werden mit Produktname und Marke, einer passenden vorhandenen Kategorie, Notizen und Produktbild ergänzt. Gescannte Artikel werden standardmäßig als **1 Stück** erfasst, auch ohne Datenbanktreffer. Das Packungsgewicht oder -volumen steht separat als „Packungsinhalt“ in den Notizen. Lagerort und MHD werden manuell ergänzt. Bestand und Gebinde vor dem Speichern prüfen.

Nährwerte, Zutaten und Produkt-URL werden zusätzlich in vorhandene Notion-Spalten übernommen. Die Nährwerte beziehen sich auf **100 g beziehungsweise 100 ml**, nicht auf die gesamte Packung oder den Bestand. Vorhandene lokale Barcodes führen zum bestehenden Artikel; doppelte neue Artikel mit demselben Barcode werden verhindert. Unbekannte Produkte und Netzwerkfehler lassen die manuelle Erfassung zu. Erst Speichern übernimmt den Artikel lokal; der Inventarabgleich überträgt ihn nach Notion.

### Fotos und Produktbilder

Vorhandene Notion-Cover werden in den Artikeldetails angezeigt. Die Bildaktion bietet ausdrücklich Galerie oder neue Fotoaufnahme an. Eigene Bilder werden im privaten App-Speicher als JPEG mit korrigierter Ausrichtung und maximal 1600 Pixeln auf der langen Seite vorbereitet. Beim Abgleich werden sie über die Notion-Datei-Upload-API als Seiten-Cover gespeichert. Open-Food-Facts-Bilder werden als externe HTTPS-Cover übernommen. Ausstehende Bilder bleiben bei Offline-Nutzung oder Übertragungsfehlern erhalten; normale Feldänderungen ersetzen kein bestehendes Cover.

### Vorrat, Kochen und Einkaufsliste

Rezeptzutaten zeigen den verfügbaren Vorrat. Fehlende Zutaten lassen sich unter Berücksichtigung der gewählten Portionen und kompatiblen Einheiten (g/kg, ml/l, Stück) zur Einkaufsliste hinzufügen. Abgelaufene Vorräte zählen nicht als verfügbar. Der Abgleich verwendet identische Namen ohne Unterschiede in Groß-/Kleinschreibung und Leerzeichen; unterschiedliche Produktbezeichnungen werden nicht automatisch gleichgesetzt.

„Gekocht · Zutaten vom Vorrat abbuchen“ zieht nach Bestätigung die benötigten Mengen ab und verwendet zuerst Chargen mit dem frühesten MHD. Fehlt eine Zutat, wird nichts abgebucht. Niedrige Bestände können ohne doppelte offene Einträge auf die Einkaufsliste gesetzt werden.

Abgehakte Einkaufsartikel lassen sich mit ihren Mengen ins Inventar übertragen und aus der Einkaufsliste entfernen. Eine gespeicherte Übernahme-ID verhindert Doppelbuchungen nach einem Abbruch. Nicht numerische Mengen müssen manuell erfasst werden; Lagerort und MHD lassen sich anschließend ergänzen.

## Notion einrichten

1. Eine Notion-Integration mit Lese-, Einfüge- und Änderungsrechten einrichten und die benötigten Datenbanken beziehungsweise übergeordneten Seiten für diese Integration freigeben.
2. Unter Einstellungen den Integration-Token und die Rezept-Datenbank-ID eintragen, speichern und die Verbindung prüfen.
3. Optional eine Inventar-Datenbank-ID eintragen oder „Inventar-Datenbank in Notion anlegen“ wählen. Die App erstellt oder verwendet „Yummify Inventar“ unter der übergeordneten Seite der Rezeptdatenbank. Diese Seite muss für die Integration freigegeben sein.
4. Die Inventar-Verbindung prüfen und synchronisieren. Das Inventar verwendet denselben Token wie die Rezepte.

Inventaranbindung und Rezeptanlage verwenden die Notion-API-Version `2025-09-03` und benötigen eine Datenbank mit genau einer Datenquelle. Das vorhandene Schema wird vor dem Schreiben geprüft.

### Rezeptdatenbank

Die folgenden Namen sind tatsächliche Notion-Feldnamen und bleiben auch in der englischen Dokumentation unverändert.

| Notion-Feld | Typ | Verwendung |
|---|---|---|
| Titelfeld, z. B. `Name`, `Titel` oder `Artikel` | `title` | Rezeptname; für neue Rezepte erforderlich |
| `Portionen` | `number` | Ausgangsportionen; für neue Rezepte erforderlich |
| `Zutaten` | `rich_text` | Zutaten, eine pro Zeile; für neue Rezepte alternativ `Lebensmittel` |
| `Beschreibung` | `rich_text` | Kurzbeschreibung; erforderlich, wenn beim Anlegen ausgefüllt |
| `Kategorie` | `select`, `multi_select` oder `rich_text` | Kategorie; erforderlich, wenn beim Anlegen ausgefüllt |
| `Küche` | `select` oder `multi_select` | Zusätzliche Filter beim Lesen |
| `Tags` | `multi_select` | Zusätzliche Filter beim Lesen |
| `Bewertung` | `select` oder `number` | Sternebewertung, z. B. `★` bis `★★★★★` oder 1 bis 5 |
| `Geplant am` | `date` | Optionales Zurückschreiben des geplanten Datums |
| Seiteninhalt | Notion-Blöcke | Zubereitung; neue Rezepte speichern jeden Schritt als nummerierten Listenblock |
| Seiten-Cover | Externes oder hochgeladenes Bild | Rezeptbild |

### Inventardatenbank

| Bevorzugtes Notion-Feld | Unterstützte Alternative | Typ |
|---|---|---|
| `Artikel` | `Name` | `title` |
| `Bestand` | `Menge` | `number` |
| `nächstes MHD` | `Ablaufdatum` | `date` |
| `Kategorie` | — | `multi_select`, `select` oder `rich_text` |
| `Einheit` | — | `select`, `multi_select` oder `rich_text` |
| `Lagerort` | — | `select`, `multi_select` oder `rich_text` |

Neue Inventardatenbanken verwenden `Artikel`, `Bestand`, `nächstes MHD`, Kategorien als Mehrfachauswahl sowie Einheit und Lagerort als Auswahl. Fehlende Zusatzfelder `Mindestbestand` (`number`), `Barcode`, `Notizen` und `Artikel-ID` (je `rich_text`) ergänzt die App automatisch beim Verbindungscheck oder Abgleich. Dafür sind Änderungsrechte am Schema erforderlich. Bestehende Spalten werden nicht umbenannt oder umgewandelt. Die `Artikel-ID` dient zur Wiederaufnahme unterbrochener Übertragungen und sollte unverändert bleiben.

Mehrere Kategorien bleiben beim Abgleich erhalten. Leere Mengen werden als 0 gelesen; leere Datums- und Auswahlfelder bleiben leer. Gebinde wie Packung und Dose(n) werden nicht in Gramm umgerechnet. Eine optionale Spalte `Status` (`select`, `status` oder `rich_text`) erhält „Vorhanden“ bei positivem Bestand beziehungsweise „Aufgebraucht“ bei 0, sofern die jeweilige Auswahl unterstützt wird.

### Produktdaten aus Open Food Facts

Diese Zusatzspalten werden **nur verwendet, wenn sie bereits existieren**; die App legt sie nicht automatisch an.

| Notion-Feld | Typ | Quelle / Bezugsmenge |
|---|---|---|
| `Kalorien` oder `kcal` | `number` | `energy-kcal_100g`, kcal je 100 g/ml |
| `Fett` | `number` | `fat_100g`, g je 100 g/ml |
| `Kohlenhydrate` | `number` | `carbohydrates_100g`, g je 100 g/ml |
| `Protein` | `number` | `proteins_100g`, g je 100 g/ml |
| `Zutaten` | `rich_text` | `ingredients_text_de`, alternativ `ingredients_text` |
| `URL` | `url` oder `rich_text` | Produktlink bei Open Food Facts |

Existieren `Kalorien` und `kcal` gleichzeitig, hat `Kalorien` Vorrang. Fehlende Nährwerte bleiben unbekannt; echte Nullwerte werden als 0 übertragen. Artikel ohne Produktdaten überschreiben vorhandene Notion-Produktangaben nicht. Produktdaten bleiben lokal über Neustarts und Bestandsänderungen erhalten; lange Zutatenlisten werden in Notion-Textabschnitte aufgeteilt.

## Offline-Nutzung und Synchronisation

Inventaränderungen werden zuerst lokal gespeichert. Ausstehende Änderungen, Löschungen und Bilder bleiben bis zur erfolgreichen Übertragung erhalten. Mit aktiviertem automatischem Abgleich wird beim Öffnen und nach Änderungen synchronisiert; wartende Änderungen werden etwa jede Minute erneut versucht, solange der App-Prozess läuft. Zusätzlich ist ein manueller Abgleich möglich. Ohne automatischen Abgleich erfolgt die Übertragung nur manuell.

Das Inventar ist nach Datenbank getrennt. Ein zunächst lokal erfasstes Inventar wird bei der ersten Einrichtung übernommen. Noch nicht synchronisierte lokale Änderungen haben bei Konflikten Vorrang; Änderungen anderer Geräte werden beim nächsten Abgleich geladen. Wochenplan und Einkaufsliste werden lokal gespeichert.

## Entwickeln, bauen und installieren

Aktueller App-Stand: **1.1.0**, Android **8.0+ (API 26)**; Compile-/Target-SDK **34**.

Benötigt werden JDK 17, Android SDK 34 und die Android-Build-Werkzeuge. Android Studio kann für die Entwicklung verwendet werden. Den SDK-Pfad über `local.properties` (`sdk.dir=…`) oder `ANDROID_HOME` konfigurieren.

```bash
# Debug-APK bauen
./gradlew assembleDebug

# Auf einem über USB angeschlossenen Gerät installieren und starten
adb -d install -r app/build/outputs/apk/debug/app-debug.apk
adb -d shell am start -n de.yummify.app/.MainActivity

# Tests und Android Lint ausführen
./gradlew testDebugUnitTest lintDebug
```

Die Tests decken unter anderem Mengenrechnung, Haltbarkeit, atomare Rezept-Abbuchung, Datumsermittlung, Pagination, Notion-Feldzuordnung einschließlich `kcal`, Rezeptanlage, Fehlerbehandlung, Wiederaufnahme unterbrochener Übertragungen, Offline-Speicherung, Einkaufs-Doppelbuchungen und Widget-Darstellung ab.

Ein Release-Build (`./gradlew assembleRelease`) benötigt den lokal konfigurierten Signierschlüssel `app/release.jks`. APKs, Gradle-Caches, SDK-Pfade und Signierschlüssel werden nicht versioniert.

### Technik und Quellcode

Kotlin 1.9.23, Jetpack Compose mit Material 3, Coroutines und StateFlow, OkHttp 4.12, Gson, Coil, SharedPreferences, Google-Code-Scanner und Android RemoteViews. Tests verwenden JUnit, MockWebServer und Robolectric.

- [App-Einstieg und Navigation](app/src/main/java/de/yummify/app/MainActivity.kt)
- [Datenmodelle, lokale Speicherung, APIs und Repositories](app/src/main/java/de/yummify/app/data)
- [Oberflächen und Gestaltung](app/src/main/java/de/yummify/app/ui)
- [Widget](app/src/main/java/de/yummify/app/widget)
- [Tests](app/src/test/java/de/yummify/app)

## Datenquelle und Lizenzen

Open Food Facts stellt Produktdaten unter [ODbL](https://opendatacommons.org/licenses/odbl/1-0/), einzelne Datenbankinhalte unter der [Database Contents License](https://opendatacommons.org/licenses/dbcl/1-0/) und Produktbilder unter [CC BY-SA](https://creativecommons.org/licenses/by-sa/3.0/) bereit. Die Produktquelle und Lizenzhinweise bleiben in den Artikelnotizen nachvollziehbar. Diese Datenlizenzen gelten für die eingebundenen Produktdaten und Bilder; im Repository ist derzeit keine separate Lizenzdatei für den App-Quellcode enthalten.
