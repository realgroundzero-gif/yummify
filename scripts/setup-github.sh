#!/usr/bin/env bash
# Richtet Labels und (optional) einen Milestone im aktuellen GitHub-Repo ein. Idempotent.
# Aufruf: scripts/setup-github.sh            -> nur Labels
#         scripts/setup-github.sh 0.2.0      -> Labels + Milestone "v0.2.0"
set -euo pipefail

gh auth status >/dev/null 2>&1 || { echo "Nicht bei GitHub angemeldet: gh auth login" >&2; exit 1; }

label() { gh label create "$1" --color "$2" --description "$3" --force >/dev/null; echo "Label: $1"; }

label epic        6F42C1 "Großes Feature, bündelt Stories"
label story       0E8A16 "User Story mit Akzeptanzkriterien"
label bug         D73A4A "Fehler"
label prio:must   B60205 "MoSCoW: Must have"
label prio:should D93F0B "MoSCoW: Should have"
label prio:could  FBCA04 "MoSCoW: Could have"
label prio:wont   CFD3D7 "MoSCoW: Won't have (diesmal nicht)"

if [ -n "${1:-}" ]; then
  VERSION="$1"
  if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Version muss im Format X.Y.Z sein, bekommen: $VERSION" >&2
    exit 1
  fi
  TITLE="v${VERSION}"
  if gh api "repos/{owner}/{repo}/milestones?state=all&per_page=100" --paginate --jq '.[].title' | grep -Fxq "$TITLE"; then
    echo "Milestone $TITLE existiert bereits."
  else
    gh api "repos/{owner}/{repo}/milestones" -f title="$TITLE" -f description="Iteration ${VERSION}" >/dev/null
    echo "Milestone $TITLE angelegt."
  fi
fi
