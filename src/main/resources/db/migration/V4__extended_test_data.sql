-- Extended test data migration for dev and test environments
-- This script extends V2__test_data.sql with:
-- - Additional locations per institution
-- - Patients at each location
-- - Surgical centers with time slots (Wednesday and Friday, 7-9 AM)
-- - Treatment plans with treatments (past and future appointments, 4-12 weeks apart)
-- - PRAX-002 institution remains empty for admin testing

-- Note: This script uses PostgreSQL-specific functions for date calculations
-- and generates time slots using recursive CTEs

-- ============================================
-- ZUSÄTZLICHE LOCATIONS
-- ============================================

-- Zusätzliche Locations für DEV-TEST Institution
INSERT INTO location (location_name, street, house_number, postal_code, city, country, owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, active, institution_id, version)
SELECT 
    'Augenarztpraxis Muster - Filiale Charlottenburg',
    'Kurfürstendamm',
    '100',
    '10719',
    'Berlin',
    'Deutschland',
    'Dr. Max Mustermann',
    'Dr. med.',
    '123456789',
    '987654321',
    '+49 30 23456789',
    '+49 30 23456790',
    'charlottenburg@augenarzt-muster.de',
    'Zweigstelle in Charlottenburg',
    TRUE,
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'DEV-TEST'
ON CONFLICT DO NOTHING;

INSERT INTO location (location_name, street, house_number, postal_code, city, country, owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, active, institution_id, version)
SELECT 
    'Augenarztpraxis Muster - Filiale Prenzlauer Berg',
    'Schönhauser Allee',
    '50',
    '10437',
    'Berlin',
    'Deutschland',
    'Dr. Anna Schmidt',
    'Dr. med.',
    '234567890',
    '876543210',
    '+49 30 34567890',
    '+49 30 34567891',
    'prenzlauerberg@augenarzt-muster.de',
    'Zweigstelle in Prenzlauer Berg',
    TRUE,
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'DEV-TEST'
ON CONFLICT DO NOTHING;

-- Zusätzliche Locations für PRAX-001 Institution
INSERT INTO location (location_name, street, house_number, postal_code, city, country, owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, active, institution_id, version)
SELECT 
    'Augenarztpraxis Dr. Müller - Hauptstandort',
    'Friedrichstraße',
    '123',
    '10117',
    'Berlin',
    'Deutschland',
    'Dr. Klaus Müller',
    'Dr. med.',
    '345678901',
    '765432109',
    '+49 30 45678901',
    '+49 30 45678902',
    'haupt@mueller-augen.de',
    'Hauptstandort der Praxis',
    TRUE,
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;

INSERT INTO location (location_name, street, house_number, postal_code, city, country, owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, active, institution_id, version)
SELECT 
    'Augenarztpraxis Dr. Müller - Filiale Mitte',
    'Unter den Linden',
    '1',
    '10117',
    'Berlin',
    'Deutschland',
    'Dr. Petra Müller',
    'Dr. med.',
    '456789012',
    '654321098',
    '+49 30 56789012',
    '+49 30 56789013',
    'mitte@mueller-augen.de',
    'Filiale in Berlin-Mitte',
    TRUE,
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;

-- ============================================
-- ZUSÄTZLICHE PATIENTEN
-- ============================================

-- Zusätzliche Patienten für DEV-TEST Institution (an verschiedenen Standorten)
INSERT INTO patient (first_name, last_name, birth, patient_street, patient_house_no, patient_postal_code, patient_city, patient_country, gender, phone, email, insurance_number, location_id, institution_id, health_insurance_id, version)
SELECT 
    'Lisa',
    'Weber',
    '1985-07-20',
    'Kantstraße',
    '12',
    10623,
    'Berlin',
    'DE',
    'W',
    '+49 30 11111111',
    'lisa.weber@example.com',
    'D111111111',
    l.id,
    i.id,
    hi.id,
    0
FROM institution i
CROSS JOIN location l
LEFT JOIN health_insurance hi ON hi.institution_id = i.id AND hi.billing_carrier_id = 'AOK-BB'
WHERE i.institution_code = 'DEV-TEST' AND l.institution_id = i.id AND l.location_name LIKE '%Charlottenburg%'
LIMIT 1
ON CONFLICT (institution_id, first_name, last_name, birth) DO NOTHING;

INSERT INTO patient (first_name, last_name, birth, patient_street, patient_house_no, patient_postal_code, patient_city, patient_country, gender, phone, email, insurance_number, location_id, institution_id, health_insurance_id, version)
SELECT 
    'Thomas',
    'Fischer',
    '1972-11-30',
    'Prenzlauer Allee',
    '8',
    10405,
    'Berlin',
    'DE',
    'M',
    '+49 30 22222222',
    'thomas.fischer@example.com',
    'E222222222',
    l.id,
    i.id,
    hi.id,
    0
FROM institution i
CROSS JOIN location l
LEFT JOIN health_insurance hi ON hi.institution_id = i.id AND hi.billing_carrier_id = 'AOK-BB'
WHERE i.institution_code = 'DEV-TEST' AND l.institution_id = i.id AND l.location_name LIKE '%Prenzlauer Berg%'
LIMIT 1
ON CONFLICT (institution_id, first_name, last_name, birth) DO NOTHING;

-- Patienten für PRAX-001 Institution
INSERT INTO patient (first_name, last_name, birth, patient_street, patient_house_no, patient_postal_code, patient_city, patient_country, gender, phone, email, insurance_number, location_id, institution_id, health_insurance_id, version)
SELECT 
    'Sabine',
    'Koch',
    '1995-03-15',
    'Friedrichstraße',
    '200',
    10117,
    'Berlin',
    'DE',
    'W',
    '+49 30 33333333',
    'sabine.koch@example.com',
    'F333333333',
    l.id,
    i.id,
    hi.id,
    0
FROM institution i
CROSS JOIN location l
LEFT JOIN health_insurance hi ON hi.institution_id = i.id AND hi.billing_carrier_id = 'AOK-BB'
WHERE i.institution_code = 'PRAX-001' AND l.institution_id = i.id AND l.location_name LIKE '%Hauptstandort%'
LIMIT 1
ON CONFLICT (institution_id, first_name, last_name, birth) DO NOTHING;

INSERT INTO patient (first_name, last_name, birth, patient_street, patient_house_no, patient_postal_code, patient_city, patient_country, gender, phone, email, insurance_number, location_id, institution_id, health_insurance_id, version)
SELECT 
    'Michael',
    'Bauer',
    '1988-09-25',
    'Unter den Linden',
    '50',
    10117,
    'Berlin',
    'DE',
    'M',
    '+49 30 44444444',
    'michael.bauer@example.com',
    'G444444444',
    l.id,
    i.id,
    hi.id,
    0
FROM institution i
CROSS JOIN location l
LEFT JOIN health_insurance hi ON hi.institution_id = i.id AND hi.billing_carrier_id = 'AOK-BB'
WHERE i.institution_code = 'PRAX-001' AND l.institution_id = i.id AND l.location_name LIKE '%Mitte%'
LIMIT 1
ON CONFLICT (institution_id, first_name, last_name, birth) DO NOTHING;

-- ============================================
-- DIAGNOSEN UND MEDIKAMENTE
-- ============================================

-- Diagnosen erstellen
INSERT INTO diagnosis (name, icd_code, description, version)
VALUES 
    ('Feuchte altersbedingte Makuladegeneration', 'H35.3', 'AMD feucht', 0),
    ('Diabetisches Makulaödem', 'H35.0', 'DME', 0),
    ('Venenverschluss der Netzhaut', 'H43.8', 'RVO', 0),
    ('Zentrale seröse Chorioretinopathie', 'H35.81', 'CSCR', 0)
ON CONFLICT DO NOTHING;

-- ============================================
-- SURGICAL CENTERS MIT ZEITSLOTS
-- ============================================

-- Surgical Centers für DEV-TEST Institution (2 Stück)
INSERT INTO surgical_center (name, description, phone, email, contact, surgical_center_street, surgical_center_house_no, surgical_center_postal_code, surgical_center_city, surgical_center_country, institution_id, version)
SELECT 
    'Augenklinik Mitte',
    'Zentrum für intravitreale Injektionen',
    '+49 30 11111111',
    'augenklinik@example.com',
    'Dr. Mustermann',
    'Hauptstraße',
    '10',
    10115,
    'Berlin',
    'DE',
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'DEV-TEST'
ON CONFLICT DO NOTHING;

INSERT INTO surgical_center (name, description, phone, email, contact, surgical_center_street, surgical_center_house_no, surgical_center_postal_code, surgical_center_city, surgical_center_country, institution_id, version)
SELECT 
    'MVZ Sehen und Mehr',
    'Zentrum für intravitreale Injektionen',
    '+49 30 22222222',
    'mvz@example.com',
    'Dr. Schmidt',
    'Bahnhofstraße',
    '20',
    10115,
    'Berlin',
    'DE',
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'DEV-TEST'
ON CONFLICT DO NOTHING;

-- Surgical Centers für PRAX-001 Institution (2 Stück)
INSERT INTO surgical_center (name, description, phone, email, contact, surgical_center_street, surgical_center_house_no, surgical_center_postal_code, surgical_center_city, surgical_center_country, institution_id, version)
SELECT 
    'Augen-OP Zentrum',
    'Zentrum für intravitreale Injektionen',
    '+49 30 33333333',
    'op-zentrum@example.com',
    'Dr. Müller',
    'Friedrichstraße',
    '50',
    10117,
    'Berlin',
    'DE',
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;

INSERT INTO surgical_center (name, description, phone, email, contact, surgical_center_street, surgical_center_house_no, surgical_center_postal_code, surgical_center_city, surgical_center_country, institution_id, version)
SELECT 
    'Netzhautzentrum',
    'Zentrum für intravitreale Injektionen',
    '+49 30 44444444',
    'netzhaut@example.com',
    'Dr. Koch',
    'Unter den Linden',
    '30',
    10117,
    'Berlin',
    'DE',
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;

-- Zeitslots für Surgical Centers generieren (Mittwoch und Freitag, 7-9 Uhr, 6 Monate Vergangenheit bis 2 Jahre Zukunft)
-- Verwende rekursive CTE für PostgreSQL
WITH RECURSIVE time_slots AS (
    -- Start: Erster Mittwoch 6 Monate in der Vergangenheit
    SELECT 
        sc.id as surgical_center_id,
        (DATE_TRUNC('week', CURRENT_DATE - INTERVAL '6 months') + INTERVAL '2 days')::date as slot_date,
        0 as week_offset,
        3 as day_of_week  -- Mittwoch
    FROM surgical_center sc
    WHERE sc.institution_id IN (
        SELECT id FROM institution WHERE institution_code IN ('DEV-TEST', 'PRAX-001')
    )
    
    UNION ALL
    
    -- Erster Freitag (3 Tage nach Mittwoch)
    SELECT 
        sc.id as surgical_center_id,
        (DATE_TRUNC('week', CURRENT_DATE - INTERVAL '6 months') + INTERVAL '4 days')::date as slot_date,
        0 as week_offset,
        5 as day_of_week  -- Freitag
    FROM surgical_center sc
    WHERE sc.institution_id IN (
        SELECT id FROM institution WHERE institution_code IN ('DEV-TEST', 'PRAX-001')
    )
    
    UNION ALL
    
    -- Rekursion: Nächste Woche (Mittwoch und Freitag)
    SELECT 
        ts.surgical_center_id,
        (ts.slot_date + INTERVAL '1 week')::date,
        ts.week_offset + 1,
        ts.day_of_week
    FROM time_slots ts
    WHERE ts.week_offset < 130  -- ~2.5 Jahre (130 Wochen)
      AND ts.slot_date <= (CURRENT_DATE + INTERVAL '2 years')::date
)
INSERT INTO surgical_center_time_slot (description, date, start_time, end_time, is_available, is_approved, surgical_center_id, version)
SELECT 
    CASE 
        WHEN ts.day_of_week = 3 THEN 'Regulärer Mittwoch-Termin'
        WHEN ts.day_of_week = 5 THEN 'Regulärer Freitag-Termin'
    END,
    ts.slot_date,
    '07:00:00'::time,
    '09:00:00'::time,
    ts.slot_date >= CURRENT_DATE,  -- Verfügbar ab heute
    TRUE,
    ts.surgical_center_id,
    0
FROM time_slots ts
WHERE ts.slot_date >= (CURRENT_DATE - INTERVAL '6 months')
  AND ts.slot_date <= (CURRENT_DATE + INTERVAL '2 years')
ON CONFLICT (surgical_center_id, date, start_time, end_time) DO NOTHING;

-- ============================================
-- BEHANDLUNGSPLÄNE MIT TERMINEN
-- ============================================

-- Behandlungspläne für Patienten erstellen
-- Für jeden Patienten einen Behandlungsplan mit Terminen in Vergangenheit und Zukunft (4-12 Wochen Abstand)

-- Behandlungspläne für DEV-TEST Patienten
INSERT INTO treatment_plan (creation_date, description, additional_information, institution_id, patient_id, diagnosis_id, version)
SELECT 
    (CURRENT_DATE - INTERVAL '90 days')::date as creation_date,
    'Behandlungsplan für ' || d.name || ' - ' || p.first_name || ' ' || p.last_name as description,
    'Regelmäßige Kontrolle erforderlich' as additional_information,
    i.id as institution_id,
    p.id as patient_id,
    d.id as diagnosis_id,
    0
FROM patient p
JOIN institution i ON p.institution_id = i.id
CROSS JOIN diagnosis d
WHERE i.institution_code = 'DEV-TEST'
  AND d.name = 'Feuchte altersbedingte Makuladegeneration'
ON CONFLICT DO NOTHING;

-- Behandlungspläne für PRAX-001 Patienten
INSERT INTO treatment_plan (creation_date, description, additional_information, institution_id, patient_id, diagnosis_id, version)
SELECT 
    (CURRENT_DATE - INTERVAL '60 days')::date as creation_date,
    'Behandlungsplan für ' || d.name || ' - ' || p.first_name || ' ' || p.last_name as description,
    'Regelmäßige Kontrolle erforderlich' as additional_information,
    i.id as institution_id,
    p.id as patient_id,
    d.id as diagnosis_id,
    0
FROM patient p
JOIN institution i ON p.institution_id = i.id
CROSS JOIN diagnosis d
WHERE i.institution_code = 'PRAX-001'
  AND d.name = 'Diabetisches Makulaödem'
ON CONFLICT DO NOTHING;

-- Behandlungen für Behandlungspläne erstellen
-- Termine in Vergangenheit (alle 8 Wochen, 4 Termine)
INSERT INTO treatment (approval_date, frequency, dosage, side_of_eye, treatment_plan_id, surgical_center_time_slot_id, medication_id, version)
SELECT 
    tsl.date as approval_date,
    '8 Wochen' as frequency,
    'Standard' as dosage,
    CASE (row_number() OVER (PARTITION BY tp.id) % 3)
        WHEN 0 THEN 'LEFT'
        WHEN 1 THEN 'RIGHT'
        ELSE 'BOTH'
    END as side_of_eye,
    tp.id as treatment_plan_id,
    tsl.id as surgical_center_time_slot_id,
    m.id as medication_id,
    0
FROM treatment_plan tp
JOIN patient p ON tp.patient_id = p.id
JOIN institution i ON p.institution_id = i.id
CROSS JOIN surgical_center sc
CROSS JOIN medication m
CROSS JOIN LATERAL (
    SELECT id, date
    FROM surgical_center_time_slot 
    WHERE surgical_center_id = sc.id
      AND sc.institution_id = i.id
      AND date < CURRENT_DATE
      AND date >= (tp.creation_date)
      AND EXTRACT(DOW FROM date) IN (3, 5)  -- Mittwoch oder Freitag
    ORDER BY date
    LIMIT 4
) tsl
WHERE i.institution_code IN ('DEV-TEST', 'PRAX-001')
ON CONFLICT DO NOTHING;

-- Zukünftige Behandlungen (alle 8 Wochen, 4 Termine)
INSERT INTO treatment (frequency, dosage, side_of_eye, treatment_plan_id, surgical_center_time_slot_id, medication_id, version)
SELECT 
    '8 Wochen' as frequency,
    'Standard' as dosage,
    CASE (row_number() OVER (PARTITION BY tp.id) % 3)
        WHEN 0 THEN 'LEFT'
        WHEN 1 THEN 'RIGHT'
        ELSE 'BOTH'
    END as side_of_eye,
    tp.id as treatment_plan_id,
    tsl.id as surgical_center_time_slot_id,
    m.id as medication_id,
    0
FROM treatment_plan tp
JOIN patient p ON tp.patient_id = p.id
JOIN institution i ON p.institution_id = i.id
CROSS JOIN surgical_center sc
CROSS JOIN medication m
CROSS JOIN LATERAL (
    SELECT id
    FROM surgical_center_time_slot 
    WHERE surgical_center_id = sc.id
      AND sc.institution_id = i.id
      AND date >= CURRENT_DATE
      AND date <= (CURRENT_DATE + INTERVAL '32 weeks')
      AND EXTRACT(DOW FROM date) IN (3, 5)  -- Mittwoch oder Freitag
      AND is_available = TRUE
    ORDER BY date
    LIMIT 4
) tsl
WHERE i.institution_code IN ('DEV-TEST', 'PRAX-001')
ON CONFLICT DO NOTHING;

