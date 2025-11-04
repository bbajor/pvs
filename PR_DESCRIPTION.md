## Änderungen

### Settings-Restrukturierung
- Neue Tab-Reihenfolge: Allgemein → Standorte → Benutzerverwaltung → Medikamentendatenbank
- Neuer Tab "Allgemein": Bearbeitung von Institutions-Daten (Name, Adresse, Kontakt)
- Neuer Tab "Standorte": Verwaltung von Standorten (hinzufügen, bearbeiten, aktivieren/deaktivieren)
- Erweiterte Benutzerverwaltung: Location-Zuordnung zu Benutzern möglich
- Tab "Remote LLM" entfernt: Wird jetzt pro Institution konfiguriert
- Tab "Praxisverwaltung" entfernt: Wurde durch "Standorte" ersetzt

### Task-Filterung nach Institution
- Problem behoben: Task-Liste war leer, weil keine Institution-Filterung vorhanden war
- TaskRepository: Institution-basierte Queries hinzugefügt
- TaskService: Filterung nach InstitutionContext implementiert
- SurgicalCenterTimeSlotRepository: Institution-Filterung in Queries
- SurgicalCenterService: Institution-Filterung bei TimeSlot-Abfragen

### Details
- Ein Arzt kann jetzt Behandlungen von allen Locations seiner Institution prüfen
- Tasks werden nach Institution gefiltert (nicht nach Location)
- Nur Behandlungen der aktuellen Institution werden angezeigt

## Technische Details
- Neue Tab-Komponenten: `InstitutionGeneralTab`, `LocationManagementTab`
- Erweiterte `UserSettingsTab` mit Location-Zuordnung
- Institution-basierte Queries in `TaskRepository`, `TaskService`, `SurgicalCenterTimeSlotRepository`
