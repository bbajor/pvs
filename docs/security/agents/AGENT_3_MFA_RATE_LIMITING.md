# Agent 3: Multi-Factor Authentication & Rate Limiting

**Branch:** `feature/security-auth-mfa`  
**Priorität:** 🟡 Medium-High  
**Start:** Nach Agent 2 gemergt  
**Geschätzte Dauer:** 3-4 Tage  

---

## 🎯 Mission

Multi-Factor Authentication (TOTP), Rate Limiting, Brute-Force Protection.

---

## 📋 Tasks

### 1. Multi-Factor Authentication (TOTP)

#### Database Schema
- [ ] **User Entity erweitern**
  - `mfaEnabled` (Boolean) - MFA aktiviert?
  - `mfaSecret` (String, verschlüsselt) - TOTP Secret
  - `backupCodes` (String, JSON Array, gehashed) - Backup-Codes
  - `mfaSetupCompleted` (Boolean) - Setup abgeschlossen?
  - Migration erstellen (Flyway/Liquibase)

#### TOTP-Service
- [ ] **TotpService implementieren**
  - Secret-Generierung (Base32)
  - QR-Code-URL generieren
  - TOTP-Token validieren (Time-window: 30s, Drift: 1)
  - Backup-Codes generieren (10 Codes, gehashed mit BCrypt)
  - Backup-Code validieren und deaktivieren

#### Authentication Flow
- [ ] **MFA-Authentication-Provider**
  - Spring Security Custom AuthenticationProvider
  - Zwei-Stufen-Authentifizierung
    1. Username/Password
    2. TOTP-Token
  - Session-State für MFA-Pending

- [ ] **Security Filter Chain erweitern**
  - MFA-Filter nach UsernamePasswordAuthenticationFilter
  - MFA-Required-Prüfung
  - Redirect zu MFA-Verification-View

---

### 2. MFA UI Components (Vaadin)

- [ ] **MFA-Setup-View**
  - QR-Code anzeigen (via QRGen oder ZXing)
  - Secret manuell eingeben (Fallback)
  - Verification-Code-Input für Setup-Validierung
  - Backup-Codes anzeigen und Download
  - "MFA aktiviert"-Bestätigung

- [ ] **MFA-Verification-View**
  - Code-Input-Field (6 Digits)
  - "Backup-Code verwenden"-Link
  - Failed-Attempts-Counter
  - Auto-Logout nach X Failed-Attempts

- [ ] **MFA-Management in User-Settings**
  - MFA aktivieren/deaktivieren
  - Neue Backup-Codes generieren
  - MFA-Status anzeigen

- [ ] **Admin-Panel: MFA-Management**
  - MFA für User aktivieren/deaktivieren (Admin)
  - MFA-Reset für User (falls Device verloren)
  - MFA-Status-Übersicht

---

### 3. Rate Limiting & Brute-Force Protection

#### Rate Limiting Service
- [ ] **RateLimitService implementieren**
  - Bucket4j oder Resilience4j verwenden
  - In-Memory Storage (Caffeine Cache)
  - Optional: Redis für verteilte Rate-Limits
  - Per-User Rate Limiting
  - Per-IP Rate Limiting
  - Configurable Limits (dev/test/prod)

#### Login Attempts Tracking
- [ ] **LoginAttemptsService**
  - Failed-Login-Counter pro User
  - Failed-Login-Counter pro IP
  - Account-Lockout nach X Versuchen (z.B. 5)
  - Lockout-Dauer (z.B. 15 Minuten)
  - Exponential Backoff (optional)
  - Unlock-Mechanism (Admin oder Zeit)

#### Rate Limiting Filter
- [ ] **RateLimitingFilter**
  - Spring Security Filter
  - Request-Counter pro User/IP
  - HTTP 429 (Too Many Requests) bei Überschreitung
  - Retry-After Header setzen
  - Rate-Limit-Infos in Response-Headers

---

### 4. Security Events & Alerts

- [ ] **Security-Event-Logging**
  - MFA-Setup-Events
  - MFA-Verification-Events (Success/Failure)
  - Rate-Limit-Violations
  - Account-Lockout-Events
  - Backup-Code-Usage

- [ ] **Alerts bei verdächtigen Aktivitäten**
  - Excessive Failed-Logins
  - Brute-Force-Attempts
  - Rate-Limit-Violations
  - Alert-Mechanismus definieren (E-Mail, Log-Level)

---

## 📁 Betroffene Dateien

### Zu modifizieren
- `src/main/java/de/bbajor/pvs/entity/User.java` - MFA-Felder
- `src/main/java/de/bbajor/pvs/security/WebSecurityConfig.java` - Filter-Chain erweitern
- `build.gradle` - Dependencies hinzufügen

### Zu erstellen
**Services:**
- `src/main/java/de/bbajor/pvs/security/mfa/TotpService.java`
- `src/main/java/de/bbajor/pvs/security/mfa/MfaAuthenticationProvider.java`
- `src/main/java/de/bbajor/pvs/security/mfa/BackupCodeService.java`
- `src/main/java/de/bbajor/pvs/security/ratelimit/RateLimitService.java`
- `src/main/java/de/bbajor/pvs/security/ratelimit/RateLimitingFilter.java`
- `src/main/java/de/bbajor/pvs/security/ratelimit/LoginAttemptsService.java`

**Views:**
- `src/main/java/de/bbajor/pvs/views/security/MfaSetupView.java`
- `src/main/java/de/bbajor/pvs/views/security/MfaVerificationView.java`
- `src/main/java/de/bbajor/pvs/views/settings/MfaSettingsView.java`
- `src/main/java/de/bbajor/pvs/views/admin/MfaAdminView.java`

**Tests:**
- `src/test/java/de/bbajor/pvs/security/mfa/TotpServiceTest.java`
- `src/test/java/de/bbajor/pvs/security/ratelimit/RateLimitServiceTest.java`
- `src/test/java/de/bbajor/pvs/security/ratelimit/LoginAttemptsServiceTest.java`

**Docs:**
- `docs/security/TOTP_SETUP.md`
- `docs/security/HYBRID_2FA_SETUP.md`
- `docs/security/RATE_LIMITING.md`

**Migrations:**
- `src/main/resources/db/migration/V{X}__add_mfa_fields_to_user.sql`

---

## 🧪 Testing

### Unit Tests
- [ ] TOTP-Secret-Generierung
- [ ] TOTP-Token-Validierung (mit Time-Drift)
- [ ] Backup-Code-Generierung und -Validierung
- [ ] Rate-Limiting-Logic
- [ ] Account-Lockout-Logic

### Integration Tests
- [ ] Login-Flow mit MFA
- [ ] MFA-Setup-Flow
- [ ] Backup-Code-Usage
- [ ] Rate-Limiting greift bei Überschreitung
- [ ] Account-Lockout nach X Failed-Logins

### Security Tests
- [ ] Brute-Force-Attack-Simulation
- [ ] Rate-Limiting unter Last
- [ ] MFA-Bypass-Versuche

---

## 🔗 Abhängigkeiten

### Code-Abhängigkeiten
- **Agent 2:** SecurityFilterChain erweitern (von Agent 2 erstellt)
- **User Entity:** Muss bereits existieren

### Dependencies (build.gradle)
```gradle
// TOTP Library
implementation 'dev.samstevens.totp:totp:1.7.1'
// oder alternative: 'com.warrenstrange:googleauth:1.5.0'

// QR-Code-Generierung
implementation 'com.google.zxing:core:3.5.2'
implementation 'com.google.zxing:javase:3.5.2'

// Rate Limiting
implementation 'com.github.vladimir-bukhtoyarov:bucket4j-core:8.1.1'
// oder: Resilience4j
implementation 'io.github.resilience4j:resilience4j-ratelimiter:2.1.0'
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'

// Caching (für Rate-Limiting)
implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'
```

---

## 📚 Dokumentation

### docs/security/TOTP_SETUP.md
- TOTP-Funktionsweise erklärt
- Setup-Flow für User
- QR-Code-Generierung
- Backup-Codes
- Troubleshooting (Device verloren, etc.)

### docs/security/HYBRID_2FA_SETUP.md
- Hybrid-Approach erklärt (optional aktivierbar)
- Admin-Management
- User-Self-Service
- Best Practices

### docs/security/RATE_LIMITING.md
- Rate-Limiting-Strategie
- Thresholds (dev/test/prod)
- Brute-Force-Protection
- Account-Lockout-Logic
- Unlock-Prozess

---

## 🎓 Hilfreiche Ressourcen

- [RFC 6238 - TOTP](https://datatracker.ietf.org/doc/html/rfc6238)
- [Google Authenticator](https://github.com/google/google-authenticator)
- [Bucket4j Docs](https://bucket4j.com/)
- [Resilience4j Docs](https://resilience4j.readme.io/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

---

## ✅ Definition of Done

- [ ] MFA-Setup funktioniert (QR-Code, Verification)
- [ ] Login mit MFA erfolgreich
- [ ] Backup-Codes funktionieren
- [ ] Rate-Limiting greift bei Überschreitung
- [ ] Account-Lockout nach Failed-Logins
- [ ] Admin kann MFA für User verwalten
- [ ] User kann MFA selbst aktivieren/deaktivieren
- [ ] Alle Tests grün (Unit + Integration)
- [ ] Security-Events werden geloggt
- [ ] Dokumentation vollständig
- [ ] Build erfolgreich
- [ ] Keine Linter-Errors

---

## 🚨 Wichtige Hinweise

1. **TOTP Time-Drift:** 30s Window + 1 Period Drift erlauben (total 60s)
2. **Backup-Codes:** Nur einmal verwendbar, dann deaktivieren
3. **Secrets-Storage:** TOTP-Secret verschlüsselt speichern (z.B. via AES)
4. **Rate-Limiting:** Dev-Environment großzügige Limits für Testing
5. **Account-Lockout:** Admin-Unlock-Mechanism vorsehen
6. **QR-Code-Security:** QR-Code nur einmal anzeigen, dann löschen
7. **Vaadin Session:** MFA-Pending-State in Session speichern

---

## 🤝 Koordination mit anderen Agenten

### Agent 2 (Spring Security)
- SecurityFilterChain wird hier erweitert
- MFA-Filter nach Authentication-Filter einfügen

### Agent 5 (Monitoring)
- Security-Events hier definieren
- Logging dort implementieren

### Agent 4 (Database)
- User Entity wird hier modifiziert
- Keine DB-Schema-Konflikte erwarten (neue Felder)

---

**Erstellt:** 2025-10-30  
**Status:** 🟡 Waiting for Agent 2  
**Nächster Agent:** Agent 4 (parallel möglich)
