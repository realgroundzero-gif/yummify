# Yummify

**Deutsch** · [English](README.en.md)

Yummify ist eine Android-App für Rezepte, Wochenplanung, Einkaufsliste und Lebensmittelvorräte. Sie verbindet deine Notion-Rezept- und Inventardatenbanken mit einer Oberfläche aus Kotlin und Jetpack Compose. Die App-Oberfläche ist derzeit deutschsprachig.

## Funktionen

### Rezepte

- Rezepte aus Notion laden, einschließlich aller Ergebnisse über Datenbank-Pagination, Seiten-Cover, Kategorien, Küche und Tags. Die Rezepte werden einmal pro Verbindung geladen und von allen Bereichen gemeinsam genutzt; ein erneuter Abruf erfolgt über „Erneut versuchen“ oder die Synchronisation in den Einstellungen.
- Der zuletzt geladene Stand wird auf dem Gerät gespeichert. Ohne Netz zeigt die App diese Rezepte mit einem Offline-Hinweis; Ladefehler werden mit Ursache und „Erneut versuchen“ angezeigt, statt als leere Liste.
- Nach Titel, Beschreibung und Zutaten suchen; Kategorien aus den vorhandenen Rezepten filtern. Filter bleiben beim Scrollen sichtbar.
- Zutatenmengen durch Ändern der Portionen skalieren. Der Parser versteht Dezimalzahlen („1,5 kg“), Brüche („1/2 TL“, „1 1/2 EL“, „½ Bund“), Spannen („2–3 Zehen“, es zählt der größere Wert) und übliche Einheiten. Stehen Zutaten in Notion zeilenweise, bleiben Zusätze wie „1 Zwiebel, gewürfelt“ zusammen.
- Zubereitung aus Notion-Seiteninhalten anzeigen, mit Unterstützung für Überschriften, Listen, Zitate, Hinweise und Textformatierung. Auch lange Seiten mit mehr als 100 Blöcken werden vollständig geladen.
- Zeit, Kalorien, Schwierigkeit und Kosten erscheinen nur, wenn die passenden Notion-Spalten gepflegt sind (siehe Tabelle unten). Fehlt ein Cover, zeigt die App ein neutrales Platzhalterbild.
- Favoriten über das Lesezeichen merken. Sie werden auf dem Gerät gespeichert (nicht in Notion) und lassen sich über das Lesezeichen in der Suchleiste als Filter „Nur Favoriten“ anzeigen.
- Rezepte teilen: Das Teilen-Symbol auf dem Titelbild öffnet das Android-Teilen-Menü mit einer Rezeptkarte (Bild, 1080 Pixel breit mit Titel, Zeit, Portionen, Kategorie und Zutaten) und einem Text mit Zutaten und Zubereitung. Mengen entsprechen den aktuell eingestellten Portionen. Chat-Apps wie WhatsApp und Telegram zeigen den Text meist als Bildunterschrift; wie eine App Bild und Text zusammen darstellt, entscheidet die Empfänger-App. Apps, die nur Text annehmen (z. B. Notizen), erhalten den reinen Text. Angaben ohne Notion-Spalte (z. B. die Zeit) entfallen. Der Notion-Link wird nicht mitgeteilt, weil er privat ist. Die Bilddatei liegt nur im App-Cache und wird beim nächsten Teilen gelöscht.
- Bewertungen von einem bis fünf Sternen nach Notion zurückschreiben. Schlägt das Speichern fehl, wird die vorherige Bewertung wiederhergestellt und der Grund angezeigt.
- Über den Plus-Button neue Rezepte direkt in Notion erstellen: Name, Beschreibung, Portionen, Kategorie, Zutaten, Zubereitungsschritte und optionales HTTPS-Cover. Das Formular prüft Eingaben und Datenbankschema; bei Fehlern bleibt der Entwurf erhalten.
- Ohne eingerichtete Notion-Verbindung stehen Beispielrezepte zur Verfügung. Das ist kein vollständiger Offline-Abgleich der Rezeptdatenbank.

### Wochenplan und Widget

Rezepte lassen sich für ein Datum und eine Mahlzeit planen: Frühstück, Mittagessen, Abendessen oder Snack. Der Wochenplan wird lokal gespeichert und unterstützt den Wechsel zwischen Wochen sowie das Markieren gekochter Gerichte. Ein neuer Wochenplan startet leer; Beispiel-Einträge früherer Versionen werden beim ersten Start entfernt.

Bei verbundenen Notion-Rezepten wird das geplante Datum in das Datumsfeld `Geplant am` zurückgeschrieben, sofern die Spalte existiert. Da Notion pro Rezept nur ein Datum kennt, steht dort das nächste geplante Datum ab heute (sonst das letzte vergangene). Wird die letzte Planung eines Rezepts entfernt, wird das Feld geleert. Kann Notion nicht erreicht werden, bleibt der lokale Plan gespeichert und die App meldet, dass das Notion-Datum nicht aktualisiert wurde. Eine Nährwert-Tagesübersicht am Ende des Planers wird nicht angezeigt.

Das Startbildschirm-Widget zeigt Montag bis Sonntag mit Kalenderwoche, Datum und geplanten Gerichten. Der aktuelle Tag ist hervorgehoben; leere Tage zeigen „Noch nichts geplant“. Antippen öffnet den Wochenplan. Das Widget ist ab 4×2 nutzbar, lässt sich vergrößern und bietet eine je Widget gespeicherte Hintergrundtransparenz. Seine Konfiguration zeigt das tatsächliche Layout mit den aktuellen Planungen. Änderungen am Plan, Datum und Zeitzone aktualisieren die Anzeige.

### Einkaufslisten-Widget

Ein zweites Startbildschirm-Widget zeigt die aktuelle Einkaufsliste im dunklen Yummify-Design mit Terrakotta-Akzenten. Der Kopf zählt offene und gesamte Artikel; die vertikal scrollbare Liste zeigt Namen, Mengen und Kategorien. Zutaten für heute geplante, noch nicht gekochte Rezepte stehen oben und erhalten eine „HEUTE“-Markierung. Erledigte Artikel werden nach unten sortiert und durchgestrichen. Antippen des Kreises hakt einen Artikel ab oder öffnet ihn wieder; Antippen der Zeile oder des Kopfes öffnet die Einkaufsliste in der App. Änderungen in App, Einkaufsliste und Wochenplan aktualisieren das Widget. Es unterstützt 4×2 und 4×3 sowie größere Größen; die Hintergrundtransparenz ist je Widget einstellbar. Die Konfiguration zeigt bis zu sieben echte Artikel als Vorschau.

### Inventar

- Lebensmittel mit Menge, Einheit, Kategorie, Lagerort, Mindestbestand, Mindesthaltbarkeitsdatum (MHD), Barcode, Notizen und Bild verwalten.
- Kompakte Suche, Lagerortauswahl und Filter für niedrige Bestände, bald fällige und abgelaufene Artikel.
- Alphabetisch sortierte, ein- und ausklappbare Kategorieabschnitte mit Artikelanzahl und kontrastreichen Überschriften, die beim Scrollen sichtbar bleiben. Artikel ohne Zuordnung erscheinen unter „Ohne Kategorie“; innerhalb der Gruppen wird nach MHD und Name sortiert.
- Artikel im Vollbild bearbeiten: Mengen-Stepper mit auswählbarer Schrittweite, Auswahlfelder aus dem Notion-Schema und Datumsauswahl. Die Standardschritte sind 1 für Stück und Packungen, 50 für g/ml und 0,1 für kg/l.
- Durch Wischen nach links oder rechts mit anschließender Bestätigung löschen. Für Screenreader ist die Löschaktion ebenfalls verfügbar.
- Plus-Buttons in Inventar und Rezeptübersicht berücksichtigen die tatsächliche Höhe der unteren Navigation, auch bei größerer Schrift. Beim Scrollen folgen Kopfbereich (Logo, Suche, Status) und untere Navigation dem Finger: Sie verschwinden beim Abwärtsscrollen und kommen beim Zurückscrollen sofort wieder; lässt du los, rasten sie ein. Kategorie-Chips bleiben stehen, bei laufendem Screenreader und kurzen Listen bleibt alles sichtbar.

### Barcode und automatische Produktdaten

Der Google-Code-Scanner erfasst Barcodes mit der Kamera. Dafür sind Google Play-Dienste erforderlich; das Scanner-Modul wird bei Bedarf vor dem ersten Scan heruntergeladen. Alternativ lässt sich der Barcode manuell eingeben.

Beim Anlegen eines Artikels lädt ein Scan automatisch Daten von [Open Food Facts](https://world.openfoodfacts.org). Nach manueller Eingabe kann „Produktdaten laden“ gewählt werden. Die öffentliche API wird mit deutschem Sprachwunsch und einem Yummify-User-Agent aufgerufen; Notion-Zugangsdaten werden dabei nicht übertragen.

Leere Felder werden mit Produktname und Marke, einer passenden vorhandenen Kategorie, Notizen und Produktbild ergänzt. Gescannte Artikel werden standardmäßig als **1 Stück** erfasst, auch ohne Datenbanktreffer. Automatisch erzeugte Notizen enthalten nur **Packungsinhalt und Marke**. Zutaten, Nährwerte und Produktlink stehen in separaten Feldern. Bereits gespeicherte automatische Notizen werden gekürzt, sobald die entsprechenden Produktfelder vorhanden sind; eigene Ergänzungen bleiben erhalten. Lagerort und MHD werden manuell ergänzt. Bestand und Gebinde vor dem Speichern prüfen.

Nährwerte, Zutaten und Produkt-URL werden zusätzlich in vorhandene Notion-Spalten übernommen. Die Nährwerte beziehen sich auf **100 g beziehungsweise 100 ml**, nicht auf die gesamte Packung oder den Bestand. Vorhandene lokale Barcodes führen zum bestehenden Artikel; doppelte neue Artikel mit demselben Barcode werden verhindert. Unbekannte Produkte und Netzwerkfehler lassen die manuelle Erfassung zu. Erst Speichern übernimmt den Artikel lokal; der Inventarabgleich überträgt ihn nach Notion.

### Fotos und Produktbilder

Vorhandene Notion-Cover werden in den Artikeldetails angezeigt. Die Bildaktion bietet ausdrücklich Galerie oder neue Fotoaufnahme an. Eigene Bilder werden im privaten App-Speicher als JPEG mit korrigierter Ausrichtung und maximal 1600 Pixeln auf der langen Seite vorbereitet. Beim Abgleich werden sie über die Notion-Datei-Upload-API als Seiten-Cover gespeichert. Open-Food-Facts-Bilder werden als externe HTTPS-Cover übernommen. Ausstehende Bilder bleiben bei Offline-Nutzung oder Übertragungsfehlern erhalten; normale Feldänderungen ersetzen kein bestehendes Cover.

### Vorrat, Kochen und Einkaufsliste

Rezeptzutaten zeigen den verfügbaren Vorrat. Fehlende Zutaten lassen sich unter Berücksichtigung der gewählten Portionen und kompatiblen Einheiten (g/kg, ml/l, Stück) zur Einkaufsliste hinzufügen. Steht dieselbe Zutat bereits offen auf der Liste, wird die Menge addiert (z. B. 200 g + 0,3 kg Tomaten = 500 g) und das Rezept ergänzt, statt einen doppelten Eintrag anzulegen. Die Abteilung (Obst & Gemüse, Kühlregal, Fisch & Fleisch, Gewürze & Öle, Vorrat) wird aus dem Namen abgeleitet; kurze Wörter wie „Ei“ zählen nur als ganzes Wort, damit etwa „Reis“ oder „Rindfleisch“ richtig einsortiert werden. Eine neue Einkaufsliste startet leer. Abgelaufene Vorräte zählen nicht als verfügbar. Namen werden ohne Modell und ohne Netz verglichen: Groß-/Kleinschreibung, Umlaute, Mehrzahl-Endungen, Wortreihenfolge, Mengen, Einheiten, Verpackung („Dose“, „Bund“) und Zubereitungswörter („frisch“, „gehackt“, Text in Klammern) spielen keine Rolle. So gelten „Rote Paprika“ und „Paprika rot“, „Passierte Tomaten (Dose)“ und „Tomaten passiert“ sowie „Frühlingszwiebeln, gehackt“ und „Lauchzwiebeln“ (kleine Synonymliste, z. B. auch Möhre/Karotte, Sahne/Schlagsahne, Quark/Topfen) als dieselbe Zutat. Der Abgleich ist bewusst streng: „Paprika“ passt nicht zu „Rote Paprika“, „Milch“ nicht zu „Kokosmilch“, „Butter“ nicht zu „Erdnussbutter“. Eine nicht erkannte Gleichheit lässt nur eine Zeile mehr auf der Einkaufsliste; eine falsche würde eine fehlende Zutat verschwinden lassen. Dieselbe Regel gilt für Fehlmengen, Abbuchen („Gekocht“), die Einkaufsliste und die Anzeige „Im Vorrat“.

**Einkaufsliste aus dem Wochenplan:** „Aus Wochenplan hinzufügen“ (Nächste 7 Tage, Rest dieser Woche oder Nächste Woche) sammelt die Zutaten aller noch nicht gekochten Gerichte im Zeitraum, rechnet gleiche Zutaten zusammen (auch g und kg), zieht den nicht abgelaufenen Vorrat ab und zieht ab, was schon auf der Liste steht (offen oder abgehakt). Zweimaliges Ausführen fügt deshalb nichts doppelt hinzu. Teilweise gedeckte Zutaten kommen mit der fehlenden Menge und dem Hinweis „Vorrat deckt …“ auf die Liste. Zutaten, die der Vorrat vollständig deckt, erscheinen im Abschnitt **„Bereits im Vorrat“**; mit „Doch kaufen“ wandern sie mit einem Tipp zurück auf die Liste, „Ausblenden“ räumt den Abschnitt auf. Zutaten ohne Mengenangabe („Salz“) gelten als gedeckt, wenn der Vorrat sie in beliebiger Menge enthält. Mengen gelten für die im Rezept angegebenen Portionen, weil der Wochenplan keine Portionszahl speichert. Gerichte, deren Rezept in Notion nicht mehr existiert, werden in der Meldung genannt.

**„Vielleicht schon da“:** Hat die Einkaufsliste einen offenen Eintrag, den der Abgleich nicht als Vorrat erkennt, der Vorrat aber etwas Ähnliches (nicht abgelaufen, Bestand über 0) enthält, etwa „Hähnchenbrustfilet“ auf der Liste und „Hähnchenbrust“ im Vorrat, fragt die App nach. „Ist vorrätig“ nimmt den Eintrag von der Liste und legt ihn unter „Bereits im Vorrat“ ab, „Doch kaufen“ macht das rückgängig. „Nein, kaufen“ blendet den Hinweis für genau dieses Namenspaar dauerhaft aus, unabhängig von der Menge. Die App ändert die Liste nie ohne deine Antwort. Ein Embedding-Modell ist dafür nicht im Einsatz: Die Messung mit 134 Zutatenpaaren (`ingredient_pairs.tsv`) hat gezeigt, dass die getesteten Modelle deutsche Zutaten nicht sauber trennen (z. B. Öl ~ Olivenöl), während der Regelabgleich 48 von 58 gleichen Paaren erkennt und kein verschiedenes Paar verbindet.

„Gekocht · Zutaten vom Vorrat abbuchen“ zieht nach Bestätigung die benötigten Mengen ab und verwendet zuerst Chargen mit dem frühesten MHD. Fehlt eine Zutat, wird nichts abgebucht. Niedrige Bestände können ohne doppelte offene Einträge auf die Einkaufsliste gesetzt werden.

Abgehakte Einkaufsartikel lassen sich mit ihren Mengen ins Inventar übertragen und aus der Einkaufsliste entfernen. Eine gespeicherte Übernahme-ID verhindert Doppelbuchungen nach einem Abbruch. Nicht numerische Mengen müssen manuell erfasst werden; Lagerort und MHD lassen sich anschließend ergänzen.

## Notion einrichten

1. Eine Notion-Integration mit Lese-, Einfüge- und Änderungsrechten einrichten und die benötigten Datenbanken beziehungsweise übergeordneten Seiten für diese Integration freigeben.
2. Unter Einstellungen den Integration-Token und die Rezept-Datenbank-ID eintragen, speichern und die Verbindung prüfen.
3. Optional eine Inventar-Datenbank-ID eintragen oder „Inventar-Datenbank in Notion anlegen“ wählen. Die App erstellt oder verwendet „Yummify Inventar“ unter der übergeordneten Seite der Rezeptdatenbank. Diese Seite muss für die Integration freigegeben sein.
4. Die Inventar-Verbindung prüfen und synchronisieren. Das Inventar verwendet denselben Token wie die Rezepte.

Als Datenbank-ID akzeptiert die App die reine ID (mit oder ohne Bindestriche) und jeden Notion-Link, auch in der Form `notion.so/arbeitsbereich/Rezepte-<id>?v=…`.

Alle Notion-Zugriffe verwenden die API-Version `2025-09-03`. Inventaranbindung und Rezeptanlage benötigen eine Datenbank mit genau einer Datenquelle; beim Lesen von Rezepten werden alle Datenquellen berücksichtigt. Das vorhandene Schema wird vor dem Schreiben geprüft. Bei Notion-Ratenbegrenzung (HTTP 429) wartet die App die von Notion genannte Zeit ab und versucht es bis zu dreimal erneut.

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
| `Bewertung` | `select` oder `number` | Sternebewertung, z. B. `★` bis `★★★★★` oder 1 bis 5; der Typ wird aus dem Schema gelesen |
| `Geplant am` | `date` | Optionales Zurückschreiben des geplanten Datums |
| `Zeit`, `Zubereitungszeit`, `Kochzeit` oder `Dauer` | `number` oder Text, z. B. „30 Min.“ | Optional: Zubereitungszeit in Minuten |
| `Kalorien` oder `kcal`, `Protein`, `Kohlenhydrate`, `Fett` | `number` oder Text | Optional: Nährwerte pro Portion |
| `Schwierigkeit` oder `Aufwand` | `select` oder Text | Optional: Anzeige in der Rezeptansicht |
| `Kosten` oder `Preis` | `select` oder Text | Optional: Anzeige in der Rezeptansicht |
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

Inventaränderungen werden zuerst lokal gespeichert. Ausstehende Änderungen, Löschungen und Bilder bleiben bis zur erfolgreichen Übertragung erhalten. Während eines Abgleichs bleibt das Inventar bedienbar: Speichern wartet nicht mehr auf Notion. Wird ein Artikel geändert, während er gerade übertragen wird, bleibt er als ausstehend markiert und wird im nächsten Durchgang erneut gesendet. Ist der lokale Inventarspeicher beschädigt, startet die App trotzdem, sichert die Rohdaten und lädt den Stand beim nächsten Abgleich aus Notion. Nicht mehr benötigte eigene Fotos werden nach dem Ersetzen oder Löschen eines Artikels entfernt. Mit aktiviertem automatischem Abgleich wird beim Öffnen und nach Änderungen synchronisiert; wartende Änderungen werden etwa jede Minute erneut versucht, solange der App-Prozess läuft. Zusätzlich ist ein manueller Abgleich möglich. Ohne automatischen Abgleich erfolgt die Übertragung nur manuell.

Das Inventar ist nach Datenbank getrennt. Ein zunächst lokal erfasstes Inventar wird bei der ersten Einrichtung übernommen. Noch nicht synchronisierte lokale Änderungen haben bei Konflikten Vorrang; Änderungen anderer Geräte werden beim nächsten Abgleich geladen. Wochenplan, Einkaufsliste und Favoriten werden lokal gespeichert.

## Daten und Backup

Einstellungen → „Daten und Backup“ bündelt alles rund um die lokalen Daten. Die App braucht dafür keine Speicherberechtigung: Die Dateiauswahl übernimmt der Android-Dateiwähler.

- **Daten exportieren:** Erstellt eine ZIP-Datei mit Rezepte-Cache, Favoriten, Wochenplan, Einkaufsliste (samt „Bereits im Vorrat“ und abgelehnten Vorschlägen), Inventar, Einstellungen, den lokalen Produktbildern und einem Manifest (Formatversion, App-Version, Zeitstempel). Die Erfolgsmeldung nennt die Zahl der gesicherten Elemente. Wird die Datei nicht vollständig geschrieben, meldet die App einen Fehler und löscht die Teildatei.
- **Notion-Token:** Standardmäßig ist der Token nicht im Export. „Notion-Token in Export aufnehmen“ steht beim Öffnen immer auf Aus und verlangt eine ausdrückliche Bestätigung. Die Datei ist nicht verschlüsselt; wer sie besitzt, kann den Token lesen.
- **Daten importieren:** Die Datei wird zuerst vollständig geprüft (gültige ZIP, Manifest, lesbare JSON-Dateien, bekannte Formatversion). Erst nach der Zusammenfassung und deiner Bestätigung wird etwas geändert. Bei einer defekten Datei, einem fremden Format oder einer neueren Formatversion bricht der Import mit einer Meldung ab, und deine Daten bleiben unverändert. Schlägt das Übernehmen mittendrin fehl, wird der vorherige Stand wiederhergestellt. Ältere Exporte ohne neuere Felder werden beim Laden ergänzt.
- **Zusammenführen** (Standard): Elemente werden über ihre ID zusammengelegt, nichts geht verloren. Weil einzelne Elemente keinen Änderungszeitpunkt haben, entscheidet bei Konflikten der Zeitpunkt je Bereich: Ist der Export neuer als die letzte lokale Änderung des Bereichs, gewinnt die Datei, sonst (auch bei Gleichstand) der lokale Stand. Nicht abgeglichene lokale Inventar-Änderungen haben immer Vorrang vor einem unveränderten Dateistand. Aus der Datei übernommene Inventar-Einträge werden als ausstehend markiert und beim normalen Abgleich nach Notion übertragen. Löschungen werden nicht übertragen.
- **Vorhandene Daten ersetzen:** Ersetzt die lokalen Daten nach einer Sicherheitsabfrage durch den Dateiinhalt. Notion wird nicht verändert.
- **Token im Import:** Enthält die Datei einen Token, fragt die App vor der Übernahme. Ohne Zustimmung bleibt der aktuelle Token. Datenbank-IDs werden nur übernommen, wenn lokal noch keine gesetzt ist.
- **Automatisches Backup:** Schalter, Standard an. Dann sichert Android die App-Daten gemäß den Backup-Regeln, immer ohne den Notion-Token. Aus: Android sichert keine App-Daten (Cloud-Backup und Geräteübertragung). Bereits vorhandene Backups bleiben bestehen und lassen sich in den Android-Einstellungen löschen.
- **Speicher:** Zeigt getrennt, wie viel Platz Daten (JSON/Einstellungen), lokale Produktbilder und Cache belegen. „Bildcache leeren“ leert nur den Cache; Rezepte, Inventar und Produktbilder bleiben.
- **Alle lokalen Daten löschen:** Entfernt Rezepte-Cache, Favoriten, Wochenplan, Einkaufsliste, Inventar inklusive lokaler Produktbilder und den Cache von diesem Gerät. Nach Warnung (inklusive Hinweis auf nicht synchronisierte Inventar-Änderungen) und Eintippen von „LÖSCHEN“. Es wird nichts an Notion geschrieben oder gelöscht; Token, Datenbank-IDs und Einstellungen bleiben, Widgets werden aktualisiert.

## Datenschutz und Sicherheit

- Der Notion-Token wird nur auf dem Gerät gespeichert und ist von Android-Cloud-Backups und Geräteübertragungen ausgeschlossen. Nach einem Gerätewechsel muss er neu eingegeben werden.
- Netzwerkprotokolle gibt es nur in Debug- und Preview-Builds; sie enthalten nur Header, und der `Authorization`-Header wird geschwärzt. Release-Builds protokollieren keine Anfragen.
- Open Food Facts erhält nur den Barcode, nie Notion-Zugangsdaten.

## Entwickeln, bauen und installieren

Aktueller App-Stand: **1.3.0**, Android **8.0+ (API 26)**; Compile-/Target-SDK **36** (Android 16).

Benötigt werden JDK 17 oder neuer, Android SDK 36 und die Android-Build-Werkzeuge. Android Studio kann für die Entwicklung verwendet werden. Den SDK-Pfad über `local.properties` (`sdk.dir=…`) oder `ANDROID_HOME` konfigurieren.

```bash
# Debug-APK bauen
./gradlew assembleDebug

# Auf einem über USB angeschlossenen Gerät installieren und starten
adb -d install -r app/build/outputs/apk/debug/app-debug.apk
adb -d shell am start -n de.yummify.app/.MainActivity

# Tests und Android Lint ausführen
./gradlew testDebugUnitTest lintDebug

# Test-Build „Yummify Preview“ bauen
./gradlew assemblePreview
adb -d install -r app/build/outputs/apk/preview/app-preview.apk
```

**Preview-Build:** `assemblePreview` erzeugt `app-preview.apk` mit der Paket-ID `de.yummify.app.preview` und dem Namen „Yummify Preview“. Die App wird neben der normalen Yummify-App installiert, ersetzt sie nicht und teilt keine Daten mit ihr. So lassen sich neue Versionen gefahrlos ausprobieren; Notion-Token und Datenbank-IDs müssen dort einmal eingetragen werden. Die APK ist mit dem Debug-Schlüssel signiert und nur zum Testen gedacht.

Die Tests decken unter anderem Mengenrechnung, Haltbarkeit, atomare Rezept-Abbuchung, Datumsermittlung, Pagination, Notion-Feldzuordnung einschließlich `kcal`, Rezeptanlage und Rezept-Lesen, Zutaten-Parser, Notion-Links, Ratenbegrenzung, Einkaufskategorien und Zusammenführung, Fehlerbehandlung, Wiederaufnahme unterbrochener Übertragungen, beschädigten Speicher, Offline-Speicherung, Abgleich ohne Eingabe-Blockade, Einkaufs-Doppelbuchungen und Widget-Darstellung ab.

Ein Release-Build (`./gradlew assembleRelease`) wird nur signiert, wenn der Schlüssel und seine Passwörter lokal hinterlegt sind. Die Passwörter stehen nicht mehr im Repository, sondern in `local.properties` (nicht versioniert) oder in Umgebungsvariablen (`YUMMIFY_KEYSTORE`, `YUMMIFY_STORE_PW`, `YUMMIFY_KEY_ALIAS`, `YUMMIFY_KEY_PW`):

```properties
yummify.storeFile=release.jks
yummify.storePassword=…
yummify.keyAlias=yummify
yummify.keyPassword=…
```

`yummify.storeFile` ist relativ zum Ordner `app/`; Standard ist `release.jks`. Für Releases gibt es `scripts/release.sh <VERSION>`: Es prüft Branch, Milestone und Tests, setzt den Tag `vX.Y.Z`, baut die signierte APK und legt sie mit Checksumme in `releases/` ab. `versionName` kommt aus dem letzten Tag, `versionCode` ist die Anzahl der Commits. Das Backlog liegt in GitHub Issues, Einrichtung und Ablauf des Agent-Teams stehen in [docs/agent-team.md](docs/agent-team.md). Ohne diese Angaben entsteht eine unsignierte Release-APK. APKs, Gradle-Caches, SDK-Pfade und Signierschlüssel werden nicht versioniert.

### Automatische Prüfung (GitHub Actions)

Bei jedem Push auf `main` oder einen `claude/…`-Branch und bei jedem Pull Request laufen Unit-Tests, Android Lint und der Preview-Build (`.github/workflows/android.yml`). Die fertige `app-preview.apk` liegt anschließend 14 Tage lang im Lauf unter **Actions → Android → Artifacts → `yummify-preview-apk`** zum Herunterladen bereit (ZIP entpacken, APK auf dem Gerät öffnen).

### Technik und Quellcode

Kotlin 2.2 mit Compose-Compiler-Plugin, Android Gradle Plugin 8.13, Gradle 8.14, Jetpack Compose (BOM 2025.12) mit Material 3, Coroutines und StateFlow, OkHttp 4.12, Gson, Coil, SharedPreferences, Google-Code-Scanner und Android RemoteViews. Tests verwenden JUnit, MockWebServer und Robolectric.

- [App-Einstieg und Navigation](app/src/main/java/de/yummify/app/MainActivity.kt)
- [Datenmodelle, lokale Speicherung, APIs und Repositories](app/src/main/java/de/yummify/app/data)
- [Oberflächen und Gestaltung](app/src/main/java/de/yummify/app/ui)
- [Widget](app/src/main/java/de/yummify/app/widget)
- [Tests](app/src/test/java/de/yummify/app)

## Datenquelle und Lizenzen

Open Food Facts stellt Produktdaten unter [ODbL](https://opendatacommons.org/licenses/odbl/1-0/), einzelne Datenbankinhalte unter der [Database Contents License](https://opendatacommons.org/licenses/dbcl/1-0/) und Produktbilder unter [CC BY-SA](https://creativecommons.org/licenses/by-sa/3.0/) bereit. Produktquelle und Lizenzhinweise sind in den Produktinformationen der Artikeldetails sichtbar; der Produktlink wird im separaten Notion-Feld `URL` gespeichert. Diese Datenlizenzen gelten für die eingebundenen Produktdaten und Bilder; im Repository ist derzeit keine separate Lizenzdatei für den App-Quellcode enthalten.
