# DSGVO-Compliance Checkliste

Dieses Dokument sichert die DSGVO-Konformität für das Cloud-Deployment von PVS.

## Datenschutz-Grundlagen

### 1. Datenverarbeitung in der EU

✅ **Render.com EU-Server**
- Render bietet EU-Rechenzentren (Frankfurt, Ireland)
- Configure in Render Dashboard → Service → "Region": Select "Frankfurt" or "Ireland"
- Vergewissere dich, dass alle Services EU-Region verwenden

### 2. Datenübertragung

✅ **Ende-zu-Ende Verschlüsselung**
- HTTPS ist Standard bei Render (automatische SSL-Zertifikate)
- Database Connections: PostgreSQL unterstützt SSL
- Environment Variable: `DATABASE_URL` enthält SSL-Parameter

✅ **API-Kommunikation**
- Alle externen APIs müssen HTTPS verwenden
- API Keys sind in Environment Variables gespeichert (nicht im Code)

### 3. Datenminimierung

✅ **Logging minimieren**
- Production Logging: `WARN` Level nur
- Keine sensiblen Daten in Logs (Passwörter, Gesundheitsdaten)
- Log Retention: Render speichert Logs 30 Tage

✅ **Database Schema**
- Nur notwendige Felder speichern
- Gesundheitsdaten nur nach Einwilligung

### 4. Zugriffskontrolle

✅ **Authentication & Authorization**
- Spring Security aktiviert
- User-basierte Zugriffsrechte
- Passwörter: BCrypt-Hashing (nicht im Klartext)

✅ **API Keys & Secrets**
- Alle Secrets in GitHub Secrets
- Render Environment Variables für Production
- Keine Secrets im Code oder Git

### 5. Datenlöschung

✅ **Right to be Forgotten**
- Patienten können gelöscht werden
- Audit Trail für Löschvorgänge (optional implementieren)
- Database Backups: Enthalten gelöschte Daten (7 Tage Retention bei Render)

**Hinweis**: Backup-Management muss DSGVO-konform erfolgen.

### 6. Datenintegrität

✅ **Database Backups**
- Render PostgreSQL: Automatische tägliche Backups
- Retention: 7 Tage (Starter Plan)
- Verschlüsselte Backups

✅ **Audit Trail**
- Flyway Migration History für Schema-Änderungen
- Optional: Application-Level Audit Logging implementieren

## Technische Maßnahmen

### Environment Configuration

```yaml
# application-prod.yaml
spring:
  jpa:
    show-sql: false  # Keine SQL-Queries in Logs
logging:
  level:
    root: WARN  # Minimale Logging
```

### Database Security

- PostgreSQL SSL Connections aktiviert
- Firewall Rules: Nur Render Services können auf DB zugreifen
- Separate DB-Credentials für Prod/Test

### API Security

- Spring Security: CSRF Protection aktiviert
- Session Timeout konfiguriert
- HTTPS only (Render Standard)

## Compliance Checklist

### Vor Production Go-Live

- [ ] EU-Region in Render konfiguriert (Frankfurt/Ireland)
- [ ] HTTPS aktiviert (automatisch bei Render)
- [ ] Database SSL Connections aktiviert
- [ ] Alle Secrets aus Code entfernt
- [ ] Logging auf WARN Level in Prod
- [ ] Backups konfiguriert und getestet
- [ ] DSGVO-konforme Datenschutzerklärung vorhanden
- [ ] Einwilligungsmanagement implementiert
- [ ] Löschfunktionen getestet
- [ ] Zugriffskontrollen validiert

### Regelmäßige Überprüfungen

- [ ] Quartalsweise: Review der Environment Variables
- [ ] Monatlich: Backup-Restore-Test
- [ ] Bei Änderungen: DSGVO-Impact Assessment
- [ ] Jährlich: Security Audit

## Externe Dienstleister

### Render.com

✅ **DSGVO-Konformität**
- Render ist GDPR-konform
- DPA (Data Processing Agreement) verfügbar
- EU-Rechenzentren verfügbar

**Action Required**: DPA mit Render abschließen (falls notwendig)

### GitHub (für CI/CD)

✅ **DSGVO-Konformität**
- GitHub bietet EU-Server
- Secrets werden verschlüsselt gespeichert

**Hinweis**: Repository sollte private sein für Produktionscode

## Datenkategorien

### Gesundheitsdaten (Art. 9 DSGVO)

- **Kategorie**: Besonders schützenswerte Daten
- **Rechtliche Grundlage**: Art. 9 Abs. 2 lit. h (Gesundheitsversorgung)
- **Maßnahmen**:
  - Verschlüsselte Übertragung
  - Zugriffskontrolle
  - Audit-Logging (empfohlen)

### Patienten-Stammdaten

- **Kategorie**: Personenbezogene Daten
- **Rechtliche Grundlage**: Art. 6 Abs. 1 lit. f (berechtigtes Interesse)
- **Maßnahmen**:
  - Datenminimierung
  - Löschfunktionen
  - Zugriffskontrolle

## Incident Response

### Data Breach (Datenleck)

Bei Verdacht auf Datenleck:

1. **Sofort**: Zugriff stoppen (Service stoppen, falls nötig)
2. **Innerhalb 72h**: Meldung an Aufsichtsbehörde
3. **Betroffene informieren**: Bei hohem Risiko für Patienten
4. **Dokumentation**: Incident loggen
5. **Root Cause**: Analysieren und beheben
6. **Preventive Measures**: Implementieren

### Notfall-Kontakte

- **Datenschutzbeauftragter**: [TODO: Kontakt eintragen]
- **Render Support**: support@render.com
- **Aufsichtsbehörde**: [TODO: Zuständige Behörde]

## Weitere Ressourcen

- [Render Privacy Policy](https://render.com/privacy)
- [Render Security](https://render.com/security)
- [GDPR Guide](https://gdpr.eu/)
- [BfDI](https://www.bfdi.bund.de/) (Bundesbeauftragter für Datenschutz)

