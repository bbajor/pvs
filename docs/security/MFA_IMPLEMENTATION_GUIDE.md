# MFA Implementation Guide (Agent 3)

**Multi-Factor Authentication mit TOTP & Rate Limiting**

---

## 🎯 Was wurde implementiert

### Backend Services
✅ **TotpService** (`security/mfa/TotpService.java`)
- Secret-Generierung (Base32)
- QR-Code-Generierung (Google Authenticator)
- TOTP-Validierung (30s Window, 1 Period Drift)
- Backup-Codes-Generierung

✅ **RateLimitService** (`security/ratelimit/RateLimitService.java`)
- Per-User & Per-IP Rate Limiting
- Standard: 100 req/s
- Login: 5 req/min
- Bucket4j + Caffeine Cache

✅ **LoginAttemptsService** (`security/ratelimit/LoginAttemptsService.java`)
- Failed-Login-Tracking
- Account-Lockout (5 Versuche)
- Auto-Unlock (15 Minuten)

### Database
✅ **UserAccount erweitert**
- `mfaEnabled` - MFA aktiviert?
- `mfaSecret` - TOTP Secret
- `mfaBackupCodes` - Hashed Backup-Codes
- `mfaSetupCompleted` - Setup fertig?

✅ **Migration** (`V999__add_mfa_fields_to_user_account.sql`)

### Dependencies
✅ **build.gradle**
- `dev.samstevens.totp:totp:1.7.1`
- `com.google.zxing` für QR-Codes
- `bucket4j-core` für Rate-Limiting
- `caffeine` für In-Memory-Cache

---

## 🚀 Wie MFA nutzen (für UI-Entwicklung)

### 1. MFA-Setup-Flow

```java
@Autowired
private TotpService totpService;

@Autowired
private UserAccountRepository userRepo;

// User will MFA aktivieren
public void setupMfa(UserAccount user) {
    // 1. Secret generieren
    String secret = totpService.generateSecret();
    
    // 2. QR-Code generieren
    String qrCodeDataUri = totpService.generateQrCodeDataUri(
        secret, 
        user.getUsername(), 
        "PVS"
    );
    
    // 3. QR-Code anzeigen (Vaadin Image)
    Image qrCode = new Image(qrCodeDataUri, "MFA QR-Code");
    
    // 4. Backup-Codes generieren
    List<String> backupCodes = totpService.generateBackupCodes();
    
    // 5. User verifiziert Code
    // (siehe verifySetup())
}

// User gibt ersten Code ein (Verification)
public boolean verifySetup(UserAccount user, String secret, String code) {
    if (totpService.verifyCode(secret, code)) {
        // Code korrekt → MFA aktivieren
        user.setMfaEnabled(true);
        user.setMfaSecret(secret);  // TODO: Encrypt!
        user.setMfaSetupCompleted(true);
        // TODO: Backup-Codes hashen und speichern
        userRepo.save(user);
        return true;
    }
    return false;
}
```

### 2. MFA-Login-Flow

```java
// Nach Username/Password-Auth
public boolean verifyMfaCode(UserAccount user, String code) {
    if (!user.isMfaEnabled()) {
        return true;  // MFA nicht aktiviert
    }
    
    // TOTP-Code prüfen
    if (totpService.verifyCode(user.getMfaSecret(), code)) {
        return true;
    }
    
    // TODO: Backup-Code prüfen (falls TOTP fehlschlägt)
    
    return false;
}
```

### 3. Rate-Limiting nutzen

```java
@Autowired
private RateLimitService rateLimitService;

@Autowired
private LoginAttemptsService loginAttemptsService;

// Vor Login
public boolean canAttemptLogin(String username, String ipAddress) {
    // 1. Account gesperrt?
    if (loginAttemptsService.isLocked(username)) {
        return false;
    }
    
    // 2. Rate-Limit überschritten?
    if (!rateLimitService.allowLoginRequest(username)) {
        return false;
    }
    
    if (!rateLimitService.allowRequestByIp(ipAddress)) {
        return false;
    }
    
    return true;
}

// Nach erfolgreicher Auth
public void onLoginSuccess(String username) {
    loginAttemptsService.loginSucceeded(username);
    rateLimitService.resetLimit(username);
}

// Nach fehlgeschlagener Auth
public void onLoginFailed(String username) {
    loginAttemptsService.loginFailed(username);
}
```

---

## 📝 TODO für vollständige Implementierung

### UI-Components (Vaadin)
- [ ] `MfaSetupView` - QR-Code-Setup-Dialog
- [ ] `MfaVerificationView` - Code-Eingabe bei Login
- [ ] `MfaSettingsView` - User-Settings für MFA
- [ ] `MfaAdminView` - Admin-Panel für MFA-Management

### Security-Integration
- [ ] `MfaAuthenticationFilter` - Spring Security Filter für MFA-Check
- [ ] Login-Flow erweitern (2-Step-Auth)
- [ ] MFA-Required-Check in Security-Config

### Backup-Codes
- [ ] `BackupCodeService` - Backup-Code-Handling
- [ ] Backup-Codes hashen (BCrypt)
- [ ] One-Time-Usage implementieren

### Testing
- [ ] Unit-Tests für TotpService
- [ ] Unit-Tests für RateLimitService
- [ ] Integration-Tests für MFA-Flow

---

## 🧪 Testing (manuell)

### TOTP-Service testen

```java
TotpService totp = new TotpService();

// 1. Secret generieren
String secret = totp.generateSecret();
System.out.println("Secret: " + secret);

// 2. QR-Code generieren
String qrCode = totp.generateQrCodeDataUri(secret, "testuser", "PVS");
System.out.println("QR-Code: " + qrCode);

// 3. In Google Authenticator scannen
// 4. Code validieren
boolean valid = totp.verifyCode(secret, "123456");  // Code aus App
System.out.println("Valid: " + valid);
```

### Rate-Limiting testen

```bash
# 100 Requests in Folge
for i in {1..150}; do
  curl http://localhost:8080/login
done

# Erwartung: Nach ~100 Requests → HTTP 429 (Too Many Requests)
```

---

## 🔒 Security-Hinweise

### ⚠️ TOTP-Secret VERSCHLÜSSELN!

**Aktuell:** Secret wird plain-text in DB gespeichert (NICHT Production-ready!)

**Lösung:**
```java
// Encryption-Service verwenden
String encryptedSecret = encryptionService.encrypt(secret);
user.setMfaSecret(encryptedSecret);

// Bei Validierung
String decryptedSecret = encryptionService.decrypt(user.getMfaSecret());
boolean valid = totpService.verifyCode(decryptedSecret, code);
```

### ⚠️ Backup-Codes HASHEN!

**Aktuell:** Backup-Codes müssen noch gehasht werden

**Lösung:**
```java
// BCrypt für Backup-Codes
String hashedCode = passwordEncoder.encode(backupCode);
```

---

## 📚 Weitere Docs

- [TOTP Setup Guide](./TOTP_SETUP.md) - Für End-User
- [Rate Limiting](./RATE_LIMITING.md) - Konfiguration
- [Security Headers](./SECURITY_HEADERS.md) - OWASP-Compliance

---

**Erstellt:** 2025-10-30  
**Status:** ✅ Backend fertig, UI TODO  
**Version:** 1.0 (Agent 3)
