# Yummify – Hinweise für die Entwicklung

Android-App (Kotlin, Jetpack Compose, Material 3) für Rezepte, Wochenplan, Einkaufsliste und Inventar mit Notion als Backend. Package `de.yummify.app`, einziges Modul `:app`.

## Bauen und Testen

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

Voraussetzungen: JDK 17+, Android SDK 34 (`local.properties` mit `sdk.dir=…` oder `ANDROID_HOME`). Release-Builds brauchen `app/release.jks` (nicht versioniert).

## Struktur

- `MainActivity.kt` – NavHost (`recipes`, `planner`, `shopping`, `inventory`, `settings`, `recipe_detail/{recipeId}`) und Bottom-Bar. Intent-Extras `route` und `recipeId` für Widgets und Deep Links.
- `data/model` – Datenklassen. Fachlogik für das Inventar steckt in `InventoryMath` (Einheiten umrechnen, Abbuchen, Fehlmengen).
- `data/repository` – Singletons über `getInstance(context)`, Persistenz als Gson-JSON in SharedPreferences. Ausnahme: `RecipeRepository` wird pro Aufruf mit Token und DB-ID erzeugt und hat keinen Cache.
- `data/remote` – `NotionInventoryApi` und `NotionRecipeApi` (Notion-Version `2025-09-03`, `data_sources`), `OpenFoodFactsApi`. `RecipeRepository` liest Rezepte noch mit `2022-06-28`.
- `ui/screens/<feature>` – je ein Screen und ein `AndroidViewModel` mit `StateFlow`-UI-State.
- `widget/` – Wochenplan- und Einkaufslisten-Widget (RemoteViews). Repositories rufen nach Änderungen `updateAllWidgets` auf.
- `design-system/` – HTML- und PNG-Entwürfe sowie `DESIGN.md` („Warm Culinary Nocturne“).

## Konventionen

- Oberfläche, Fehlermeldungen und Kommentare zu Fachlogik sind auf Deutsch. Texte stehen derzeit direkt im Code.
- Notion-Feldnamen (`Artikel`, `Bestand`, `nächstes MHD`, `Portionen`, `Zutaten`, `Geplant am`, …) sind Teil des Vertrags mit den Datenbanken des Nutzers und stehen im README. Vorhandene Spalten nie umbenennen oder umwandeln. Vor dem Schreiben das Schema prüfen.
- Inventar ist offline-first: Änderungen gehen über `InventoryRepository.mutate`, werden `dirty` markiert und per `sync()` übertragen. Lokale Änderungen haben bei Konflikten Vorrang.
- Neue Felder in gespeicherten Datenklassen: Gson ignoriert Kotlin-Defaults, ältere Daten liefern `null`. Beim Laden normalisieren.
- Nutzerfehler als `require(...) { "deutsche Meldung" }` werfen und im ViewModel als Nachricht anzeigen.
- Tests liegen unter `app/src/test` (JUnit, MockWebServer, Robolectric). Neue Fachlogik bekommt einen Unit-Test.
- README (`README.md` deutsch, `README.en.md` englisch) bei Funktionsänderungen beide aktualisieren.

## Offene Punkte

Siehe `docs/CODE_REVIEW.md` für alle Befunde und die empfohlene Reihenfolge.
