# Repository-Struktur

Diese Datei dokumentiert die geplante und tatsächliche Struktur des Repositories.

## 📁 Geplante Struktur

```
pvs/
├── docs/                          # Dokumentation
│   ├── deployment/                 # Deployment-Anleitungen
│   │   ├── HETZNER_COMPLETE_SETUP.md    # ⭐ Haupt-Anleitung Hetzner
│   │   ├── HETZNER_SETUP.md             # (Alt, wird ersetzt)
│   │   ├── QUICKSTART.md
│   │   ├── README.md
│   │   └── ...
│   ├── security/                   # Sicherheits-Anleitungen (NEU)
│   │   ├── SSH_KEY_SETUP.md       # SSH-Key generieren
│   │   ├── SSH_KEY_CLEANUP.md     # SSH-Key aus Git entfernen
│   │   ├── DATABASE_PASSWORD_CHANGE.md  # DB-Passwort ändern
│   │   └── SECURITY_INCIDENT.md   # Was tun bei Sicherheitsvorfall
│   └── administration/            # Server-Administration (NEU)
│       ├── BACKUP_RESTORE.md
│       ├── MONITORING.md
│       └── TROUBLESHOOTING.md
│
├── scripts/                        # Alle Scripts (NEU)
│   ├── deployment/                 # Deployment-Scripts
│   │   ├── setup-server.sh        # Server-Grundsetup
│   │   ├── init-databases.sh      # Datenbank-Initialisierung
│   │   └── backup-db.sh           # Datenbank-Backup
│   ├── security/                   # Security-Scripts
│   │   ├── cleanup-ssh-key.sh     # SSH-Key aus Git entfernen
│   │   └── generate-new-ssh-key.sh # Neuen SSH-Key generieren
│   └── utilities/                  # Utility-Scripts
│       └── check-ip.sh
│
├── docker-compose.production.yml   # Production Docker Compose
├── docker-compose.dev.yml          # Development Docker Compose
├── docker-compose.yml              # Lokales Development
├── Dockerfile                      # Application Dockerfile
├── README.md                       # Haupt-README
└── ...
```

## 🔄 Reorganisations-Plan

### Phase 1: Neue Verzeichnisse erstellen

```bash
mkdir -p docs/security
mkdir -p docs/administration
mkdir -p scripts/deployment
mkdir -p scripts/security
mkdir -p scripts/utilities
```

### Phase 2: Dateien verschieben/konsolidieren

#### Security-Dokumentation:
- `SSH_KEY_CLEANUP_ANLEITUNG.md` → `docs/security/SSH_KEY_CLEANUP.md`
- `SSH_KEY_CLEANUP_QUICKSTART.md` → Entfernen (Inhalt in Haupt-Datei)
- `SSH_KEY_CLEANUP_WINDOWS.md` → Abschnitt in `docs/security/SSH_KEY_SETUP.md`
- `SSH_SETUP_ANLEITUNG.md` → `docs/security/SSH_KEY_SETUP.md`
- `setup-ssh-windows.md` → Entfernen (Inhalt in Haupt-Datei)
- `SECURITY_CLEANUP_CHECKLIST.md` → `docs/security/SECURITY_INCIDENT.md`

#### Deployment-Dokumentation:
- `SCHNELLSTART.md` → `docs/deployment/QUICKSTART.md` (ersetzt)
- `SETUP_CHECKLIST.md` → Entfernen (Inhalt in HETZNER_COMPLETE_SETUP.md)
- `DEPLOYMENT_START.md` → Entfernen oder in README integrieren
- `START_HIER.md` → Entfernen (README.md zeigt den Start)

#### Security-Scripts:
- `cleanup-ssh-key.sh` → `scripts/security/cleanup-ssh-key.sh`
- `generate-new-ssh-key.sh` → `scripts/security/generate-new-ssh-key.sh`
- `generate-new-ssh-key.ps1` → `scripts/security/generate-new-ssh-key.ps1`
- `ssh-key-setup.ps1` → Entfernen (obsolet)

#### Deployment-Scripts:
- `setup-server.sh` → `scripts/deployment/setup-server.sh`
- `init-databases.sh` → `scripts/deployment/init-databases.sh`
- `check-ip.sh` → `scripts/utilities/check-ip.sh`

#### Administration-Dokumentation:
- `passwort-wechseln.md` → `docs/security/DATABASE_PASSWORD_CHANGE.md`
- `server-setup-nach-passwort.md` → Entfernen (veraltet)
- `CLEANUP_MANUAL.md` → Entfernen (Inhalt in SSH_KEY_CLEANUP.md)

#### Architektur-Dokumentation:
- `DATABASE_ARCHITECTURE_DECISION.md` → `docs/deployment/DATABASE_ARCHITECTURE.md`

### Phase 3: Neue Dokumente erstellen

- `docs/security/SSH_KEY_SETUP.md` - Konsolidierte SSH-Key Anleitung (Linux + Windows)
- `docs/security/SSH_KEY_CLEANUP.md` - Konsolidierte Cleanup-Anleitung
- `docs/administration/BACKUP_RESTORE.md` - Backup/Restore Anleitung
- `docs/administration/TROUBLESHOOTING.md` - Troubleshooting-Guide

### Phase 4: README aktualisieren

- Links zu neuen Pfaden aktualisieren
- Struktur dokumentieren
- Quick-Links hinzufügen

## ✅ Nach der Reorganisation

- ✅ Klare Struktur nach Themenbereichen
- ✅ Keine verstreuten MD-Dateien im Root
- ✅ Scripts logisch gruppiert
- ✅ Einfach zu finden für neue Entwickler
- ✅ Konsolidierte Dokumentation (weniger Duplikate)

