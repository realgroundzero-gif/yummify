# Agent-Team: Einrichtung und Ablauf

Sieben Agents begleiten eine Iteration vom Backlog bis zur signierten APK. Das Backlog liegt in GitHub Issues und wird mit der `gh`-CLI bedient.

| Agent | Aufgabe | Schreibt |
|---|---|---|
| `product-owner` | Ideen zu Epics und Stories mit Akzeptanzkriterien, Milestone = Iteration | Issues |
| `designer` | Screens, Nutzerfluss, Design-Tokens, Barrierefreiheit | `docs/design/` |
| `backend-dev` | Repositories, Speicherung, API-Clients, Fachlogik | `data/` |
| `frontend-dev` | Compose-Screens, ViewModels, Navigation | `ui/` |
| `tester` | Tests und Prüfung gegen die Akzeptanzkriterien, Votum FREIGABE / KEINE FREIGABE | Tests, Bug-Issues |
| `redakteur` | Iterations-Doku, Changelog, README-Tabelle, PR-Texte (frecher Ton) | Doku |
| `release-manager` | Definition of Done prüfen, signierte APK bauen, versionieren, taggen | Tag, `releases/` |

## Was im Repo liegt

- `.claude/agents/` die sieben Agents, `.claude/settings.json` die Berechtigungen. Gradle und Issue-Befehle laufen ohne Rückfrage. Push, PRs, Releases und `release.sh` fragen nach. Force-Push und Löschen sind gesperrt.
- `.github/ISSUE_TEMPLATE/` Vorlagen für Epic, Story und Bug.
- `scripts/setup-github.sh` legt Labels und Milestone an, `scripts/link-sub-issue.sh` hängt eine Story als Sub-Issue an ein Epic, `scripts/release.sh` prüft den Milestone, baut, signiert, taggt und schreibt Checksumme nach `releases/`.
- `CLAUDE.md` Projektkontext, Backlog-Regeln und Definition of Done.
- `app/build.gradle.kts`: `versionName` kommt aus dem letzten Tag `vX.Y.Z`, `versionCode` ist die Commit-Zahl. Ohne Tag gilt `1.3.0`.

## Was an den mitgelieferten Vorgaben geändert wurde

Das Projekt hat weder Hilt noch Room, MockK, Turbine oder Compose-UI-Tests. Damit die Agents keine Architektur nebenbei umbauen, gilt:

- `CLAUDE.md` beschreibt die echte Architektur (Repositories als Singletons, Gson in SharedPreferences). Hilt und Room einzuführen ist ein eigenes Epic und deine Entscheidung.
- `backend-dev`, `frontend-dev` und `tester` verweisen auf diese Konventionen. Der `tester` nutzt JUnit, MockWebServer und Robolectric und meldet fehlende Werkzeuge, statt sie einzubauen.
- Neue Texte kommen in `strings.xml`, bestehende werden nicht nebenbei umgezogen.
- Der `redakteur` pflegt die Release-Tabelle in `README.md` **und** `README.en.md`.
- Die Signier-Variablen heißen `YUMMIFY_KEYSTORE`, `YUMMIFY_STORE_PW`, `YUMMIFY_KEY_ALIAS`, `YUMMIFY_KEY_PW`. Die `local.properties`-Einträge `yummify.storeFile` und so weiter funktionieren weiter.
- `release.sh` nutzt auf macOS `shasum`, wenn `sha256sum` fehlt.
- Der CI-Lauf holt die volle Git-Historie, damit Version und `versionCode` stimmen.

## Noch zu tun (auf deinem Rechner)

1. `gh auth login`, danach `gh auth status` prüfen. In einer Cloud-Sitzung ist `gh` meist nicht angemeldet, dort laufen die Agents nicht. Sie sind für Claude Code auf deinem Rechner gedacht.
2. Labels und ersten Milestone anlegen: `scripts/setup-github.sh 1.4.0`. Die bisherigen Issues (#5 bis #9) tragen noch keine Labels.
3. Nach dem Merge von PR #7 auf `main` den Startpunkt taggen: `git tag -a v1.3.0 -m "Iteration 1.3.0"` und `git push origin v1.3.0`. Das Tag muss zu dem Stand passen, den du als 1.3.0 betrachtest (nicht `v0.1.0`, die App ist schon bei 1.3.0).
4. Signier-Variablen setzen (nicht im Repo): `YUMMIFY_KEYSTORE`, `YUMMIFY_STORE_PW`, `YUMMIFY_KEY_ALIAS`, `YUMMIFY_KEY_PW`. Der Keystore sollte ein neuer sein, das alte Passwort steht in der Git-Historie.
5. In Claude Code mit `/agents` prüfen, dass alle sieben Agents erkannt werden.

## Beispiel-Prompt für eine Iteration

> Iteration v1.4.0, Feature: Portionen im Wochenplan speichern. Erst product-owner: Epic und Stories als Entwurf, ich gebe sie frei. Danach designer, backend-dev, frontend-dev, tester und redakteur. Nicht pushen, keine PRs erstellen.

Beim ersten Durchlauf lässt du den `product-owner` mit dem Wort „Entwurf“ starten. Er legt dann nichts an, sondern zeigt dir den Text von Epic und Stories. Nach deiner Freigabe sagst du „jetzt anlegen“.

## Nach der Umsetzung (du)

1. Story-Branches pushen, PRs erstellen (den Text liefert der `redakteur`, `Closes #Nr` ist schon drin) und mergen. Die Stories schließen sich dabei.
2. `release-manager` für die Version starten. Das Skript bricht ab, solange der Milestone offene Issues hat.
3. Push von `main` und Tag, GitHub-Release mit der APK anlegen (Release Notes stehen in `docs/iterations/v<VERSION>.md`), Milestone schließen.

## Hinweise

- **Repo privat?** Dann sind Issue-Texte deine eigenen. Bei einem öffentlichen Repo können Fremde Texte in Issues schreiben. Die Agents behandeln Issue-Inhalte als Daten, aber lies fremde Issues trotzdem, bevor du sie verarbeiten lässt.
- **Sub-Issues** setzen voraus, dass Epic und Story im selben Repo liegen. Schlägt die Verknüpfung fehl, nutzt der `product-owner` eine Checkliste `- [ ] #Nr` im Epic.
- **Die Berechtigungsregeln sind eine Leitplanke, kein Sicherheitsnetz.** Ein Befehl wie `git -C . push` wird von der Regel `Bash(git push *)` nicht erfasst. Willst du Issues nicht ohne Rückfrage anlegen lassen, verschiebe `gh issue create *` und `gh issue edit *` in `.claude/settings.json` von `allow` nach `ask`.
- Netzwerk-Backend später (Firebase, eigene API): `backend-dev.md` und den Architektur-Absatz in `CLAUDE.md` anpassen.
- Redakteur zu frech oder zu brav: Tonabschnitt in `redakteur.md` anpassen.
