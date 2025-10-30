# Multi-Tenancy – Plan zur Umsetzung und Datenmodell-Verbesserungen

## Zielbild
- Jede Einrichtung (Praxis/MVZ/Klinik) besitzt einen strikt abgeschotteten Datenbestand.
- Login erfordert zusätzlich zum Benutzernamen/Kennwort einen Einrichtungsbezeichner (z. B. zufällige Kennnummer oder kurzer Slug).
- Rollen-/Rechtesystem wird mandantenfähig: globaler Admin (legt Einrichtungen an), mandantenlokaler Admin (verwaltet Benutzer/Rollen im eigenen Mandanten), Fachrollen pro Mandant.

## Architektur-Ansatz
- Modus: „Single Database, Shared Schema“ mit Mandanten-Spalte (`tenant_id`) auf allen mandantenbezogenen Tabellen.
  - Gründe: geringster Aufwand, konsistente Transaktionen über Entities, einfacher Betrieb; gute Startlösung mit klarer Migrationsstrategie.
  - Alternativen (für später): Schema-per-Tenant oder Database-per-Tenant (höhere Isolation, mehr Betriebsaufwand).
- Technisch: Spring Security + JPA/Hibernate-Tenant-Filter.
  - `TenantContext` (ThreadLocal) wird beim Request gesetzt (aus Login/JWT/Session) und in Repositories/Queries automatisch erzwungen (Hibernate `@Filter` oder JPA-Specs/AOP-Schicht).
  - Service-Layer validiert zusätzlich den Mandantenkontext für Schreib-/Lesevorgänge (Defense in Depth).

## Datenmodell-Änderungen
- Neue Tabelle: `tenant` (z. B. `praxis`).
  - Spalten: `id` (UUID PK), `code` (unique, kurzer Slug oder zufälliger Code), `name`, `created_at`, `status`.
- Benutzerbezug:
  - `user` erhält Pflicht-FK `tenant_id` (User ist genau einem Mandanten zugeordnet).
  - Unique-Constraint: `(tenant_id, username)`.
  - Rollen: `role` bleibt global definiert; `user_roles` wird pro Tenant aufgelöst oder `role_assignment` mit `(tenant_id, user_id, role)`.
- Patienten- und Fachdaten:
  - Jede mandantenbezogene Tabelle erhält `tenant_id` (FK -> `tenant`).
  - Eindeutigkeiten werden mandantenlokal: z. B. `patient` mit Unique `(tenant_id, external_id)` oder `(tenant_id, lastname, birthdate, ... – je nach Domänenentscheidung)`.
  - Alle FK-Beziehungen bleiben wie bisher, zusätzlich gilt: Eltern/Kind teilen denselben `tenant_id` (integritätsprüfend per FK + CHECKs in Service-Layer).
- Indizes/Constraints:
  - Für alle häufig gefilterten Tabellen zusammengesetzte Indizes auf `(tenant_id, <fachliche_schlüssel>)`.
  - Prüfen: Migrationsreihenfolge so wählen, dass Backfill performant ist (Batchgrößen, Indizes temporär deaktiviert/versetzt anlegen, je nach DB).

## Authentifizierung/Autorisierung
- Login: Eingabefelder `tenantCode` + `username` + `password`.
  - `tenantCode` wird auf `tenant.code` gemappt, `tenant_id` wird im Token/Session mitgeführt.
- JWT/Session:
  - Enthält `tenant_id` und `roles` des Users in diesem Tenant.
  - Globaler Admin authentifiziert ohne `tenant_id`, wechselt bei Bedarf kontextuell (z. B. per Admin-UI „Impersonate Tenant“ oder expliziter Auswahl).
- Zugriffsschutz:
  - Request-Filter setzt `TenantContext` aus Token.
  - Repositories erzwingen `tenant_id` automatisch (Filter/Specification/Soft-Scope).
  - Service-Methoden validieren zusätzlich (kein Cross-Tenant durch ID-Raten).

## Migrationsstrategie (Bestandsdaten)
1. `tenant` anlegen und „Default“-Mandant erzeugen.
2. `tenant_id` als nullable in alle relevanten Tabellen hinzufügen.
3. Backfill: Setze `tenant_id` für alle existierenden Datensätze auf „Default“ in performanten Batches.
4. `user` zuordnen: Alle bestehenden Benutzer dem Default-Mandanten zuweisen; Unique-Constraints auf `(tenant_id, username)` anlegen.
5. Non-null & FK-Constraints aktivieren, wenn Backfill abgeschlossen ist.
6. Indizes auf `(tenant_id, …)` anlegen.
7. Applikationscode ausrollen, der `tenant_id` erzwingt; Read-Only-Smoke-Test je Tenant.

## Entwicklungsaufgaben (Inkremente)
- Login/Token:
  - UI: zusätzliches Feld `tenantCode`; Server: Resolve `tenant_id`, sign JWT mit `tenant_id`.
- Infrastruktur:
  - `TenantContext` + Servlet-Filter + Resolver aus Security-Kontext.
  - Hibernate-Filter oder Repositories via `@EntityGraph`/`Specification` immer mit `tenant_id` ergänzen.
- Datenmodell + Migration:
  - Flyway/Liquibase-Migrationen: `tenant`-Tabelle, Spalten `tenant_id`, Constraints, Indizes, Backfill-Script.
- Domänenlogik:
  - Service-Layer erhält Guard-Methoden `assertSameTenant(entityOrId)`.
  - Repository-API: Nur `findByIdAndTenantId`/`findAllByTenantId...` oder globale Specs benutzen.
- Rollenmodell:
  - `GLOBAL_ADMIN` (Mandanten anlegen, initiales Kennwort setzen).
  - `TENANT_ADMIN` (Benutzer/Rollen im eigenen Mandanten verwalten).
  - Fachrollen (lesen/schreiben) wie bisher, aber mandantenlokal angewandt.
- Tests:
  - Auth-Flow-Tests für Login mit `tenantCode`.
  - Query-Tests: Alle Repositories liefern nur Daten des gesetzten `tenant_id`.
  - Autorisierungstests: Cross-Tenant-Zugriff ist verboten (403/404).

## Sicherheitsaspekte
- Defense in depth: Filter in Datenbankzugriff + Service-Guards + Security-Kontext.
- Auditing: Jede Audit-Log-Zeile speichert `tenant_id` mit.
- Exporte/Dateien: Pfad/Storage immer mit `tenant_id` isolieren.
- Async/Batch/Events: `TenantContext` propagieren (Decorator/Executor), um Leaks zu verhindern.

## Ausblick/Skalierung
- Bei starkem Wachstum: Umstieg auf Schema-per-Tenant.
- Sharding-Strategien vorbereiten (mit `tenant_id` als Partition Key).

## Akzeptanzkriterien
- Kein Cross-Tenant-Read/Write möglich (technisch erzwungen, Tests vorhanden).
- Login mit `tenantCode` funktioniert; globaler Admin kann neue Tenants anlegen.
- Eindeutigkeit von Patienten und anderen Entities ist mandantenlokal gewährleistet.
- Migration erhält alle Bestandsdaten konsistent und ordnet sie einem Default-Mandanten zu.
