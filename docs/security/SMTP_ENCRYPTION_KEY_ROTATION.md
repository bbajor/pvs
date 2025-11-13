# SMTP Verschlüsselungsschlüssel - Rotation-Strategie

## Übersicht

Die Rotation des `SMTP_ENCRYPTION_KEY` ist wichtig für die Sicherheit, erfordert aber sorgfältige Planung, da alle verschlüsselten SMTP-Passwörter mit dem neuen Schlüssel neu verschlüsselt werden müssen.

## Wann rotieren?

### Regelmäßige Rotation

- **Empfohlen**: Alle **6-12 Monate** (abhängig von Sicherheitsanforderungen)
- **Bei Sicherheitsvorfällen**: Sofort rotieren, wenn der Schlüssel kompromittiert wurde
- **Nach Personalwechsel**: Wenn Personen mit Zugriff auf den Schlüssel das Unternehmen verlassen

### Indikatoren für sofortige Rotation

- Verdacht auf Kompromittierung
- Unbefugter Zugriff auf den Server/Container
- Schlüssel wurde versehentlich in Logs/Code committet
- Sicherheitsaudit empfiehlt Rotation

## Rotations-Prozess

### Vorbereitung

1. **Neuen Schlüssel generieren**:
   ```bash
   # AES-256 (32 Bytes) - empfohlen
   openssl rand -base64 32
   ```

2. **Backup des alten Schlüssels** (für Migration):
   - Sicher aufbewahren (verschlüsselt!)
   - Nur für Migration benötigt, danach sicher löschen

3. **Wartungsfenster planen**:
   - SMTP-Funktionalität ist während der Rotation nicht verfügbar
   - Benutzer informieren (falls E-Mail-Versand betroffen)

### Migration (Zero-Downtime)

#### Schritt 1: Dual-Key Support (optional, für Zero-Downtime)

Erweitere `SmtpConfigService` um Unterstützung für zwei Schlüssel:

```java
@Value("${smtp.encryption.old-key:${SMTP_ENCRYPTION_KEY_OLD:}}")
private String oldEncryptionKey;

// Versuche zuerst mit neuem Schlüssel, dann mit altem
public String decryptPassword(String encryptedPassword) {
    try {
        return decryptWithKey(encryptedPassword, secretKey);
    } catch (Exception e) {
        if (oldSecretKey != null) {
            return decryptWithKey(encryptedPassword, oldSecretKey);
        }
        throw e;
    }
}
```

#### Schritt 2: Migration durchführen

1. **Neuen Schlüssel setzen** (neben altem):
   ```bash
   export SMTP_ENCRYPTION_KEY_OLD="alter-schluessel"
   export SMTP_ENCRYPTION_KEY="neuer-schluessel"
   ```

2. **Alle verschlüsselten Passwörter migrieren**:
   - Lese alle verschlüsselten SMTP-Passwörter aus der Datenbank
   - Entschlüssele mit altem Schlüssel
   - Verschlüssele mit neuem Schlüssel
   - Speichere zurück in Datenbank

3. **Dual-Key Support entfernen** (nach erfolgreicher Migration):
   - Entferne `SMTP_ENCRYPTION_KEY_OLD`
   - Entferne Code für alten Schlüssel

### Einfache Migration (mit Downtime)

1. **Wartungsfenster einplanen**
2. **Neuen Schlüssel setzen**:
   ```bash
   export SMTP_ENCRYPTION_KEY="neuer-schluessel"
   ```
3. **Alle SMTP-Passwörter neu eingeben**:
   - Benutzer müssen ihre SMTP-Passwörter neu eingeben
   - System verschlüsselt automatisch mit neuem Schlüssel
4. **Application neu starten**

## Migration-Script (Beispiel)

```bash
#!/bin/bash
# migrate-smtp-keys.sh

OLD_KEY="${SMTP_ENCRYPTION_KEY_OLD}"
NEW_KEY="${SMTP_ENCRYPTION_KEY}"

if [ -z "$OLD_KEY" ] || [ -z "$NEW_KEY" ]; then
    echo "Fehler: SMTP_ENCRYPTION_KEY_OLD und SMTP_ENCRYPTION_KEY müssen gesetzt sein"
    exit 1
fi

# Beispiel: Alle verschlüsselten Passwörter aus DB lesen und migrieren
# (Anpassen an deine Datenbank-Struktur!)

# 1. Lese verschlüsselte Passwörter
# 2. Entschlüssele mit OLD_KEY
# 3. Verschlüssele mit NEW_KEY
# 4. Speichere zurück

echo "Migration abgeschlossen. Alten Schlüssel jetzt entfernen!"
```

## Nach der Rotation

### Cleanup

1. **Alten Schlüssel entfernen**:
   - Aus Environment-Variablen entfernen
   - Aus Secrets-Management-Systemen entfernen
   - Aus Backups entfernen (nach Bestätigung, dass Migration erfolgreich war)

2. **Dokumentation aktualisieren**:
   - Rotationsdatum dokumentieren
   - Nächstes Rotationsdatum planen

3. **Verifizierung**:
   - SMTP-Funktionalität testen
   - Verschlüsselte Passwörter können entschlüsselt werden
   - Keine Fehler in Logs

## Best Practices

### Schlüssel-Generierung

- **Zufällig**: Nutze kryptographisch sichere Zufallsgeneratoren
- **Länge**: Mindestens 32 Bytes (AES-256)
- **Keine Patterns**: Keine vorhersagbaren Muster

### Schlüssel-Verwaltung

- **Secrets-Management**: Nutze Tools wie:
  - HashiCorp Vault
  - AWS Secrets Manager
  - Azure Key Vault
  - Kubernetes Secrets
- **Zugriff beschränken**: Nur autorisierte Personen
- **Audit-Logging**: Wer hat wann Zugriff?

### Dokumentation

- **Rotations-Historie**: Wann wurde rotiert?
- **Nächste Rotation**: Wann ist die nächste geplant?
- **Verantwortliche**: Wer ist für die Rotation zuständig?

## Notfall-Plan

### Wenn Migration fehlschlägt

1. **Rollback**: Setze alten Schlüssel zurück
   ```bash
   export SMTP_ENCRYPTION_KEY="alter-schluessel"
   ```
2. **Application neu starten**
3. **Problem analysieren**: Warum ist Migration fehlgeschlagen?
4. **Plan anpassen**: Migration-Plan überarbeiten
5. **Erneut versuchen**: Nach Behebung des Problems

### Wenn alter Schlüssel verloren geht

⚠️ **KRITISCH**: Alle mit dem alten Schlüssel verschlüsselten Passwörter sind **nicht mehr entschlüsselbar**!

**Lösung**:
- Alle SMTP-Passwörter müssen **neu eingegeben** werden
- Keine automatische Migration möglich
- Benutzer müssen ihre Passwörter manuell neu konfigurieren

**Prävention**:
- Alte Schlüssel sicher aufbewahren (verschlüsselt!)
- Backup-Strategie für Schlüssel
- Dokumentation, wo Schlüssel gespeichert sind

## Checkliste

- [ ] Neuen Schlüssel generiert (32 Bytes, zufällig)
- [ ] Backup des alten Schlüssels erstellt (verschlüsselt)
- [ ] Wartungsfenster geplant / Zero-Downtime-Strategie vorbereitet
- [ ] Migration-Script getestet (in Test-Umgebung)
- [ ] Benutzer informiert (falls Downtime)
- [ ] Neuen Schlüssel in Environment-Variablen gesetzt
- [ ] Migration durchgeführt
- [ ] SMTP-Funktionalität getestet
- [ ] Alten Schlüssel entfernt (nach erfolgreicher Migration)
- [ ] Dokumentation aktualisiert
- [ ] Nächstes Rotationsdatum geplant

## Code-Referenz

- Service: `de.bbajor.pvs.security.email.service.SmtpConfigService`
- Config: `src/main/resources/application*.yaml`
