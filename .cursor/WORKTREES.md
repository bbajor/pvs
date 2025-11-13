# Worktree-Konfiguration für Multi-Agent-Entwicklung

Diese Datei dokumentiert wichtige Einstellungen und Best Practices für die Arbeit mit mehreren Agenten parallel auf Git Worktrees.

## Setup-Commands

Die Worktree-Config führt automatisch `./gradlew build --no-daemon` aus, was:
- ✅ Alle Dependencies lädt
- ✅ Vaadin Frontend vorbereitet (inkl. automatischem `npm install`)
- ✅ Projekt kompiliert
- ✅ `--no-daemon` verhindert Konflikte zwischen mehreren Worktrees

## Wichtige Einstellungen für parallele Worktrees

### 1. Server-Ports

**Problem**: Standard-Port 8080 würde bei mehreren Instanzen kollidieren.

**Lösung**: Setze unterschiedliche Ports pro Worktree via Environment-Variable:

```bash
# Worktree 1
PORT=8081 ./gradlew bootRun

# Worktree 2
PORT=8082 ./gradlew bootRun

# Worktree 3
PORT=8083 ./gradlew bootRun
```

Oder in der IDE (IntelliJ/VSCode) als Run-Configuration:
- Environment Variables: `PORT=8081`

### 2. Whisper-Service Port

**Problem**: Whisper-Service läuft standardmäßig auf Port 9000.

**Lösung**: 
- Option A: Ein zentraler Whisper-Service für alle Worktrees (empfohlen)
  ```bash
  # Einmalig starten (z.B. in Haupt-Worktree)
  podman-compose -f podman-compose.dev.yml up whisper -d
  ```
- Option B: Pro Worktree eigenen Whisper mit unterschiedlichen Ports (wenn nötig)

### 3. Gradle Daemon

**Wichtig**: `--no-daemon` ist bereits in der Config enthalten. Dies verhindert:
- Konflikte zwischen mehreren Gradle-Daemons
- Lock-Probleme bei gleichzeitigen Builds
- Speicher-Probleme durch mehrere Daemon-Instanzen

### 4. Gradle Cache

**Status**: Gradle-Cache (`~/.gradle/caches`) wird zwischen Worktrees geteilt.

**Vorteil**: Schnellere Builds durch Wiederverwendung von Dependencies

**Nachteil**: Bei gleichzeitigen Builds können theoretisch Konflikte auftreten (selten)

**Lösung**: Falls Probleme auftreten, temporär mit `--no-build-cache` arbeiten.

### 5. Build-Verzeichnisse

**Isoliert pro Worktree**:
- `build/` (Gradle)
- `target/` (Maven, falls verwendet)
- `node_modules/` (npm)
- `.gradle/` (lokaler Gradle-Cache)

**Geteilt**:
- `~/.gradle/caches/` (globaler Gradle-Cache)

### 6. Datenbank-Isolation

**H2 in-memory**: Pro Prozess isoliert - perfekt für parallele Tests ✅

**PostgreSQL**: Bei lokaler PostgreSQL-Datenbank auf unterschiedliche Datenbanken/Ports achten.

### 7. Branch-Strategie

**Wichtig**: Jeder Agent sollte einen eigenen Branch verwenden:
- Pattern: `cursor/<agent>/<topic>`
- Beispiel: `cursor/agent1/feature-x`, `cursor/agent2/feature-y`

**Nicht möglich**: Derselbe Branch in mehreren Worktrees gleichzeitig.

## Best Practices

1. ✅ **Eigener Branch pro Agent**: `cursor/<agent>/<topic>`
2. ✅ **Unterschiedliche Ports**: PORT=8081, PORT=8082, etc.
3. ✅ **Zentraler Whisper-Service**: Einmal starten, alle Worktrees nutzen ihn
4. ✅ **Regelmäßig synchronisieren**: Push/Pull um Merge-Konflikte zu minimieren
5. ✅ **Gradle ohne Daemon**: Bereits in Config enthalten
6. ✅ **Isolierte Build-Verzeichnisse**: Automatisch durch Worktrees

## Troubleshooting

### Port bereits belegt
```bash
# Windows: Port prüfen
netstat -ano | findstr :8080

# Port freigeben oder anderen Port verwenden
PORT=8081 ./gradlew bootRun
```

### Gradle-Build hängt
- Prüfe ob andere Gradle-Prozesse laufen
- `--no-daemon` sollte das verhindern
- Falls nötig: `./gradlew --stop` im Haupt-Worktree

### Frontend-Build-Probleme
- Vaadin führt `npm install` automatisch aus
- Falls Probleme: `./gradlew clean vaadinPrepareFrontend`

### Merge-Konflikte
- Regelmäßig `git pull origin dev` ausführen
- Konflikte früh lösen, nicht aufschieben

