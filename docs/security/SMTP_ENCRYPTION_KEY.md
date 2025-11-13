# SMTP Verschlüsselungsschlüssel - Secrets Management

## Übersicht

Der `SMTP_ENCRYPTION_KEY` wird zur Verschlüsselung von SMTP-Passwörtern verwendet. Der Schlüssel **darf niemals im Code hardcodiert sein** und muss ausschließlich über Environment-Variablen bereitgestellt werden.

## Konfiguration

### Environment-Variable

```bash
SMTP_ENCRYPTION_KEY=<dein-verschluesselungsschluessel>
```

### Anforderungen

- **Länge**: Muss genau **16, 24 oder 32 Bytes** lang sein (AES-128, AES-192 oder AES-256)
- **Format**: UTF-8 String
- **Sicherheit**: 
  - Mindestens 32 Zeichen für AES-256 (empfohlen)
  - Zufällig generiert, nicht vorhersagbar
  - Keine Wörter aus Wörterbüchern
  - Kombination aus Groß-/Kleinbuchstaben, Zahlen und Sonderzeichen

### Beispiel-Generierung

```bash
# Linux/macOS: 32 Bytes (AES-256)
openssl rand -base64 32

# Alternative: 32 zufällige Zeichen
tr -dc 'A-Za-z0-9!@#$%^&*' < /dev/urandom | head -c 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

## Verwendung in verschiedenen Umgebungen

### Development (`application-dev.yaml`)

```yaml
smtp:
  encryption:
    key: ${SMTP_ENCRYPTION_KEY:}
```

- Optional: Kann leer bleiben, wenn SMTP in Dev nicht benötigt wird
- **Warnung**: Wenn gesetzt, sollte ein separater Test-Schlüssel verwendet werden (nicht der Production-Schlüssel!)

### Test (`application-test.yaml`)

```yaml
smtp:
  encryption:
    key: ${SMTP_ENCRYPTION_KEY:}
```

- Optional: Kann leer bleiben für Tests ohne SMTP
- Empfohlen: Separater Test-Schlüssel für Test-Umgebungen

### Production (`application-prod.yaml`)

```yaml
smtp:
  encryption:
    key: ${SMTP_ENCRYPTION_KEY}
```

- **KRITISCH**: MUSS gesetzt sein (kein Default-Wert!)
- Application startet nicht, wenn `SMTP_ENCRYPTION_KEY` fehlt
- Nur über Environment-Variable, niemals hardcodiert!

## Deployment

### Docker / Docker Compose

```yaml
environment:
  - SMTP_ENCRYPTION_KEY=${SMTP_ENCRYPTION_KEY}
```

Oder in `.env` Datei (nicht committen!):

```bash
SMTP_ENCRYPTION_KEY=dein-verschluesselungsschluessel-hier
```

### Kubernetes

```yaml
env:
  - name: SMTP_ENCRYPTION_KEY
    valueFrom:
      secretKeyRef:
        name: smtp-secrets
        key: encryption-key
```

### Systemd Service

```ini
[Service]
Environment="SMTP_ENCRYPTION_KEY=dein-verschluesselungsschluessel"
```

## Rotation-Strategie

Siehe [SMTP_ENCRYPTION_KEY_ROTATION.md](./SMTP_ENCRYPTION_KEY_ROTATION.md) für Details zur Schlüssel-Rotation.

## Sicherheitshinweise

⚠️ **KRITISCH**:

1. **Niemals im Code committen**: Der Schlüssel darf nicht in Git-Repositories landen
2. **Separate Schlüssel pro Umgebung**: Dev, Test und Prod sollten unterschiedliche Schlüssel verwenden
3. **Zugriff beschränken**: Nur autorisierte Personen sollten Zugriff auf den Production-Schlüssel haben
4. **Logging vermeiden**: Der Schlüssel wird nie in Logs ausgegeben
5. **Backup sicher aufbewahren**: Verschlüsselte Passwörter können nur mit dem ursprünglichen Schlüssel entschlüsselt werden

## Fehlerbehebung

### "SMTP_ENCRYPTION_KEY muss als Environment-Variable gesetzt sein"

**Problem**: Die Environment-Variable ist nicht gesetzt.

**Lösung**: 
```bash
export SMTP_ENCRYPTION_KEY="dein-schluessel"
# Oder in .env Datei setzen (nicht committen!)
```

### "SMTP_ENCRYPTION_KEY muss 16, 24 oder 32 Bytes lang sein"

**Problem**: Der Schlüssel hat die falsche Länge.

**Lösung**: 
- Prüfe die Länge: `echo -n "$SMTP_ENCRYPTION_KEY" | wc -c`
- Generiere neuen Schlüssel mit korrekter Länge (siehe Beispiel-Generierung oben)

### "Fehler beim Entschlüsseln des SMTP-Passworts"

**Problem**: Der Verschlüsselungsschlüssel stimmt nicht mit dem überein, der zum Verschlüsseln verwendet wurde.

**Lösung**: 
- Stelle sicher, dass derselbe Schlüssel verwendet wird, der zum Verschlüsseln verwendet wurde
- Bei Schlüssel-Rotation: Alle verschlüsselten Passwörter müssen neu verschlüsselt werden

## Code-Referenz

- Service: `de.bbajor.pvs.security.email.service.SmtpConfigService`
- Config: `src/main/resources/application*.yaml`
