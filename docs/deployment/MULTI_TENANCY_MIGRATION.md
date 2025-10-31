# Multi-Tenancy Migration Guide

## Übersicht

Dieses Dokument beschreibt die Migration einer bestehenden Single-Tenant-Installation zur Multi-Tenant-Architektur.

## ⚠️ Wichtige Hinweise

- **Backup erstellen**: Vor der Migration MUSS ein vollständiges Datenbank-Backup erstellt werden!
- **Downtime einplanen**: Die Migration erfordert Downtime (ca. 10-30 Minuten je nach Datenmenge)
- **Rollback-Plan**: Backup sollte für mindestens 30 Tage aufbewahrt werden
- **Testing**: Migrations-Script ERST auf Test-/Staging-Umgebung testen!

## Voraussetzungen

- PostgreSQL 12+ oder H2 Database
- Vollständiges Datenbank-Backup
- Downtime-Fenster genehmigt
- Test-Umgebung verfügbar

## Migrations-Script

### Schritt 1: Tenant-Tabelle erstellen

```sql
-- Tenant-Tabelle mit allen notwendigen Feldern
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0
);

-- Index für Performance
CREATE INDEX idx_tenant_code ON tenant(tenant_code);
CREATE INDEX idx_tenant_active ON tenant(active);
```

### Schritt 2: Standard-Tenant anlegen

```sql
-- Standard-Tenant für bestehende Daten
-- WICHTIG: Tenant-Code und Name anpassen!
INSERT INTO tenant (tenant_code, tenant_name, active, description, version)
VALUES (
    'PROD-LEGACY',
    'Bestehende Praxis (Migriert)',
    true,
    'Automatisch migrierte Daten aus Single-Tenant-Installation',
    0
);
```

### Schritt 3: tenant_id-Spalten hinzufügen

```sql
-- Kern-Tabellen (NOT NULL nach Migration)
ALTER TABLE patient ADD COLUMN tenant_id BIGINT;
ALTER TABLE user_account ADD COLUMN tenant_id BIGINT;
ALTER TABLE practice ADD COLUMN tenant_id BIGINT;
ALTER TABLE treatment ADD COLUMN tenant_id BIGINT;
ALTER TABLE treatment_plan ADD COLUMN tenant_id BIGINT;
ALTER TABLE task ADD COLUMN tenant_id BIGINT;
ALTER TABLE surgical_center ADD COLUMN tenant_id BIGINT;
ALTER TABLE surgical_center_time_slot ADD COLUMN tenant_id BIGINT;
ALTER TABLE clinical_trial ADD COLUMN tenant_id BIGINT;

-- Optionale Tabellen (NULL erlaubt für System-weite Einträge)
ALTER TABLE medication ADD COLUMN tenant_id BIGINT;
ALTER TABLE diagnosis ADD COLUMN tenant_id BIGINT;
ALTER TABLE health_insurance ADD COLUMN tenant_id BIGINT;
```

### Schritt 4: Bestehende Daten zuweisen

```sql
-- Standard-Tenant-ID ermitteln
DO $$
DECLARE
    default_tenant_id BIGINT;
BEGIN
    SELECT id INTO default_tenant_id FROM tenant WHERE tenant_code = 'PROD-LEGACY';
    
    -- Alle bestehenden Daten dem Standard-Tenant zuweisen
    UPDATE patient SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    UPDATE user_account SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    UPDATE practice SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    UPDATE treatment SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    UPDATE treatment_plan SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    UPDATE task SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    UPDATE surgical_center SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    UPDATE surgical_center_time_slot SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    UPDATE clinical_trial SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    
    -- Optional: System-weite Medications/Diagnoses beibehalten (tenant_id = NULL)
    -- Falls gewünscht, analog zu oben updaten
END $$;
```

### Schritt 5: Constraints und Foreign Keys setzen

```sql
-- NOT NULL Constraints für Kern-Tabellen
ALTER TABLE patient ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE practice ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE treatment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE treatment_plan ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE task ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE surgical_center ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE surgical_center_time_slot ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE clinical_trial ALTER COLUMN tenant_id SET NOT NULL;

-- Foreign Key Constraints
ALTER TABLE patient 
    ADD CONSTRAINT fk_patient_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE user_account 
    ADD CONSTRAINT fk_user_account_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE practice 
    ADD CONSTRAINT fk_practice_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE treatment 
    ADD CONSTRAINT fk_treatment_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE treatment_plan 
    ADD CONSTRAINT fk_treatment_plan_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE task 
    ADD CONSTRAINT fk_task_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE surgical_center 
    ADD CONSTRAINT fk_surgical_center_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE surgical_center_time_slot 
    ADD CONSTRAINT fk_surgical_center_time_slot_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE clinical_trial 
    ADD CONSTRAINT fk_clinical_trial_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

-- Optional: Foreign Keys für nullable tenant_id
ALTER TABLE medication 
    ADD CONSTRAINT fk_medication_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE diagnosis 
    ADD CONSTRAINT fk_diagnosis_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE health_insurance 
    ADD CONSTRAINT fk_health_insurance_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;
```

### Schritt 6: Unique Constraints aktualisieren

```sql
-- Patient: Vorhandene Unique Constraints entfernen und mit tenant_id neu erstellen
ALTER TABLE patient DROP CONSTRAINT IF EXISTS patient_first_name_last_name_birth_key;
ALTER TABLE patient DROP CONSTRAINT IF EXISTS patient_insurance_number_key;

ALTER TABLE patient 
    ADD CONSTRAINT patient_tenant_name_birth_unique 
    UNIQUE (tenant_id, first_name, last_name, birth);

ALTER TABLE patient 
    ADD CONSTRAINT patient_tenant_insurance_number_unique 
    UNIQUE (tenant_id, insurance_number);

-- UserAccount: Username-Unique Constraint anpassen
ALTER TABLE user_account DROP CONSTRAINT IF EXISTS user_account_username_key;

ALTER TABLE user_account 
    ADD CONSTRAINT user_account_tenant_username_unique 
    UNIQUE (tenant_id, username);

-- SurgicalCenterTimeSlot: Unique Constraint anpassen
ALTER TABLE surgical_center_time_slot DROP CONSTRAINT IF EXISTS surgical_center_time_slot_surgical_center_id_date_start_ti_key;

ALTER TABLE surgical_center_time_slot 
    ADD CONSTRAINT surgical_center_time_slot_unique 
    UNIQUE (tenant_id, surgicalcenter_id, date, start_time, end_time);
```

### Schritt 7: Indexes für Performance

```sql
-- Performance-Indexes für tenant_id
CREATE INDEX idx_patient_tenant_id ON patient(tenant_id);
CREATE INDEX idx_user_account_tenant_id ON user_account(tenant_id);
CREATE INDEX idx_practice_tenant_id ON practice(tenant_id);
CREATE INDEX idx_treatment_tenant_id ON treatment(tenant_id);
CREATE INDEX idx_treatment_plan_tenant_id ON treatment_plan(tenant_id);
CREATE INDEX idx_task_tenant_id ON task(tenant_id);
CREATE INDEX idx_surgical_center_tenant_id ON surgical_center(tenant_id);
CREATE INDEX idx_surgical_center_time_slot_tenant_id ON surgical_center_time_slot(tenant_id);
CREATE INDEX idx_clinical_trial_tenant_id ON clinical_trial(tenant_id);
CREATE INDEX idx_medication_tenant_id ON medication(tenant_id);
CREATE INDEX idx_diagnosis_tenant_id ON diagnosis(tenant_id);
CREATE INDEX idx_health_insurance_tenant_id ON health_insurance(tenant_id);
```

### Schritt 8: Super-Admin-Benutzer erstellen (Optional)

```sql
-- Super-Admin ohne Tenant-Zuordnung (kann alle Tenants verwalten)
INSERT INTO user_account (
    username, 
    password_hash, 
    enabled, 
    tenant_id, 
    user_id, 
    full_name, 
    email,
    version
)
VALUES (
    'superadmin',
    '{bcrypt}$2a$10$...', -- Passwort-Hash hier einfügen
    true,
    NULL, -- Kein Tenant für Super-Admin
    'super-admin-001',
    'System Administrator',
    'admin@example.com',
    0
);

-- Rolle hinzufügen (ElementCollection-Tabelle)
INSERT INTO user_account_roles (user_account_id, roles)
SELECT id, 'SUPER_ADMIN' FROM user_account WHERE username = 'superadmin';

INSERT INTO user_account_roles (user_account_id, roles)
SELECT id, 'ADMIN' FROM user_account WHERE username = 'superadmin';

INSERT INTO user_account_roles (user_account_id, roles)
SELECT id, 'USER' FROM user_account WHERE username = 'superadmin';
```

## Validierung

Nach der Migration:

```sql
-- 1. Prüfen, ob alle Patienten einen Tenant haben
SELECT COUNT(*) FROM patient WHERE tenant_id IS NULL;
-- Sollte 0 sein

-- 2. Prüfen, ob alle User (außer Super-Admin) einen Tenant haben
SELECT COUNT(*) FROM user_account 
WHERE tenant_id IS NULL 
  AND NOT EXISTS (
      SELECT 1 FROM user_account_roles 
      WHERE user_account_id = user_account.id 
        AND roles = 'SUPER_ADMIN'
  );
-- Sollte 0 sein

-- 3. Tenant-Verteilung prüfen
SELECT t.tenant_code, t.tenant_name, COUNT(p.id) as patient_count
FROM tenant t
LEFT JOIN patient p ON t.id = p.tenant_id
GROUP BY t.id, t.tenant_code, t.tenant_name;

-- 4. Unique Constraints testen
SELECT tenant_id, first_name, last_name, birth, COUNT(*)
FROM patient
GROUP BY tenant_id, first_name, last_name, birth
HAVING COUNT(*) > 1;
-- Sollte leer sein
```

## Rollback-Plan

Falls Probleme auftreten:

```sql
-- 1. Anwendung stoppen

-- 2. Datenbank-Backup wiederherstellen
-- (Abhängig vom verwendeten Backup-Tool)

-- 3. Oder: Manuelle Rollback-Schritte
-- Foreign Keys und Constraints entfernen
ALTER TABLE patient DROP CONSTRAINT IF EXISTS fk_patient_tenant;
-- ... weitere Constraints

-- Spalten entfernen
ALTER TABLE patient DROP COLUMN IF EXISTS tenant_id;
-- ... weitere Spalten

-- Tenant-Tabelle entfernen
DROP TABLE IF EXISTS tenant CASCADE;
```

## Nach der Migration

1. **Anwendung mit Multi-Tenant-Version deployen**
2. **Login testen** mit Tenant-Code
3. **Funktionstest durchführen**:
   - Patient anlegen
   - Behandlung erfassen
   - Benutzer-Verwaltung
4. **Monitoring aktivieren** für Cross-Tenant-Zugriffe
5. **Backup-Strategie aktualisieren**

## Neue Tenant hinzufügen

Nach erfolgreicher Migration können neue Tenants über die Web-UI hinzugefügt werden:

1. Als **Super-Admin** einloggen
2. Navigation: **Admin → Tenant-Verwaltung**
3. Neuen Tenant anlegen
4. Tenant-Code notieren und an Kunden weitergeben
5. Ersten Admin-Benutzer für den neuen Tenant anlegen

## Troubleshooting

### Problem: Foreign Key Constraint Violation

```sql
-- Prüfen, welche Datensätze keinen gültigen Tenant haben
SELECT 'patient' as table_name, COUNT(*) 
FROM patient p 
LEFT JOIN tenant t ON p.tenant_id = t.id 
WHERE p.tenant_id IS NOT NULL AND t.id IS NULL;
```

### Problem: Unique Constraint Violation

```sql
-- Duplikate in Patient-Tabelle finden
SELECT tenant_id, first_name, last_name, birth, COUNT(*)
FROM patient
GROUP BY tenant_id, first_name, last_name, birth
HAVING COUNT(*) > 1;

-- Duplikate manuell bereinigen (Vorsicht!)
-- Fall-by-Fall-Entscheidung nötig
```

### Problem: Performance nach Migration

```sql
-- Statistiken aktualisieren
ANALYZE patient;
ANALYZE user_account;
-- ... weitere Tabellen

-- Oder alle:
ANALYZE;
```

## Kontakt & Support

Bei Fragen oder Problemen:
- GitHub Issues: [Repository-URL]
- E-Mail: support@example.com

---

**Version**: 1.0  
**Letzte Aktualisierung**: 2025-10-31  
**Getestet mit**: PostgreSQL 14, H2 2.x
