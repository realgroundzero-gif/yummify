# Veröffentlichung im Play Store: Checkliste

Stand der Hinweise: Oktober 2026. Das ist keine Rechtsberatung; Gesetze, Steuerregeln und Play-Richtlinien ändern sich, prüfe sie vor dem Schritt nach.

## Vor dem Einreichen im Code
- [ ] `app/src/main/res/values/legal_provider.xml` ausfüllen (Name, Anschrift, E-Mail). Die App zeigt den Block „Anbieter“ unter Einstellungen › Rechtliches erst dann.
- [ ] App-Bundle statt APK bauen (`./gradlew bundleRelease`); Play verlangt `.aab` für neue Apps. Signiert wird mit dem Upload-Schlüssel, Play App Signing übernimmt den Verteilungsschlüssel.
- [ ] Schlüssel sichern (Keystore und Passwörter getrennt vom Repo).
- [ ] Versionsnummer: `versionName` aus dem Git-Tag, `versionCode` aus der Commit-Zahl (steigt mit jedem Commit).

## Play Console
- [ ] Entwicklerkonto (einmalig etwa 25 US-Dollar, Identitätsprüfung). Bei privaten Konten gelten zusätzliche Testauflagen (geschlossener Test mit einer Mindestzahl an Testern über mehrere Wochen vor der Produktion); aktuelle Zahl in der Console nachlesen.
- [ ] Händlerstatus (Trader) nach EU-Recht angeben. Als Händler werden Name, Anschrift und Kontaktdaten öffentlich im Eintrag gezeigt.
- [ ] Datenschutzerklärung als öffentlich erreichbare Webseite (kein PDF) verlinken. Inhalt: siehe `LegalSection` in der App (`strings_legal.xml`).
- [ ] Formular „Datensicherheit“: Die App hat kein Konto und keine eigene Datenerhebung; sie sendet auf Nutzerwunsch Daten an Notion, Open Food Facts und ein vom Nutzer eingetragenes Home Assistant (siehe Datenschutzhinweise). Wie das einzustufen ist, entscheidet die Console-Hilfe.
- [ ] Inhaltsfreigabe (IARC-Fragebogen), Zielgruppe (nicht an Kinder), Werbung (keine), App-Kategorie.
- [ ] „App-Zugriff“: Die App braucht für den Hauptnutzen ein eigenes Notion-Konto mit Integration. Prüfer brauchen eine Anleitung oder Testzugang (Notion-Workspace mit Beispiel-Datenbank).
- [ ] Eintrag: Titel, Kurz- und Langbeschreibung, Icon 512×512, Feature-Grafik 1024×500, mindestens zwei Screenshots.
- [ ] Marken: „Notion“, „Bring!“, „Home Assistant“, „Open Food Facts“ nur beschreibend nennen, keine Zugehörigkeit andeuten. Den Namen „Yummify“ vorher auf Markenkonflikte prüfen.

## Steuern und Gewerbe (Deutschland, Überblick)
- Kostenlos, ohne Einnahmen und ohne Gewinnabsicht (Hobby): in der Regel keine Gewerbeanmeldung nötig.
- Mit Einnahmen (Kaufpreis, In-App-Käufe, Abos, Werbung): meist gewerbliche Tätigkeit. Dann Gewerbeanmeldung beim Gewerbeamt, danach Fragebogen zur steuerlichen Erfassung beim Finanzamt. „Kleingewerbe“ ist keine eigene Rechtsform, sondern ein Gewerbe mit kleinen Umsätzen; die Kleinunternehmerregelung (§ 19 UStG) befreit unter bestimmten Umsatzgrenzen von der Umsatzsteuer.
- Einnahmen sind auch ohne Gewerbe einkommensteuerpflichtig. Im Zweifel Steuerberatung oder das Finanzamt fragen.

## Webseite und Impressum
- Eine eigene Webseite ist nicht vorgeschrieben. Nötig ist eine öffentlich erreichbare Datenschutzerklärung (zum Beispiel eine einfache Seite auf GitHub Pages).
- Ein Impressum ist für rein private, nicht geschäftsmäßige Angebote nicht vorgeschrieben, bei geschäftsmäßigem Angebot (etwa mit Einnahmen) schon. Es kann auf derselben Seite stehen. Die App zeigt die Anbieterangaben unter Rechtliches.
