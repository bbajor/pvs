-- Test data migration for dev and test environments
-- This script only runs in dev/test profiles, not in production
-- 
-- NOTE: This script should be skipped in production environments.
-- Flyway will execute this script, but it should only contain test data
-- that is safe to run in dev/test environments.
--
-- Daten basieren auf TestDataInitializer.java
-- 10 Institutionen mit je ca. 100 Patienten

-- ============================================
-- INSTITUTIONEN
-- ============================================

-- Insert 10 test institutions
INSERT INTO institution (institution_code, institution_name, description, active, database_name, container_name)
VALUES 
    ('PRAX-001', 'Augenarztpraxis Dr. Müller', 'Leere Test-Institution für Neuaufbau', TRUE, 'pvs_inst_prax_001', 'postgres-inst-prax-001'),
    ('PRAX-002', 'MVZ Augenheilkunde Hamburg', 'Vollständige Test-Institution mit Patienten und Behandlungen', TRUE, 'pvs_inst_prax_002', 'postgres-inst-prax-002'),
    ('PRAX-003', 'Augenzentrum Berlin', 'Test-Institution Berlin', TRUE, 'pvs_inst_prax_003', 'postgres-inst-prax-003'),
    ('PRAX-004', 'Augenpraxis München', 'Test-Institution München', TRUE, 'pvs_inst_prax_004', 'postgres-inst-prax-004'),
    ('PRAX-005', 'MVZ Augenheilkunde Köln', 'Test-Institution Köln', TRUE, 'pvs_inst_prax_005', 'postgres-inst-prax-005'),
    ('PRAX-006', 'Augenarztpraxis Frankfurt', 'Test-Institution Frankfurt', TRUE, 'pvs_inst_prax_006', 'postgres-inst-prax-006'),
    ('PRAX-007', 'Netzhautzentrum Stuttgart', 'Test-Institution Stuttgart', TRUE, 'pvs_inst_prax_007', 'postgres-inst-prax-007'),
    ('PRAX-008', 'Augenklinik Düsseldorf', 'Test-Institution Düsseldorf', TRUE, 'pvs_inst_prax_008', 'postgres-inst-prax-008'),
    ('PRAX-009', 'Augenpraxis Leipzig', 'Test-Institution Leipzig', TRUE, 'pvs_inst_prax_009', 'postgres-inst-prax-009'),
    ('PRAX-010', 'MVZ Augenheilkunde Dresden', 'Test-Institution Dresden', TRUE, 'pvs_inst_prax_010', 'postgres-inst-prax-010')
ON CONFLICT (institution_code) DO NOTHING;

-- ============================================
-- LOCATIONS (je Institution 2 Standorte)
-- ============================================

-- Locations für PRAX-001
INSERT INTO location (location_name, street, house_number, postal_code, city, country, owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, active, institution_id, version)
SELECT 
    'Standort 1 - Hauptpraxis',
    'Hauptstraße',
    '42',
    '10115',
    'Berlin',
    'Deutschland',
    'Max Mustermann',
    'Dr. med.',
    '123456789',
    '987654321',
    '+49 30 12345678',
    '+49 30 12345679',
    'praxis@augenarzt-muster.de',
    'Beispielstandort für die Entwicklungsumgebung. Spezialisiert auf Netzhauterkrankungen und intravitreale Injektionen.',
    TRUE,
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;

INSERT INTO location (location_name, street, house_number, postal_code, city, country, owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, active, institution_id, version)
SELECT 
    'Standort 2 - Filiale',
    'Hauptstraße',
    '42',
    '10115',
    'Berlin',
    'Deutschland',
    'Max Mustermann',
    'Dr. med.',
    '123456789',
    '987654321',
    '+49 30 12345678',
    '+49 30 12345679',
    'praxis@augenarzt-muster.de',
    'Beispielstandort für die Entwicklungsumgebung. Spezialisiert auf Netzhauterkrankungen und intravitreale Injektionen.',
    TRUE,
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;

-- Locations für PRAX-002 bis PRAX-010 (gleiche Struktur)
DO $$
DECLARE
    inst_code TEXT;
    inst_name TEXT;
    city_name TEXT;
    postal_code TEXT;
BEGIN
    FOR i IN 2..10 LOOP
        inst_code := 'PRAX-' || LPAD(i::TEXT, 3, '0');
        
        -- Hole Institution Name und bestimme Stadt basierend auf Code
        SELECT institution_name INTO inst_name FROM institution WHERE institution_code = inst_code;
        
        CASE i
            WHEN 2 THEN city_name := 'Hamburg'; postal_code := '20095';
            WHEN 3 THEN city_name := 'Berlin'; postal_code := '10115';
            WHEN 4 THEN city_name := 'München'; postal_code := '80331';
            WHEN 5 THEN city_name := 'Köln'; postal_code := '50667';
            WHEN 6 THEN city_name := 'Frankfurt'; postal_code := '60311';
            WHEN 7 THEN city_name := 'Stuttgart'; postal_code := '70173';
            WHEN 8 THEN city_name := 'Düsseldorf'; postal_code := '40213';
            WHEN 9 THEN city_name := 'Leipzig'; postal_code := '04109';
            WHEN 10 THEN city_name := 'Dresden'; postal_code := '01067';
        END CASE;
        
        -- Standort 1
        EXECUTE format('
            INSERT INTO location (location_name, street, house_number, postal_code, city, country, owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, active, institution_id, version)
            SELECT 
                ''Standort 1 - Hauptpraxis'',
                ''Hauptstraße'',
                ''42'',
                %L,
                %L,
                ''Deutschland'',
                ''Max Mustermann'',
                ''Dr. med.'',
                ''123456789'',
                ''987654321'',
                ''+49 30 12345678'',
                ''+49 30 12345679'',
                ''praxis@augenarzt-muster.de'',
                ''Beispielstandort für die Entwicklungsumgebung. Spezialisiert auf Netzhauterkrankungen und intravitreale Injektionen.'',
                TRUE,
                i.id,
                0
            FROM institution i
            WHERE i.institution_code = %L
            ON CONFLICT DO NOTHING
        ', postal_code, city_name, inst_code);
        
        -- Standort 2
        EXECUTE format('
            INSERT INTO location (location_name, street, house_number, postal_code, city, country, owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, active, institution_id, version)
            SELECT 
                ''Standort 2 - Filiale'',
                ''Hauptstraße'',
                ''42'',
                %L,
                %L,
                ''Deutschland'',
                ''Max Mustermann'',
                ''Dr. med.'',
                ''123456789'',
                ''987654321'',
                ''+49 30 12345678'',
                ''+49 30 12345679'',
                ''praxis@augenarzt-muster.de'',
                ''Beispielstandort für die Entwicklungsumgebung. Spezialisiert auf Netzhauterkrankungen und intravitreale Injektionen.'',
                TRUE,
                i.id,
                0
            FROM institution i
            WHERE i.institution_code = %L
            ON CONFLICT DO NOTHING
        ', postal_code, city_name, inst_code);
    END LOOP;
END $$;

-- ============================================
-- USER ACCOUNTS
-- ============================================

-- Super admin (no institution)
-- Password: "123" (plain text with {noop} prefix for DelegatingPasswordEncoder)
INSERT INTO user_account (username, password_hash, enabled, email, full_name, institution_id, version)
VALUES 
    ('superadmin', '{noop}123', TRUE, 'admin@pvs.local', 'Super Administrator', NULL, 0)
ON CONFLICT DO NOTHING;

INSERT INTO user_account_roles (user_account_id, roles)
SELECT id, 'SUPER_ADMIN' FROM user_account WHERE username = 'superadmin'
ON CONFLICT DO NOTHING;
INSERT INTO user_account_roles (user_account_id, roles)
SELECT id, 'ADMIN' FROM user_account WHERE username = 'superadmin'
ON CONFLICT DO NOTHING;
INSERT INTO user_account_roles (user_account_id, roles)
SELECT id, 'USER' FROM user_account WHERE username = 'superadmin'
ON CONFLICT DO NOTHING;

-- Institution 1: Nur Institutionsadmin
INSERT INTO user_account (username, password_hash, enabled, email, full_name, institution_id, version)
SELECT 
    'inst1-admin',
    '{noop}123',
    TRUE,
    'inst1-admin@pvs.local',
    'Institution 1 Admin',
    i.id,
    0
FROM institution i
WHERE i.institution_code = 'PRAX-001'
ON CONFLICT (institution_id, username) DO NOTHING;

INSERT INTO user_account_roles (user_account_id, roles)
SELECT ua.id, 'INSTITUTION_ADMIN' FROM user_account ua
JOIN institution i ON ua.institution_id = i.id
WHERE ua.username = 'inst1-admin' AND i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;
INSERT INTO user_account_roles (user_account_id, roles)
SELECT ua.id, 'ADMIN' FROM user_account ua
JOIN institution i ON ua.institution_id = i.id
WHERE ua.username = 'inst1-admin' AND i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;
INSERT INTO user_account_roles (user_account_id, roles)
SELECT ua.id, 'USER' FROM user_account ua
JOIN institution i ON ua.institution_id = i.id
WHERE ua.username = 'inst1-admin' AND i.institution_code = 'PRAX-001'
ON CONFLICT DO NOTHING;

-- Institution 2 bis 10: Vollständige Testdaten mit allen Rollen
DO $$
DECLARE
    inst_code TEXT;
    inst_num TEXT;
BEGIN
    FOR i IN 2..10 LOOP
        inst_code := 'PRAX-' || LPAD(i::TEXT, 3, '0');
        inst_num := i::TEXT;
        
        -- Admin
        INSERT INTO user_account (username, password_hash, enabled, email, full_name, institution_id, version)
        SELECT 
            'inst' || inst_num || '-admin',
            '{noop}123',
            TRUE,
            'inst' || inst_num || '-admin@pvs.local',
            'Institution ' || inst_num || ' Admin',
            inst.id,
            0
        FROM institution inst
        WHERE inst.institution_code = inst_code
        ON CONFLICT (institution_id, username) DO NOTHING;
        
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'ADMIN' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-admin' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'USER' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-admin' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        
        -- Owner
        INSERT INTO user_account (username, password_hash, enabled, email, full_name, institution_id, version)
        SELECT 
            'inst' || inst_num || '-owner',
            '{noop}123',
            TRUE,
            'inst' || inst_num || '-owner@pvs.local',
            'Institution ' || inst_num || ' Owner',
            inst.id,
            0
        FROM institution inst
        WHERE inst.institution_code = inst_code
        ON CONFLICT (institution_id, username) DO NOTHING;
        
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'OWNER' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-owner' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'USER' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-owner' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        
        -- Doctor
        INSERT INTO user_account (username, password_hash, enabled, email, full_name, institution_id, version)
        SELECT 
            'inst' || inst_num || '-doctor',
            '{noop}123',
            TRUE,
            'inst' || inst_num || '-doctor@pvs.local',
            'Institution ' || inst_num || ' Doctor',
            inst.id,
            0
        FROM institution inst
        WHERE inst.institution_code = inst_code
        ON CONFLICT (institution_id, username) DO NOTHING;
        
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'DOCTOR' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-doctor' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'USER' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-doctor' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        
        -- Medical Staff
        INSERT INTO user_account (username, password_hash, enabled, email, full_name, institution_id, version)
        SELECT 
            'inst' || inst_num || '-medical',
            '{noop}123',
            TRUE,
            'inst' || inst_num || '-medical@pvs.local',
            'Institution ' || inst_num || ' Medical Staff',
            inst.id,
            0
        FROM institution inst
        WHERE inst.institution_code = inst_code
        ON CONFLICT (institution_id, username) DO NOTHING;
        
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'MEDICAL_STAFF' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-medical' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'USER' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-medical' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        
        -- Tech User
        INSERT INTO user_account (username, password_hash, enabled, email, full_name, institution_id, version)
        SELECT 
            'inst' || inst_num || '-tech',
            '{noop}123',
            TRUE,
            'inst' || inst_num || '-tech@pvs.local',
            'Institution ' || inst_num || ' Tech User',
            inst.id,
            0
        FROM institution inst
        WHERE inst.institution_code = inst_code
        ON CONFLICT (institution_id, username) DO NOTHING;
        
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'TECH_USER' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-tech' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'USER' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-tech' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
        
        -- User
        INSERT INTO user_account (username, password_hash, enabled, email, full_name, institution_id, version)
        SELECT 
            'inst' || inst_num || '-user',
            '{noop}123',
            TRUE,
            'inst' || inst_num || '-user@pvs.local',
            'Institution ' || inst_num || ' User',
            inst.id,
            0
        FROM institution inst
        WHERE inst.institution_code = inst_code
        ON CONFLICT (institution_id, username) DO NOTHING;
        
        INSERT INTO user_account_roles (user_account_id, roles)
        SELECT ua.id, 'USER' FROM user_account ua
        JOIN institution inst ON ua.institution_id = inst.id
        WHERE ua.username = 'inst' || inst_num || '-user' AND inst.institution_code = inst_code
        ON CONFLICT DO NOTHING;
    END LOOP;
END $$;

-- ============================================
-- HEALTH INSURANCE (je Institution)
-- ============================================

-- Health Insurance für alle Institutionen
DO $$
DECLARE
    inst_code TEXT;
    insurance_names TEXT[] := ARRAY['TK', 'AOK', 'Barmer', 'DAK', 'BKK', 'IKK', 'KKH', 'Debeka'];
    insurance_name TEXT;
BEGIN
    FOR i IN 1..10 LOOP
        inst_code := 'PRAX-' || LPAD(i::TEXT, 3, '0');
        
        -- Erstelle mehrere Krankenkassen pro Institution
        FOREACH insurance_name IN ARRAY insurance_names
        LOOP
            INSERT INTO health_insurance (billing_carrier_name, cost_carrier_name, billing_carrier_id, cost_carrier_id, billing_carrier_country_code, cost_carrier_country_code, institution_id, version)
            SELECT 
                insurance_name,
                insurance_name,
                UPPER(REPLACE(insurance_name, ' ', '-')),
                UPPER(REPLACE(insurance_name, ' ', '-')),
                'DE',
                'DE',
                inst.id,
                0
            FROM institution inst
            WHERE inst.institution_code = inst_code
            ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- ============================================
-- PATIENTEN (je Institution ca. 100)
-- ============================================

-- Patienten für alle Institutionen generieren
DO $$
DECLARE
    inst_code TEXT;
    inst_id BIGINT;
    loc1_id BIGINT;
    loc2_id BIGINT;
    first_names TEXT[] := ARRAY['Anna', 'Max', 'Sophie', 'Felix', 'Lena', 'Jonas', 'Marie', 'Paul', 'Emma', 'Alexander'];
    last_names TEXT[] := ARRAY['Müller', 'Schmidt', 'Schneider', 'Fischer', 'Weber', 'Meyer', 'Wagner', 'Becker', 'Schulz', 'Hoffmann'];
    street_names TEXT[] := ARRAY['Hauptstraße', 'Gartenweg', 'Bahnhofstraße', 'Am Markt', 'Feldweg', 'Waldstraße', 'Schulstraße', 'Kirchplatz', 'Rosenweg', 'Wiesenstraße'];
    cities TEXT[] := ARRAY['Berlin', 'Hamburg', 'München', 'Köln', 'Frankfurt', 'Stuttgart', 'Düsseldorf', 'Leipzig', 'Dortmund', 'Essen', 'Bremen', 'Dresden'];
    mail_providers TEXT[] := ARRAY['gmail.com', 'web.de', 'gmx.de', 'yahoo.com', 't-online.de'];
    insurances TEXT[] := ARRAY['TK', 'AOK', 'Barmer', 'DAK', 'BKK', 'IKK', 'KKH', 'Debeka'];
    first_name TEXT;
    last_name TEXT;
    street TEXT;
    city TEXT;
    postal_code INTEGER;
    phone TEXT;
    email TEXT;
    insurance_name TEXT;
    insurance_id BIGINT;
    birth_date DATE;
    gender CHAR(1);
    patient_num INTEGER;
    location_id BIGINT;
BEGIN
    FOR inst_num IN 1..10 LOOP
        inst_code := 'PRAX-' || LPAD(inst_num::TEXT, 3, '0');
        
        -- Hole Institution ID
        SELECT id INTO inst_id FROM institution WHERE institution_code = inst_code;
        
        -- Hole Location IDs
        SELECT id INTO loc1_id FROM location WHERE institution_id = inst_id AND location_name = 'Standort 1 - Hauptpraxis' LIMIT 1;
        SELECT id INTO loc2_id FROM location WHERE institution_id = inst_id AND location_name = 'Standort 2 - Filiale' LIMIT 1;
        
        -- Generiere ca. 100 Patienten pro Institution
        FOR patient_num IN 1..100 LOOP
            -- Zufällige Werte
            first_name := first_names[1 + (patient_num * 7) % array_length(first_names, 1)];
            last_name := last_names[1 + (patient_num * 11) % array_length(last_names, 1)];
            street := street_names[1 + (patient_num * 13) % array_length(street_names, 1)] || ' ' || (1 + (patient_num * 17) % 100)::TEXT;
            city := cities[1 + (patient_num * 19) % array_length(cities, 1)];
            postal_code := 10000 + (patient_num * 23) % 90000;
            phone := '+49 ' || (130 + (patient_num * 29) % 70)::TEXT || ' ' || LPAD(((patient_num * 31) % 100000000)::TEXT, 8, '0');
            email := 'patient' || LPAD(patient_num::TEXT, 6, '0') || '@' || mail_providers[1 + (patient_num * 37) % array_length(mail_providers, 1)];
            insurance_name := insurances[1 + (patient_num * 41) % array_length(insurances, 1)];
            
            -- Geburtsdatum: zwischen 30 und 90 Jahren
            birth_date := CURRENT_DATE - INTERVAL '1 year' * (30 + (patient_num * 43) % 60) - INTERVAL '1 day' * ((patient_num * 47) % 365);
            
            -- Geschlecht: abwechselnd
            gender := CASE WHEN patient_num % 2 = 0 THEN 'W' ELSE 'M' END;
            
            -- Location: abwechselnd zwischen beiden Standorten
            location_id := CASE WHEN patient_num % 2 = 0 THEN loc1_id ELSE loc2_id END;
            
            -- Hole Health Insurance ID
            SELECT id INTO insurance_id FROM health_insurance 
            WHERE institution_id = inst_id 
            AND cost_carrier_name = insurance_name 
            LIMIT 1;
            
            -- Insert Patient
            EXECUTE format('
                INSERT INTO patient (
                    first_name, last_name, birth, patient_street, patient_house_no, 
                    patient_postal_code, patient_city, patient_country, gender, 
                    phone, email, insurance_number, location_id, institution_id, 
                    health_insurance_id, version
                )
                VALUES (
                    %L,
                    %L,
                    %L,
                    %L,
                    %L,
                    %s,
                    %L,
                    ''DE'',
                    %L,
                    %L,
                    %L,
                    %L,
                    %s,
                    %s,
                    %s,
                    0
                )
                ON CONFLICT (institution_id, first_name, last_name, birth) DO NOTHING
            ',
                first_name,
                last_name,
                birth_date,
                SPLIT_PART(street, ' ', 1),
                SPLIT_PART(street, ' ', 2),
                postal_code,
                city,
                gender,
                phone,
                email,
                UPPER(SUBSTRING(first_name, 1, 1)) || LPAD(patient_num::TEXT, 9, '0'),
                location_id,
                inst_id,
                insurance_id
            );
        END LOOP;
    END LOOP;
END $$;

-- ============================================
-- MEDIKAMENTE (aus TestDataInitializer)
-- ============================================

-- 8 Medikamente aus TestDataInitializer
INSERT INTO medication (
    arzneimittelbezeichnung,
    wirkstoffe,
    darreichungsform,
    indikation_atc,
    anwendungsart,
    description,
    zulassungsinhaber,
    is_favourite,
    valid_from,
    valid_until,
    version
)
VALUES 
    ('Eylea', 'Aflibercept', 'Injektionslösung', 'Feuchte altersbedingte Makuladegeneration', 'intravitreal', '40mg/ml Injektionslösung', 'Pharma GmbH', FALSE, CURRENT_DATE - INTERVAL '2 years', CURRENT_DATE + INTERVAL '5 years', 0),
    ('Lucentis', 'Ranibizumab', 'Injektionslösung', 'Diabetisches Makulaödem', 'intravitreal', '50mg/ml Injektionslösung', 'Pharma GmbH', TRUE, CURRENT_DATE - INTERVAL '3 years', CURRENT_DATE + INTERVAL '4 years', 0),
    ('Avastin', 'Bevacizumab', 'Injektionslösung', 'Glaukom', 'intravitreal', '60mg/ml Injektionslösung', 'Pharma GmbH', FALSE, CURRENT_DATE - INTERVAL '1 year', CURRENT_DATE + INTERVAL '6 years', 0),
    ('Beovu', 'Brolucizumab', 'Injektionslösung', 'Venenverschluss der Netzhaut', 'intravitreal', '70mg/ml Injektionslösung', 'Pharma GmbH', TRUE, CURRENT_DATE - INTERVAL '4 years', CURRENT_DATE + INTERVAL '3 years', 0),
    ('Ozurdex', 'Dexamethason', 'Injektionslösung', 'Visusbedrohende Uveitis', 'intravitreal', '80mg/ml Injektionslösung', 'Pharma GmbH', FALSE, CURRENT_DATE - INTERVAL '2 years', CURRENT_DATE + INTERVAL '5 years', 0),
    ('Iluvien', 'Fluocinolonacetonid', 'Injektionslösung', 'Retinale Ischämie', 'intravitreal', '90mg/ml Injektionslösung', 'Pharma GmbH', TRUE, CURRENT_DATE - INTERVAL '3 years', CURRENT_DATE + INTERVAL '4 years', 0),
    ('Jetrea', 'Ocriplasmin', 'Injektionslösung', 'Netzhautablösung', 'intravitreal', '100mg/ml Injektionslösung', 'Pharma GmbH', FALSE, CURRENT_DATE - INTERVAL '1 year', CURRENT_DATE + INTERVAL '6 years', 0),
    ('Visudyne', 'Verteporfin', 'Injektionslösung', 'Zentrale seröse Chorioretinopathie', 'intravitreal', '110mg/ml Injektionslösung', 'Pharma GmbH', TRUE, CURRENT_DATE - INTERVAL '2 years', CURRENT_DATE + INTERVAL '5 years', 0)
ON CONFLICT DO NOTHING;

-- ============================================
-- DIAGNOSEN (aus TestDataInitializer)
-- ============================================

-- 8 Diagnosen aus TestDataInitializer
INSERT INTO diagnosis (name, icd_code, description, version)
VALUES 
    ('Feuchte altersbedingte Makuladegeneration', 'H35.3', 'Diagnose für Feuchte altersbedingte Makuladegeneration', 0),
    ('Diabetisches Makulaödem', 'H35.0', 'Diagnose für Diabetisches Makulaödem', 0),
    ('Glaukom', 'H40.1', 'Diagnose für Glaukom', 0),
    ('Venenverschluss der Netzhaut', 'H43.8', 'Diagnose für Venenverschluss der Netzhaut', 0),
    ('Visusbedrohende Uveitis', 'H35.7', 'Diagnose für Visusbedrohende Uveitis', 0),
    ('Retinale Ischämie', 'H34.8', 'Diagnose für Retinale Ischämie', 0),
    ('Netzhautablösung', 'H33.2', 'Diagnose für Netzhautablösung', 0),
    ('Zentrale seröse Chorioretinopathie', 'H35.81', 'Diagnose für Zentrale seröse Chorioretinopathie', 0)
ON CONFLICT DO NOTHING;
