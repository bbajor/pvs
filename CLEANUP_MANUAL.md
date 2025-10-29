# Manuelle Git-Historie Bereinigung

## Schritt 1: Backup erstellen
```bash
git stash push -m "backup-before-cleanup-$(date +%Y%m%d-%H%M%S)"
```

## Schritt 2: Filter-Branch ausführen
Entfernt die Dateien mit dem SSH-Key aus der kompletten Historie:

```bash
git filter-branch --force --index-filter \
    "git rm --cached --ignore-unmatch GITHUB_SECRETS_EINFACH.md GITHUB_SECRETS.md || true" \
    --prune-empty --tag-name-filter cat -- --all
```

## Schritt 3: Cleanup der Git-Referenzen
```bash
git for-each-ref --format="delete %(refname)" refs/original | git update-ref --stdin
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

## Schritt 4: Force-Push (⚠️ WICHTIG)
Da die Historie umgeschrieben wurde, muss ein Force-Push gemacht werden:

```bash
# Zuerst prüfen, welche Remote-Repositories es gibt
git remote -v

# Dann Force-Push (ACHTUNG: Überschreibt die Remote-Historie!)
git push origin --force --all
git push origin --force --tags
```

## ⚠️ Warnungen
- Force-Push überschreibt die Remote-Historie
- Team-Mitglieder müssen ihre lokalen Repositories neu klonen oder rebasen
- Erstelle vorher ein Backup!

