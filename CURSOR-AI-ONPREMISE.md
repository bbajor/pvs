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
2. Das Paket `build/onpremise/ivomplaner-onpremise.tar.gz` wird auf den Server kopiert.
3. Auf dem Server installiert `onpremise/install.sh` Java 21, PostgreSQL, systemd-Service und Betriebsverzeichnis.
4. Updates laufen ueber `/opt/pvs/update.sh <jar-or-url>`.
5. Backups laufen ueber `/opt/pvs/backup.sh`; Restore ueber `/opt/pvs/restore.sh <dump>`.

## Betriebsprinzip

- Anwendung: `/opt/pvs/app/pvs-app.jar`
- Konfiguration: `/opt/pvs/.env`
- Backups: `/opt/pvs/backups`
- Service: `pvs-onpremise.service`
- Healthcheck: `http://127.0.0.1:8080/actuator/health`

Der Server braucht damit Java 21, PostgreSQL, curl und systemd. Mehr Magie ist nicht vorgesehen; Magie debuggt sich schlecht.
