# Cursor AI und nativer On-Premise-Betrieb

Diese Notiz beschreibt den empfohlenen Arbeitsmodus, damit IVOMPlaner schnell, aber kontrolliert produktiv auf einem On-Premise-System landet.

## Cursor AI sinnvoll nutzen

- **Ask Mode**: Code verstehen, Risiken klären, Architekturentscheidungen prüfen.
- **Plan Mode**: vor Deployment-, Security- oder Migrationsänderungen einen konkreten Plan erstellen lassen.
- **Agent Mode**: fokussierte Änderungen umsetzen, Tests ausführen, Commit/PR vorbereiten.
- **Cloud Agents**: parallele Aufgaben wie CI-Fixes, Doku oder Refactorings auslagern.
- **Bugbot/Review**: PRs zusätzlich auf Bugs, Security und fehlende Tests prüfen lassen.

Wichtig: Secrets bleiben außerhalb des Repos. `.env`-Dateien werden lokal auf dem Zielsystem gepflegt.

## Nativer On-Premise-Releasepfad

Der produktive On-Premise-Pfad nutzt keine Container-Runtime:

1. CI oder Entwickler baut das Release-Paket:
   ```bash
   bash onpremise/build-native-package.sh
   ```
2. Das Paket `build/distributions/ivomplaner-onpremise-<version>.tar.gz` wird als Release-Artefakt bereitgestellt.
3. Auf der Praxis-VM installiert `install.sh` Java 21, PostgreSQL, systemd-Service und Betriebsverzeichnis.
4. Updates laufen ueber `ivomplaner-update latest`, `ivomplaner-update <tarball>` oder fuer Super-Admins direkt aus der App.
5. Backups laufen ueber `ivomplaner-backup`; Restore ueber `ivomplaner-restore <dump>`.

## Betriebsprinzip

- Releases: `/opt/ivomplaner/releases/<version>`
- Aktive Version: `/opt/ivomplaner/current`
- Konfiguration: `/etc/ivomplaner/ivomplaner.env`
- Backups: `/opt/ivomplaner/backups`
- Service: `ivomplaner.service`
- Healthcheck: `http://127.0.0.1:8080/actuator/health`
- App-Update-View: `admin/system-update`, nur fuer `SUPER_ADMIN`.
- App-Update-Wrapper: `/usr/local/bin/ivomplaner-update-wrapper` startet das Update per `systemd-run`.

Der Server braucht damit Java 21, PostgreSQL, curl und systemd. Mehr Magie ist nicht vorgesehen; Magie debuggt sich schlecht.
