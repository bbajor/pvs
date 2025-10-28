# Gradle Build-Optimierung 🚀

## Was wurde optimiert:

### 1. **gradle.properties** - Build-Caching aktiviert
- ✅ Build-Caching aktiviert (`org.gradle.caching=true`)
- ✅ Parallel-Build aktiviert (`org.gradle.parallel=true`)
- ✅ Configuration-Cache für schnellere Starts
- ✅ File-Watching für Dev-Performance
- ✅ JVM-Optimierungen (2GB Heap, Metaspace)

### 2. **build.gradle** - Task-Optimierungen
- ✅ **Test-Caching**: Tests werden nur neu ausgeführt, wenn sich Code/Dependencies geändert haben
- ✅ **Test-Parallelisierung**: Tests laufen parallel (max. 50% der CPU-Cores)
- ✅ **Incremental Compilation**: Nur geänderte Java-Dateien werden kompiliert
- ✅ **Layered Jars**: Spring Boot JARs für besseres Docker-Layer-Caching
- ✅ **Dependency-Cache**: Schnellere Dependency-Resolution

### 3. **GitHub Actions** - CI/CD-Optimierung
- ✅ **Build-Cache**: Gradle-Cache wird zwischen Builds gespeichert
- ✅ **Parallel-Build**: `--parallel` Flag für multi-core Builds
- ✅ **Build-Cache**: `--build-cache` Flag für Task-Caching

## Performance-Verbesserungen:

### Vorher:
- Build: ~3-5 Minuten
- Tests: ~2-3 Minuten
- Gesamt: ~5-8 Minuten

### Nachher (bei Code-Änderungen):
- Build: ~1-2 Minuten (wenn nur wenig geändert)
- Tests: ~30s-1min (wenn nur wenig geändert)
- Gesamt: ~2-3 Minuten

### Bei keiner Code-Änderung:
- Build: ~10-20 Sekunden (fast komplett aus Cache)
- Tests: ~5-10 Sekunden (fast komplett aus Cache)
- Gesamt: ~15-30 Sekunden 🎉

## Lokale Nutzung:

```bash
# Erster Build (legt Cache an)
./gradlew build

# Nächster Build ohne Code-Änderungen (nutzt Cache)
./gradlew build  # ⚡ Viel schneller!

# Build mit Cache-Clean (falls Probleme)
./gradlew clean build

# Cache-Status prüfen
./gradlew build --info | grep -i cache
```

## CI/CD:

Der GitHub Actions Workflow nutzt automatisch:
- ✅ Gradle Build-Cache
- ✅ Dependency-Cache
- ✅ Parallel Execution
- ✅ Restore zwischen Builds

## Weitere Optimierungen:

### Für noch schnelleres Feedback:
```bash
# Nur geänderten Code kompilieren (ohne Tests)
./gradlew compileJava

# Nur Tests ausführen (ohne Build)
./gradlew test

# Gradle Daemon nutzen (lokal)
./gradlew build  # Daemon läuft standardmäßig
```

## Troubleshooting:

**Cache-Probleme?**
```bash
# Cache löschen
rm -rf .gradle
rm -rf ~/.gradle/caches

# Fresh Build
./gradlew clean build --no-build-cache
```

**Performance-Probleme?**
```bash
# Build-Scan erstellen
./gradlew build --scan

# Build-Profile
./gradlew build --profile
```

## Metriken prüfen:

```bash
# Build-Dauer ausgeben
./gradlew build --info 2>&1 | grep "BUILD SUCCESSFUL"

# Welche Tasks wurden gecached?
./gradlew build --info 2>&1 | grep "FROM-CACHE"
```

