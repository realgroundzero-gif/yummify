# Yummify – Code-Review und Übergabe

## Umsetzungsstand

Stand Version 1.2.0. Erledigte Befunde sind jeweils durch Unit-Tests abgesichert (87 Tests, alle grün; `lintDebug` ohne Fehler).

| Befund | Status |
|---|---|
| 1 Token im Logcat | ✅ Gemeinsamer `NotionHttp`-Client: Logging nur in Debug/Preview, nur Header, `Authorization` geschwärzt |
| 2 Signier-Passwort im Repo | ✅ Aus `local.properties` oder Umgebungsvariablen. **Offen für dich:** Das alte Passwort steht weiter in der Git-Historie; vor einer Play-Store-Veröffentlichung neuen Schlüssel erzeugen |
| 3 Token im Backup | ✅ `yummify_user_prefs.xml` von Cloud-Backup und Geräteübertragung ausgeschlossen |
| 4 Absturzschleife bei kaputtem Speicher | ✅ Rohdaten werden gesichert, App startet leer mit Hinweis (gilt auch für Wochenplan und Einkaufsliste) |
| 5 Gson und neue Felder | ✅ `normalized()` beim Laden von Inventar und Rezept-Cache |
| 6 Beispieldaten | ✅ Neue Nutzer starten leer; alte Demo-Einträge (`m1`–`m11`, `s1`–`s14`) werden einmalig entfernt |
| 7 „Ei“-Kategorie | ✅ Regeltabelle mit Ganzwort- und Wortende-Regeln |
| 8 DatePicker-Zeitzone | ✅ UTC wie im Inventar |
| 9 Notion-Link | ✅ Letzte 32 Hex-Zeichen, auch mit Titel im Link |
| 10 Ständiges Neuladen | ✅ `RecipeRepository` ist ein Singleton mit `StateFlow`, lädt nur bei geänderter Verbindung, Cache auf dem Gerät |
| 11 Fehler wie „keine Rezepte“ | ✅ Fehlerzeile mit „Erneut versuchen“, Detailansicht mit Fehler, Zurück und Wiederholen; Teil-Listen werden nicht mehr still zurückgegeben |
| 12 Platzhalterwerte | ✅ Felder nullable, nur aus vorhandenen Notion-Spalten; alte Platzhalter im Wochenplan werden entfernt |
| 13 Portionen aus beliebiger Zahl | ✅ Nur `Portionen`/`Portion` |
| 14 Nur 100 Blöcke | ✅ Block-Pagination (verschachtelte Blöcke weiterhin nicht) |
| 15 Favoriten | ✅ Lokal gespeichert, Filter „Nur Favoriten“, Lesezeichen in Liste und Detail |
| 16 Funktionen ohne Wirkung | ✅ Filter-Knopf ist jetzt der Favoriten-Filter; Erinnerungs-Schalter und Einkaufs-Sync-Attrappe entfernt; „Zuletzt synchronisiert“ ist echt |
| 17 Notion-Rückschreiben | ✅ Fehler werden gemeldet, Bewertung wird zurückgesetzt; `Geplant am` = nächstes geplantes Datum, wird beim Entfernen der letzten Planung geleert. Dabei gefunden: Leeren des Datums hat vorher nie funktioniert (Gson ließ `null` weg) |
| 18 Sync blockiert Eingaben | ✅ Netzwerk außerhalb des Mutex, Änderungen während des Uploads bleiben „ausstehend“; 429-Retry mit `Retry-After` |
| 19 Widget-Tipp | ⏳ Auf echtem Gerät mit Android 14 prüfen (Tipp auf eine Zeile im Einkaufslisten-Widget muss die App öffnen) |
| 20 Rotation | ✅ Intent nur beim ersten Start auswerten |
| 21 Zwei API-Versionen | ✅ Alles auf `2025-09-03` über `NotionRecipeReader` |
| 22 JSON per String | ✅ Gson-Maps |
| 23 Duplikate in der Einkaufsliste | ✅ Zusammenführen bei gleichem Namen und umrechenbarer Einheit; Änderungen unter einem Lock |
| 24 Zutaten-Parser | ✅ Gemischte und Unicode-Brüche, Spannen, Zeilen vor Kommas |
| 25 Code-Struktur | 🔶 Toter Code entfernt (Beispiel-Wochenplan/-Einkaufsliste, ungenutzte Status-Komponenten); lange Zeilen bestehen teilweise weiter |
| 26 Build-Versionen | ✅ AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21 mit Compose-Compiler-Plugin, Compose BOM 2025.12.01, compileSdk/targetSdk 36, alle Abhängigkeiten im Versionskatalog, `proguard-rules.pro` angelegt. AGP 9 bewusst noch nicht (größere Migration) |
| 27 CI | ✅ GitHub Actions: Tests, Lint, Preview-APK als Download-Artefakt |
| 28 Lokale Bilder | ✅ Ersetzte und gelöschte Fotos werden entfernt |
| 29 Strings in Ressourcen | ⏳ Offen, erst bei Bedarf einer zweiten Sprache |

Issue [#6](https://github.com/realgroundzero-gif/yummify/issues/6) (Rezept teilen) ist umgesetzt: `share/RecipeShareText` (Text), `share/RecipeCardRenderer` (Karte per Canvas), `share/RecipeSharer` (Cache, FileProvider, Share-Sheet). Offen für einen Test auf dem Gerät: Darstellung als Bildunterschrift in WhatsApp, Telegram und Signal sowie der Alternativ-Intent für reine Text-Apps (`EXTRA_ALTERNATE_INTENTS`).

Issue [#5](https://github.com/realgroundzero-gif/yummify/issues/5) ist in Schritt A und B umgesetzt (Version 1.3.0): `IngredientMatcher` (regelbasierter Abgleich, überall verwendet) und `WeekShoppingPlanner` mit „Aus Wochenplan hinzufügen“ und dem Abschnitt „Bereits im Vorrat“. **Schritt C ist umgesetzt:** Die Messung (#8, 134 Paare in `ingredient_pairs.tsv`) hat ergeben, dass weder Universal Sentence Encoder noch BERT-Embedder deutsche Zutaten ohne falsche Zusammenlegung trennen; der Regel-Matcher findet 48 von 58 gleichen Paaren und verbindet kein verschiedenes. Es wird deshalb kein Modell eingebaut. Die nicht erkannten Fälle (z. B. „Hähnchenbrust“ und „Hähnchenbrustfilet“) löst „Vielleicht schon da“ in der Einkaufsliste (#9) als Vorschlag.

Neu: Build-Typ `preview` (`de.yummify.app.preview`, „Yummify Preview“) für Test-APKs, die neben der normalen App laufen.

---

Stand: 07.10.2026, Commit `026e2b1` (Version 1.1.0, versionCode 2). Gelesen wurde der gesamte Quellcode (≈ 8 400 Zeilen Kotlin/XML in `app/src`), die Build-Konfiguration und die Tests. Build und Tests konnten in dieser Umgebung nicht laufen, weil das Android SDK nicht installiert werden konnte (`dl.google.com` ist im Netzwerk blockiert). Alle Befunde stammen deshalb aus dem Lesen des Codes. Jeder Befund nennt Datei und Zeile.

## 1. Überblick

| Bereich | Zustand |
|---|---|
| Inventar (lokal + Notion-Sync, Barcode, Open Food Facts, Fotos) | **Am ausgereiftesten.** Offline-first, Tombstones, Wiederaufnahme über `Artikel-ID`, Schema-Prüfung, gute Tests. |
| Rezepte (Notion lesen/anlegen, Bewertung, Portionen) | Funktioniert. Kein Cache, alte Notion-API-Version, Platzhalterwerte, Fehler werden verschluckt. |
| Wochenplan + Widget | Funktioniert lokal. Beispieldaten werden dauerhaft eingespielt, Notion-Rückschreiben ist „fire and forget“. |
| Einkaufsliste + Widget | Funktioniert lokal. Beispieldaten beim ersten Start, fehlerhafte Kategorie-Heuristik, Sync-Button ohne Funktion. |
| Einstellungen | Funktioniert. Token liegt im Klartext in einer gesicherten Prefs-Datei. |

**Stack:** Kotlin 1.9.23, AGP 8.3.2, Gradle 8.5, Compose BOM 2024.03, Material 3, Navigation Compose 2.7.7, OkHttp 4.12, Gson, Coil 2, ML Kit Code Scanner. minSdk 26, compile/target 34. Kein DI-Framework, kein Room und kein WorkManager.

## 2. Architektur

```
MainActivity (NavHost, Bottom-Bar)
 └─ ui/screens/<feature>/{Screen, ViewModel}      AndroidViewModel, StateFlow
     └─ data/repository/*                          Singletons über getInstance(context)
         ├─ UserPreferencesRepository              SharedPreferences "yummify_user_prefs"
         ├─ InventoryRepository                    SharedPreferences "yummify_inventory" (JSON via Gson), Mutex, eigener CoroutineScope
         ├─ MealPlanRepository                     SharedPreferences "yummify_meal_plan"
         ├─ ShoppingListRepository                 SharedPreferences "yummify_shopping_list"
         └─ RecipeRepository                       KEIN Singleton – pro Aufruf neu erzeugt, mit eigenem OkHttpClient
     └─ data/remote/*
         ├─ NotionInventoryApi                     Notion-Version 2025-09-03, data_sources
         ├─ NotionRecipeApi                        2025-09-03, nur Anlegen
         └─ OpenFoodFactsApi
 widget/*                                          RemoteViews, lesen die Repository-StateFlows direkt
```

Wichtige Konventionen im Bestand:
- Die Oberfläche ist komplett deutsch. Texte sind fest im Code und nicht in `strings.xml`.
- Persistenz: JSON-Listen in SharedPreferences. Das Inventar schreibt mit `commit()` und prüft das Ergebnis, die anderen Repositories nutzen `apply()`.
- Fehler aus dem Inventar werden als `IllegalArgumentException` oder `IOException` mit deutscher Nutzernachricht geworfen und im ViewModel als `message` angezeigt.
- Tests: JUnit, MockWebServer und Robolectric unter `app/src/test`. Sie decken vor allem Inventar, Notion-Mapping, Open Food Facts und die Widgets ab.

## 3. Stärken

- **Inventar-Sync ist sorgfältig gebaut.** Lokale Änderungen haben Vorrang, Löschungen bleiben als Tombstone erhalten, und ein abgebrochener Create wird über die stabile `Artikel-ID` wiedergefunden (`NotionInventoryApi.kt:77`). Mehrere Datenquellen werden abgelehnt, statt in die falsche Tabelle zu schreiben (`:48`).
- **Das Notion-Schema wird geprüft, bevor geschrieben wird.** Bestehende Spalten werden nie umbenannt, und Produktspalten werden nur benutzt, wenn es sie schon gibt.
- **Bildverarbeitung:** EXIF-Rotation, Downsampling auf 1600 px, 5-MB-Grenze, privater Speicher und FileProvider.
- **`InventoryMath.consume` bucht atomar ab.** Entweder werden alle Zutaten abgebucht oder keine, und die früheste Charge (MHD) geht zuerst.
- **Gute Testabdeckung** für das Inventar und die Notion-Feldzuordnung (62 Tests).
- **Barrierefreiheit** wurde mitgedacht: Löschaktion für Screenreader, Content-Descriptions, Bottom-Bar skaliert mit der Schriftgröße.

## 4. Befunde

Priorität: **P1** = Sicherheit oder Datenverlust, bald beheben · **P2** = sichtbarer Fehler oder Performance · **P3** = Qualität und Wartbarkeit.

### P1 – Sicherheit und Daten

1. **Notion-Token landet im Logcat.** `RecipeRepository.kt:30` setzt `HttpLoggingInterceptor.Level.BODY` ohne `redactHeader("Authorization")`, und zwar auch im Release-Build. Damit stehen der Bearer-Token und alle Antworten im Gerätelog.
   → Nur in Debug-Builds loggen und `redactHeader("Authorization")` setzen.
2. **Das Signier-Passwort steht im Repo.** In `app/build.gradle.kts` sind `storePassword` und `keyPassword` (`yummify123`) eingecheckt. Die Keystore-Datei selbst ist nicht versioniert, das Passwort aber in der Git-Historie.
   → Werte aus `local.properties` oder Umgebungsvariablen lesen. Für einen Play-Store-Release einen neuen Schlüssel erzeugen.
3. **Der Token liegt im Klartext und wird mitgesichert.** `UserPreferencesRepository` speichert den Notion-Token in normalen SharedPreferences, und `AndroidManifest.xml:8` setzt `allowBackup="true"`. Damit kommt der Token ins Cloud- oder adb-Backup.
   → Prefs-Datei per `dataExtractionRules`/`fullBackupContent` vom Backup ausschließen oder den Token verschlüsselt ablegen (Android Keystore).
4. **Beschädigter Inventar-Speicher führt zu einer Absturzschleife.** `InventoryRepository.kt:84` (`load`) lässt eine `JsonSyntaxException` durchlaufen. Das ist laut Kommentar Absicht, damit keine Daten verloren gehen. Weil das Repository aber in `MainActivity.onResume` erzeugt wird, stürzt die App dann bei jedem Start ab.
   → Rohdaten unter einem Backup-Schlüssel sichern, mit leerer Liste weiterlaufen und eine Meldung zeigen.
5. **Gson + Kotlin-Defaults.** Gson ignoriert Default-Werte von Kotlin-Datenklassen. Ein neues Feld in `InventoryItem`, `ShoppingItem` oder `MealPlanItem` ist für bereits gespeicherte Daten deshalb `null`, auch wenn der Typ non-null ist. Beispiel: `categoryOptions` kam erst in `0bf414c` hinzu. Bei einem Artikel aus der Zeit davor, der lokal noch geändert war, wirft `categoryOptions.isNotEmpty()` eine NPE.
   → Beim Laden normalisieren (`copy(categoryOptions = categoryOptions ?: emptyList())`) oder auf kotlinx.serialization umsteigen. Spätestens bei Minify (R8) bricht Gson-Reflection ohne Keep-Regeln.

### P2 – Fehler, die man in der App sieht

6. **Beispieldaten werden dauerhaft eingespielt.** `MealPlanRepository.kt:28` schreibt beim ersten Start `SampleData.mealPlanItems` (11 Einträge ohne `plannedDate`, IDs „1“–„6“) in den Speicher. `ShoppingListRepository.kt:25` zeigt Beispielartikel, bis die Liste zum ersten Mal gespeichert wird. Die Wochenplan-Einträge tauchen über `matchesDate` als „Geister“ auf, sobald der Montag der aktuellen Woche zum Beispiel auf den 28. fällt, auch im Widget. Beim Antippen bleibt die Detailansicht ewig im Ladezustand, weil es die Rezept-ID in Notion nicht gibt.
   → Für neue Nutzer leer starten. Eine einmalige Migration entfernt die Einträge mit den IDs `m1`–`m11`.
7. **Falsche Einkaufskategorie.** In `ShoppingListRepository.kt:94` trifft `contains("Ei", ignoreCase = true)` auch auf „Rind**flei**sch“, „R**ei**s“, „W**ei**zenmehl“ und „W**ei**ßwein“. Fleisch landet so in „Kühlregal“, weil die Fleisch-Regel erst danach geprüft wird. Ähnlich wirkt „Erz“ (Zeile 108).
   → Auf ganze Wörter prüfen (`\bEi(er)?\b`) und die Regeln als Tabelle mit Tests ablegen.
8. **Datumsauswahl im Plan-Dialog um einen Tag verschoben.** `RecipeDetailScreen.kt:514` und `:582` rechnen das Datum mit `ZoneId.systemDefault()` um, der Material-3-`DatePicker` arbeitet aber in UTC. In Deutschland (UTC+1/+2) markiert der Kalender deshalb anfangs den Vortag, und die Schnellwahl „Heute/Morgen“ hebt im Kalender den falschen Tag hervor. `InventoryDetailScreen.kt:340` macht es richtig mit `ZoneOffset.UTC`.
9. **Notion-URL wird nicht erkannt.** `RecipeRepository.formatNotionId` (`:531`) nimmt alles nach dem letzten `/` und entfernt die Bindestriche. Bei Links der Form `notion.so/workspace/Rezepte-0123…ef?v=…` entsteht daraus `Rezepte0123…`, also keine gültige ID.
   → Die letzten 32 Hex-Zeichen per Regex herausziehen.
10. **Rezepte werden ständig neu geladen.** `RecipeRepository` hat keinen Cache und erzeugt bei jeder Instanz einen neuen `OkHttpClient`. `RecipeListViewModel.kt:41` lädt alle Rezepte bei *jeder* Änderung der Einstellungen neu, also auch beim Umschalten des Dark Mode. `MealPlannerViewModel.kt:96` lädt alle Rezepte bei jeder Änderung des Wochenplans neu, also auch bei jedem „gekocht“-Haken. Parallele Ladevorgänge können sich gegenseitig überschreiben.
   → Ein Singleton-`RecipeRepository` mit `StateFlow<List<Recipe>>`, gemeinsamem OkHttpClient und Cache (im Speicher, optional auf Platte). Neu laden nur, wenn sich Token oder Datenbank-ID ändern (`distinctUntilChanged`).
11. **Fehler sehen aus wie „keine Rezepte“.** `getAllRecipes` gibt bei einem Fehler `emptyList()` zurück. Bricht die Pagination ab, kommt still eine Teilliste zurück (`:200`). `getRecipeById` liefert bei Fehlern `null`, und die Detailansicht zeigt dann einen endlosen Spinner (`RecipeDetailScreen.kt:49`) ohne Zurück-Button.
   → `Result`/sealed `LoadState` mit Fehlertext und „Erneut versuchen“.
12. **Platzhalterwerte bei Notion-Rezepten.** `RecipeRepository.kt:396` ff. setzt für jedes Rezept 25 Min., 450 kcal, 25 g Protein, „Einfach“ und „€€ (Günstig)“. Diese Werte erscheinen in Detail, Karte und Wochenplan.
   → Felder nullable machen und nur anzeigen, wenn sie in Notion gepflegt sind (z. B. `Zeit`, `Kalorien`).
13. **Portionen aus einer beliebigen Zahl.** Fehlt `Portionen`, nimmt `RecipeRepository.kt:328` die *erste* Zahlenspalte, etwa `Bewertung` oder `Kalorien`.
14. **Lange Zubereitungen werden abgeschnitten.** `fetchPageBlocksAsMarkdown` (`:103`) lädt nur die ersten 100 Blöcke, ohne `has_more`/`start_cursor`. Verschachtelte Blöcke (`has_children`) werden ignoriert.
15. **Favoriten werden nicht gespeichert.** Liste (`RecipeCard.kt:37`, eigener `remember`-State), Listen-ViewModel und Detail-ViewModel halten jeweils einen eigenen Favoritenstatus nur im Speicher. Nach Navigation oder Neustart ist er weg. Das README beschreibt die Funktion aber als vorhanden.
16. **Funktionen ohne Wirkung:** Der Sync-Button der Einkaufsliste (`ShoppingListViewModel.kt:93`) wartet nur 1,5 s. Der Filter-Button in der Rezeptsuche (`RecipeListScreen.kt:252`) hat `onClick = {}`. Der Schalter „Erinnerungen“ ist mit nichts verbunden. „Zuletzt synchronisiert: Vor 3 Min.“ ist ein fester Text.
17. **Notion-Rückschreiben ohne Rückmeldung.** `updatePlannedDate` und `updateRating` ignorieren Fehler. Weil es pro Rezept nur ein Feld `Geplant am` gibt, überschreibt eine zweite Planung desselben Rezepts das Datum. Entfernt man eine von zwei Planungen, wird das Feld geleert (`MealPlannerViewModel.removeMeal`).
18. **Sync blockiert Eingaben.** `InventoryRepository.sync` hält den Mutex während der ganzen Netzwerkphase. Ein „Speichern“ wartet deshalb bis zum Netzwerk-Timeout (bis 30 s pro Request). Außerdem gibt es kein Backoff bei Notion-Rate-Limits (HTTP 429, etwa 3 Anfragen/s).
19. **Widget-Tipp öffnet eventuell die App nicht.** `ShoppingListWidgetProvider.kt:42` ruft `startActivity` aus einem BroadcastReceiver auf. Ab Android 10/14 wird das durch die Einschränkungen für Activity-Starts aus dem Hintergrund unter Umständen blockiert. Das muss auf einem echten Gerät mit Android 14 geprüft werden. Eine robuste Variante wäre ein Activity-`PendingIntent`-Template mit einer Toggle-Action, die die Activity ohne UI verarbeitet.
20. **Rotation öffnet die Detailansicht doppelt.** `MainActivity.handleIntent` wird in `onCreate` bei jeder Neuerstellung erneut aufgerufen. Kam die App über einen Intent mit `recipeId`, legt jede Neuerstellung (Rotation, Dark-Mode-Wechsel) erneut `recipe_detail` auf den Back-Stack.
    → Intent nur auswerten, wenn `savedInstanceState == null`.

### P3 – Qualität und Wartbarkeit

21. **Zwei Notion-API-Versionen.** `RecipeRepository` nutzt noch `2022-06-28` mit `databases/{id}/query`, Inventar und Rezeptanlage nutzen `2025-09-03` mit `data_sources`. Das Lesen der Rezepte sollte auf `data_sources` umziehen. Gemeinsamer `NotionClient` mit einheitlichem Header, Fehlerbehandlung und Retry.
22. **JSON per String-Interpolation** in `updatePlannedDate` und `updateRating`. Das ist im Moment ungefährlich, weil die Werte intern erzeugt werden, aber fehleranfällig. Gson-Maps verwenden, wie in den neueren APIs.
23. **Einkaufsliste führt doppelte Einträge nicht zusammen.** `addIngredients` fügt dieselbe Zutat aus zwei Rezepten zweimal ein. Read-modify-write auf `_items.value` ohne Synchronisation, Widget-Broadcast und UI können sich überschneiden.
24. **Zutaten-Parser:** Gemischte Brüche („1 1/2 EL“), Unicode-Brüche („½“) und Spannen („2–3“) werden nicht erkannt. Das Komma-Splitting (`,(?![0-9])`) zerlegt „Salz, Pfeffer“, aber auch „1 Zwiebel, gewürfelt“.
25. **Code-Struktur:** Viele voll qualifizierte Aufrufe (`de.yummify.app.data.repository.…`) statt Imports, sehr lange Einzeiler (z. B. `InventoryRepository.kt:224`, `InventoryScreen.kt` mit über 250 Zeichen pro Zeile). `SampleData.kt` (404 Zeilen) ist größtenteils toter Code (`RecipeRepository.getMealPlan` und `getShoppingItems` werden nicht verwendet).
26. **Build:** `proguard-rules.pro` fehlt, wird aber referenziert (wirkt erst mit Minify). Bibliotheksversionen stammen von Anfang 2024: AGP 8.3, Kotlin 1.9, Compose BOM 2024.03, targetSdk 34. Google Play verlangt inzwischen ein neueres targetSdk. Ein Upgrade auf Kotlin 2.x mit dem Compose-Compiler-Plugin, AGP 8.7+ und targetSdk 35/36 ist nötig. Bei targetSdk 35 wird Edge-to-Edge erzwungen; das passt zu `enableEdgeToEdge()`, die Insets müssen trotzdem geprüft werden.
27. **Keine CI.** Eine GitHub Action mit `./gradlew testDebugUnitTest lintDebug` würde Rückschritte früh zeigen.
28. **Lokale Bilder bleiben liegen.** Nach dem Löschen eines Artikels oder dem Ersetzen eines bereits synchronisierten Fotos bleiben JPEGs in `filesDir/inventory_images`.
29. **Strings sind nicht in Ressourcen.** Für eine spätere englische Oberfläche müssten alle Texte nach `strings.xml` verschoben werden.

## 5. Testlücken

Die Tests decken Inventar, Notion-Inventar, Open Food Facts und Widgets gut ab. Es fehlen:
- `RecipeRepository.parseIngredientLine`, `formatNotionId` und `NotionPage.toRecipe` (Befunde 9, 13, 24)
- `ShoppingListRepository.addIngredients` mit Kategorie-Heuristik und Duplikaten (Befunde 7, 23)
- `MealPlanRepository` (Ersetzen pro Datum und Mahlzeit, Legacy-Matching)
- ViewModels (z. B. mit `kotlinx-coroutines-test`)

## 6. Empfohlene Reihenfolge

1. **Schnelle Fixes (P1 und kleine P2):** Logging redigieren, Signier-Passwort auslagern, Backup-Regeln, Absturzschleife beim Laden, „Ei“-Kategorie, DatePicker in UTC, `formatNotionId`, Beispieldaten entfernen, Rotation-Deep-Link. Jeweils mit einem Unit-Test.
2. **Rezept-Datenschicht:** Singleton-Repository mit Cache und `LoadState`, Umzug auf `data_sources`, Block-Pagination, nullable Nährwerte und Zeiten, gespeicherte Favoriten.
3. **Build-Modernisierung:** Kotlin 2.x, AGP, Compose BOM, targetSdk 35+, CI-Workflow.
4. **Optional, Architektur:** Room statt JSON in SharedPreferences, WorkManager für den Inventar-Sync im Hintergrund (ersetzt die 60-s-Schleife, die nur läuft, solange der Prozess lebt), einfache Dependency Injection (Hilt oder manuell).
