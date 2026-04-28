# Cloud Environment Variables

Diese Dokumentation listet alle erforderlichen und optionalen Environment-Variablen für das Cloud-Deployment auf.

## Erforderliche Variablen

### Datenbank
- `DATABASE_URL` - JDBC-URL für PostgreSQL (z.B. `jdbc:postgresql://localhost:5432/pvs_prod`)
- `DATABASE_USERNAME` - Datenbank-Benutzername
- `DATABASE_PASSWORD` - Datenbank-Passwort

### Secrets (Sicherheit)
- `SMTP_ENCRYPTION_KEY` - **ERFORDERLICH für Cloud/Production**: 32-Byte-Verschlüsselungsschlüssel für SMTP-Passwörter
  - Generierung: `openssl rand -base64 32 | head -c 32`
  - **WICHTIG**: Dieser Schlüssel muss sicher gespeichert werden. Bei Verlust können verschlüsselte SMTP-Passwörter nicht mehr entschlüsselt werden.

## Optionale Variablen

### Datenbank-Pool
- `DB_POOL_MAX_SIZE` - Maximale Connection-Pool-Größe (Standard: 20)
- `DB_POOL_MIN_IDLE` - Minimale idle Connections (Standard: 5)
- `DB_CONNECTION_TIMEOUT` - Connection-Timeout in ms (Standard: 30000)
- `DB_IDLE_TIMEOUT` - Idle-Timeout in ms (Standard: 600000)
- `DB_MAX_LIFETIME` - Maximale Connection-Lifetime in ms (Standard: 1800000)

### SMTP
- `SMTP_HOST` - SMTP-Server-Hostname
- `SMTP_PORT` - SMTP-Port (Standard: 587)
- `SMTP_USERNAME` - SMTP-Benutzername
- `SMTP_PASSWORD` - SMTP-Passwort (wird verschlüsselt in DB gespeichert)
- `SMTP_FROM_ADDRESS` - Absender-E-Mail-Adresse
- `SMTP_SECURITY_METHOD` - Sicherheitsmethode (STARTTLS, SSL_TLS, NONE)
- `SMTP_ENABLED` - SMTP aktivieren (true/false)

### MFA
- `MFA_ISSUER` - Issuer-Name für TOTP (Standard: PVS)

### AI/Whisper
- `WHISPER_LOCAL_ENABLED` - Lokalen Whisper-Container aktivieren (Standard: false)
- `WHISPER_HOST` - Whisper-Container-Host (Standard: localhost)
- `WHISPER_PORT` - Whisper-Container-Port (Standard: 9000)
- `WHISPER_REMOTE_ENABLED` - Remote-Provider aktivieren (Standard: true)
- `WHISPER_PROVIDER` - Remote-Provider (Standard: aleph-alpha)
- `WHISPER_API_URL` - API-URL für Remote-Provider
- `AI_API_KEY` - API-Key für Remote-Provider (z.B. Aleph Alpha)
- `WHISPER_MONTHLY_QUOTA` - Monatliches Quota (Standard: 1000)
- `AI_CONFIDENCE_THRESHOLD` - Confidence-Threshold für Extraktion (Standard: 0.7)

### Server
- `PORT` - Server-Port (Standard: 8080)
- `SESSION_TIMEOUT` - Session-Timeout (Standard: 30m)
- `SESSION_COOKIE_SECURE` - Secure-Cookie aktivieren (Standard: true)

### Logging
- `LOG_FILE` - Log-Datei-Pfad (Standard: /var/log/pvs/application.log)
- `LOG_MAX_SIZE` - Maximale Log-Dateigröße (Standard: 100MB)
- `LOG_MAX_HISTORY` - Anzahl der Log-Dateien (Standard: 30)

### Monitoring
- `ENVIRONMENT` - Environment-Name für Metrics-Tags (Standard: cloud)

## Beispiel-Konfiguration

```bash
# Datenbank
export DATABASE_URL="jdbc:postgresql://db.example.com:5432/pvs_prod"
export DATABASE_USERNAME="pvs_user"
export DATABASE_PASSWORD="secure_password"

# Secrets (ERFORDERLICH)
export SMTP_ENCRYPTION_KEY="$(openssl rand -base64 32 | head -c 32)"

# SMTP
export SMTP_HOST="smtp.example.com"
export SMTP_PORT="587"
export SMTP_USERNAME="noreply@example.com"
export SMTP_PASSWORD="smtp_password"
export SMTP_FROM_ADDRESS="noreply@example.com"

# AI
export AI_API_KEY="your_api_key_here"
export WHISPER_REMOTE_ENABLED="true"

# Server
export PORT="8080"
export SESSION_TIMEOUT="30m"
```

## Rotation-Strategie für Secrets

### SMTP_ENCRYPTION_KEY

**WICHTIG**: Der Verschlüsselungsschlüssel sollte regelmäßig rotiert werden, aber dies erfordert eine Migration:

1. **Vorbereitung**: Neuen Schlüssel generieren
2. **Migration**: Alle verschlüsselten Daten mit neuem Schlüssel neu verschlüsseln
3. **Update**: Environment-Variable aktualisieren
4. **Verifikation**: Testen, dass alle verschlüsselten Daten noch funktionieren

**Empfehlung**: Rotation alle 6-12 Monate oder bei Sicherheitsvorfällen.

## Sicherheitshinweise

- **Niemals Secrets in Code, Commits oder Logs speichern**
- **Verwende Secrets-Management-Tools** (z.B. Hetzner Secrets, GitHub Secrets)
- **Regelmäßige Rotation** von Passwörtern und Verschlüsselungsschlüsseln
- **Minimale Berechtigungen** für Datenbank- und Service-Accounts
- **Audit-Logging** für alle Zugriffe auf sensible Daten


