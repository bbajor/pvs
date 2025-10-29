# Build-Optimierungen für schnellere CI/CD

Dieses Dokument beschreibt die Optimierungen, die für schnellere Builds in CI/CD implementiert wurden.

## 🚀 Implementierte Optimierungen

### 1. Skip-Build bei non-code Änderungen

**Problem:** Jede Änderung (auch Docs, Configs) triggerte vollständigen Build.

**Lösung:** 
- Prüfung ob Java/Gradle/Config-Dateien geändert wurden
- Skip Docker-Build wenn nur Docs/READMEs geändert wurden
- Ersparnis: ~5-10 Minuten pro non-code Push

**Dateien:**
- `.github/workflows/deploy-dev.yml` - `check-changes` Step

### 2. Tests nur einmal ausführen

**Problem:** Tests wurden doppelt ausgeführt (`build` + `test`).

**Lösung:**
- `./gradlew build -x test` - Build ohne Tests
- `./gradlew test` - Tests separat (kann bei Dev übersprungen werden)
- Ersparnis: ~2-3 Minuten

**Dateien:**
- `.github/workflows/deploy-dev.yml` - Optimierte Build-Steps

### 3. Gradle Build-Cache

**Problem:** Abhängigkeiten wurden immer neu heruntergeladen.

**Lösung:**
- `--build-cache` Flag aktiviert
- `--parallel` für parallele Ausführung
- GitHub Actions nutzt Gradle Cache
- Ersparnis: ~1-2 Minuten

**Dateien:**
- `build.gradle` - Cache-Konfiguration
- `gradle.properties` - Cache-Einstellungen

### 4. Docker Layer-Caching

**Problem:** Docker-Image wurde jedes Mal komplett neu gebaut.

**Lösung:**
- GitHub Actions Cache (`cache-from: type=gha`)
- Multi-Stage Build mit optimiertem Caching
- Dependencies-Layer wird nur bei Änderungen neu gebaut
- Ersparnis: ~3-5 Minuten bei cached Builds

**Dateien:**
- `Dockerfile` - Multi-Stage mit Caching
- `.github/workflows/deploy-dev.yml` - GHA Cache-Konfiguration

### 5. BootJar Cache-Optimierung

**Problem:** BootJar wurde immer neu gebaut, auch bei unverändertem Code.

**Lösung:**
- Cache-Output für inkrementelle Builds
- Input-Tracking für Classes und Resources
- Nur neu bauen wenn tatsächlich Änderungen
- Ersparnis: ~1-2 Minuten bei unverändertem Code

**Dateien:**
- `build.gradle` - BootJar Task-Optimierung

## 📊 Erwartete Build-Zeiten

### Vorher:
- **Code-Änderung:** ~15-20 Minuten
- **Non-code Änderung:** ~15-20 Minuten (unnötig!)

### Nachher:
- **Code-Änderung:** ~8-12 Minuten (mit Cache: ~5-8 Minuten)
- **Non-code Änderung:** ~1-2 Minuten (nur Workflow, kein Build)

## ⚙️ Weitere Optimierungs-Möglichkeiten

### Option 1: Conditional Build (nur bei relevanten Änderungen)

```yaml
# In Workflow:
- name: Check changed paths
  uses: dorny/paths-filter@v2
  id: changes
  with:
    filters: |
      code:
        - '**/*.java'
        - 'build.gradle'
        - 'src/**'
      skip:
        - 'docs/**'
        - 'README*.md'
```

### Option 2: Matrix Builds (nur wenn nötig)

Für größere Projekte könnten Matrix-Builds sinnvoll sein, aber aktuell nicht nötig.

### Option 3: Test-Parallelisierung

Bereits implementiert in `build.gradle`:
```gradle
maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
```

### Option 4: GraalVM Native Builds (zukünftig)

Für noch schnellere Container-Starts, aber komplexer im Setup.

## 🔧 Lokale Build-Optimierungen

### Schnelles Testen ohne Build:

```bash
# Nur kompilieren, keine Tests:
./gradlew compileJava --no-daemon

# Nur Tests (wenn bereits kompiliert):
./gradlew test --no-daemon

# BootJar ohne Tests:
./gradlew bootJar -x test --no-daemon
```

### Build-Cache prüfen:

```bash
# Gradle Cache-Info:
./gradlew build --info | grep cache

# Docker Cache-Info:
docker buildx du
```

## ⚠️ Wichtige Hinweise

1. **Tests trotzdem ausführen:**
   - Tests werden bei Code-Änderungen IMMER ausgeführt
   - Nur bei non-code Änderungen wird gebuilded
   
2. **Cache-Invalidierung:**
   - Caches werden automatisch invalidiert bei Dependency-Änderungen
   - Manuell: `./gradlew clean build`

3. **Production-Builds:**
   - Diese Optimierungen gelten für `dev` Branch
   - `test` und `master` Branches führen immer vollständige Builds aus

## 📈 Monitoring

Um Build-Zeiten zu überwachen:

1. **GitHub Actions:**
   - Actions Tab → Workflow → Dauer prüfen
   
2. **Gradle Build Scan:**
   ```bash
   ./gradlew build --scan
   ```

3. **Docker Build-Zeiten:**
   - In GitHub Actions Logs nach "Duration" suchen

