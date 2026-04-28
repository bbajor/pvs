# IAM Rollenmatrix (Cloud Run + LB + IAP + Firebase)

Ziel: minimale, trennscharfe Rechte fuer Betrieb mit Patientendaten.

## 0) Projektkonkrete Werte (bitte ausfuellen)

| Key | Wert |
|---|---|
| GCP Project ID | `<project-id>` |
| GCP Project Number | `<project-number>` |
| Region | `europe-west3` |
| Cloud Run Service | `pvs-api` |
| LB Backend Service Name | `<lb-backend-service-name>` |
| Google Workspace Domain | `<domain>` |
| Firebase Project ID | `<firebase-project-id>` |

Empfehlung: Diese Tabelle vor dem ersten produktiven Rollout mit echten Werten befuellen und im Change-Dokument referenzieren.

## 1) Identitaeten (empfohlene Struktur)

Google Groups:
- `pvs-medical-users@<domain>`: medizinisches Fachpersonal (IAP-Zugang)
- `pvs-practice-admins@<domain>`: Praxisadministration (IAP-Zugang)
- `pvs-security-admins@<domain>`: Security/Plattform-Verantwortung
- `pvs-deployers@<domain>`: CI/CD-Operatoren (wenn manuell)
- `pvs-auditors@<domain>`: read-only Audit

Service Accounts:
- `gha-deploy@<project>.iam.gserviceaccount.com`: GitHub Actions Deploy
- `iap-backend-invoker@<project>.iam.gserviceaccount.com`: IAP Service Identity (managed)
- `cloud-run-runtime@<project>.iam.gserviceaccount.com`: Runtime Identity fuer `pvs-api`

## 2) Rollenmatrix

| Subjekt | Scope | Rollen | Warum |
|---|---|---|---|
| `pvs-medical-users` | IAP-gesichertes Backend | `roles/iap.httpsResourceAccessor` | App-Zugang nur ueber IAP |
| `pvs-practice-admins` | IAP-gesichertes Backend | `roles/iap.httpsResourceAccessor` | Admin-Zugang nur ueber IAP |
| `pvs-security-admins` | Project | `roles/iap.admin`, `roles/compute.securityAdmin`, `roles/run.admin`, `roles/secretmanager.admin` | IAP/LB/Security und Incident-Faehigkeit |
| `pvs-auditors` | Project | `roles/logging.viewer`, `roles/monitoring.viewer`, `roles/cloudasset.viewer` | Audit ohne Schreibrechte |
| `gha-deploy` | Project | `roles/run.admin`, `roles/artifactregistry.writer`, `roles/cloudbuild.builds.editor`, `roles/iam.serviceAccountUser`, `roles/firebasehosting.admin` | Deploy API + Frontend |
| `cloud-run-runtime` | Secret Manager | `roles/secretmanager.secretAccessor` (nur konkrete Secrets) | Laufzeit-Secrets lesen, sonst nichts |
| IAP Service Identity | Cloud Run Service `pvs-api` | `roles/run.invoker` | Nur IAP darf Backend aufrufen |

Hinweis:
- `roles/owner` und `roles/editor` fuer Produktionsbetrieb vermeiden.
- Secret-Zugriff immer auf konkrete Secret-Ressourcen begrenzen.

## 2.1 Soll-Ist-Matrix (ausfuellen und freigeben)

| Subjekt | Soll-Rollen | Ist-Rollen | Status |
|---|---|---|---|
| `group:pvs-medical-users@<domain>` | `roles/iap.httpsResourceAccessor` | `<eintragen>` | ☐ |
| `group:pvs-practice-admins@<domain>` | `roles/iap.httpsResourceAccessor` | `<eintragen>` | ☐ |
| `group:pvs-security-admins@<domain>` | `roles/iap.admin`, `roles/compute.securityAdmin`, `roles/run.admin`, `roles/secretmanager.admin` | `<eintragen>` | ☐ |
| `group:pvs-auditors@<domain>` | `roles/logging.viewer`, `roles/monitoring.viewer`, `roles/cloudasset.viewer` | `<eintragen>` | ☐ |
| `serviceAccount:gha-deploy@<project>.iam.gserviceaccount.com` | `roles/run.admin`, `roles/artifactregistry.writer`, `roles/cloudbuild.builds.editor`, `roles/firebasehosting.admin`, `roles/iam.serviceAccountUser` | `<eintragen>` | ☐ |
| `serviceAccount:cloud-run-runtime@<project>.iam.gserviceaccount.com` | `roles/secretmanager.secretAccessor` (nur Secret-Resource-Ebene) | `<eintragen>` | ☐ |
| `serviceAccount:service-<project-number>@gcp-sa-iap.iam.gserviceaccount.com` | `roles/run.invoker` auf `pvs-api` | `<eintragen>` | ☐ |

## 3) Konkrete `gcloud`-Befehle

Variablen:

```bash
PROJECT_ID="<project-id>"
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
REGION="europe-west3"
CLOUD_RUN_SERVICE="pvs-api"
BACKEND_SERVICE="<lb-backend-service-name>"
IAP_BRAND="<iap-brand-id>"
```

### 3.1 IAP-Zugang fuer Nutzergruppen

```bash
gcloud iap web add-iam-policy-binding \
  --project "$PROJECT_ID" \
  --resource-type=backend-services \
  --service "$BACKEND_SERVICE" \
  --member="group:pvs-medical-users@<domain>" \
  --role="roles/iap.httpsResourceAccessor"

gcloud iap web add-iam-policy-binding \
  --project "$PROJECT_ID" \
  --resource-type=backend-services \
  --service "$BACKEND_SERVICE" \
  --member="group:pvs-practice-admins@<domain>" \
  --role="roles/iap.httpsResourceAccessor"
```

### 3.2 Runtime-Invocation nur via IAP

```bash
# IAP Service Identity sicherstellen
gcloud beta services identity create \
  --service=iap.googleapis.com \
  --project="$PROJECT_ID"

# IAP-Servicekonto bekommt run.invoker auf pvs-api
gcloud run services add-iam-policy-binding "$CLOUD_RUN_SERVICE" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-iap.iam.gserviceaccount.com" \
  --role="roles/run.invoker"
```

### 3.3 GitHub Actions Deploy SA (Workload Identity)

```bash
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:gha-deploy@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:gha-deploy@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:gha-deploy@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/cloudbuild.builds.editor"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:gha-deploy@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/firebasehosting.admin"

# fuer --service-account beim Cloud Run Deploy
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:gha-deploy@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"
```

### 3.4 Secret-Zugriff nur fuer Runtime-SA

```bash
gcloud secrets add-iam-policy-binding OIDC_ISSUER_URI \
  --project "$PROJECT_ID" \
  --member="serviceAccount:cloud-run-runtime@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

Muster fuer alle produktiven Secrets wiederholen, nicht pauschal auf Projektebene vergeben.

## 4) Validierung (Pflicht vor Go-Live)

- [ ] Unauthentifizierter Zugriff auf Cloud Run URL ist nicht moeglich.
- [ ] Zugriff ueber LB ohne IAP-Session ist blockiert.
- [ ] User aus `pvs-medical-users` kommen durch IAP bis zur App.
- [ ] User ausserhalb freigegebener Gruppen werden abgewiesen.
- [ ] `gha-deploy` kann deployen, aber keine Owner/Editor-Rechte.
- [ ] Runtime-SA sieht nur benoetigte Secrets.

## 5) Betriebshinweise

- Gruppenmitgliedschaften regelmaessig rezertifizieren (mind. quartalsweise).
- Break-glass-Konto getrennt dokumentieren und MFA-zwingen.
- IAM-Aenderungen als Change mit 4-Augen-Prinzip behandeln.
