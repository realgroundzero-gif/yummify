#!/usr/bin/env bash
# Baut und versioniert eine Release-APK für Yummify.
# Aufruf: scripts/release.sh 0.3.0
# Optional: RELEASE_BRANCH=main (Standard), SKIP_GH_CHECK=1 (Milestone-Prüfung überspringen)
set -euo pipefail

VERSION="${1:?Aufruf: scripts/release.sh 0.3.0}"

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version muss im Format X.Y.Z sein, bekommen: $VERSION" >&2
  exit 1
fi

if git rev-parse "v${VERSION}" >/dev/null 2>&1; then
  echo "Tag v${VERSION} existiert bereits. Abbruch." >&2
  exit 1
fi

BRANCH="${RELEASE_BRANCH:-main}"
if [ "$(git rev-parse --abbrev-ref HEAD)" != "$BRANCH" ]; then
  echo "Release nur vom Branch '$BRANCH' aus (aktuell: $(git rev-parse --abbrev-ref HEAD))." >&2
  exit 1
fi

for var in YUMMIFY_KEYSTORE YUMMIFY_STORE_PW YUMMIFY_KEY_ALIAS YUMMIFY_KEY_PW; do
  if [ -z "${!var:-}" ]; then
    echo "Umgebungsvariable $var fehlt (Signing). Abbruch." >&2
    exit 1
  fi
done

git diff --quiet && git diff --cached --quiet || { echo "Working Tree nicht sauber." >&2; exit 1; }

# Milestone v<VERSION> muss existieren, geschlossene Issues enthalten und darf keine offenen haben.
if [ "${SKIP_GH_CHECK:-0}" != "1" ]; then
  command -v gh >/dev/null || { echo "gh-CLI fehlt (oder SKIP_GH_CHECK=1 setzen)." >&2; exit 1; }
  OPEN="$(gh issue list --milestone "v${VERSION}" --state open --json number,title --jq '.[] | "#\(.number) \(.title)"')" \
    || { echo "Milestone v${VERSION} nicht lesbar (existiert er? gh auth status?)." >&2; exit 1; }
  if [ -n "$OPEN" ]; then
    echo "Milestone v${VERSION} hat offene Issues:" >&2
    echo "$OPEN" >&2
    exit 1
  fi
  CLOSED="$(gh issue list --milestone "v${VERSION}" --state closed --json number --jq length)"
  if [ "${CLOSED:-0}" -eq 0 ]; then
    echo "Milestone v${VERSION} enthält keine geschlossenen Issues. Abbruch." >&2
    exit 1
  fi
fi

./gradlew clean testDebugUnitTest lint

git tag -a "v${VERSION}" -m "Iteration ${VERSION}"
trap 'git tag -d "v${VERSION}"' ERR   # Tag zurückrollen, wenn der Build scheitert

./gradlew assembleRelease

mkdir -p releases
cp app/build/outputs/apk/release/app-release.apk "releases/yummify-v${VERSION}.apk"
if command -v sha256sum >/dev/null; then SHA256=(sha256sum); else SHA256=(shasum -a 256); fi   # macOS hat kein sha256sum
"${SHA256[@]}" "releases/yummify-v${VERSION}.apk" | tee "releases/yummify-v${VERSION}.apk.sha256"

echo "Fertig. Zum Veröffentlichen (nur nach Freigabe): git push && git push origin v${VERSION}"
echo "Danach: GitHub-Release mit der APK anlegen und den Milestone v${VERSION} schließen."
