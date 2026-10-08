#!/usr/bin/env bash
# Hängt eine Story als Sub-Issue an ein Epic.
# Aufruf: scripts/link-sub-issue.sh <epic-nr> <story-nr>
# Exit-Codes: 0 = verknüpft, 1 = falsche Eingabe/Fehler beim Lesen, 2 = Verknüpfung fehlgeschlagen (Fallback nötig)
set -euo pipefail

EPIC="${1:?Aufruf: scripts/link-sub-issue.sh <epic-nr> <story-nr>}"
STORY="${2:?Aufruf: scripts/link-sub-issue.sh <epic-nr> <story-nr>}"

if ! [[ "$EPIC" =~ ^[0-9]+$ && "$STORY" =~ ^[0-9]+$ ]]; then
  echo "Issue-Nummern müssen Zahlen sein." >&2
  exit 1
fi

# Die API verlangt die numerische Issue-ID, nicht die #Nummer.
STORY_ID="$(gh api "repos/{owner}/{repo}/issues/${STORY}" --jq .id)"

if gh api -X POST "repos/{owner}/{repo}/issues/${EPIC}/sub_issues" -F "sub_issue_id=${STORY_ID}" >/dev/null; then
  echo "Story #${STORY} hängt jetzt an Epic #${EPIC}."
else
  echo "Verknüpfung fehlgeschlagen. Fallback: '- [ ] #${STORY}' in die Beschreibung von Epic #${EPIC} aufnehmen." >&2
  exit 2
fi
