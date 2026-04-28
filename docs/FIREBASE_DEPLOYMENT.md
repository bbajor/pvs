# Firebase Deployment (Frontend) + Cloud Run (Backend)

This setup keeps the current stack:
- Frontend: React (Vite) on Firebase Hosting
- Backend: Spring Boot API on Cloud Run

No backend tech stack conversion is required.

## Compliance Scope

This document prepares a deployment baseline for DSGVO and SGB V oriented operation.
It is a technical hardening guide, not legal advice.

## 1) Backend deploy to Cloud Run (IAP-ready)

Build and push the existing backend container image, then deploy it:

```bash
gcloud builds submit --tag gcr.io/<PROJECT_ID>/pvs-api:latest
gcloud run deploy pvs-api \
  --image gcr.io/<PROJECT_ID>/pvs-api:latest \
  --region europe-west3 \
  --ingress internal-and-cloud-load-balancing \
  --no-allow-unauthenticated \
  --set-env-vars \
OIDC_ISSUER_URI=<ISSUER_URI>,OIDC_ROLES_CLAIM=roles,SINGLE_PRACTICE_ENABLED=true,CORS_ALLOWED_ORIGINS=https://<FIREBASE_HOST>
```

Use an EU region only (`europe-west3` or equivalent approved region).
Cloud Run must use the same region policy as your data processing agreement.
Do not expose backend directly to internet; use External HTTPS LB + IAP.

## 2) Frontend build for Firebase Hosting

Set frontend env vars (example):

```bash
cd frontend
cp .env.example .env.production
```

Fill at least:
- `VITE_OIDC_AUTHORITY`
- `VITE_OIDC_CLIENT_ID`
- `VITE_OIDC_REDIRECT_URI` (Firebase domain + `/auth/callback`)
- `VITE_OIDC_POST_LOGOUT_REDIRECT_URI`
- `VITE_API_BASE_URL` can stay empty when using Firebase Hosting rewrite to Cloud Run.
- If set explicitly, use only approved EU endpoint.

Build:

```bash
npm ci
npm run build
```

## 3) Firebase Hosting deploy

At repo root:

```bash
cp .firebaserc.example .firebaserc
# edit project id
firebase login
firebase deploy --only hosting
```

`firebase.json` is configured for:
- SPA routing (`** -> /index.html`)
- `/api/**` rewrite to Cloud Run service `pvs-api`
- strict security headers (HSTS, CSP baseline, XFO, nosniff, permissions policy)

## 3.1 CI/CD workflow (GitHub Actions)

Workflow file:
- `.github/workflows/deploy-gcp-firebase.yml`

Inputs:
- `environment`: `test` or `prod`
- `deploy_backend`: deploy Cloud Run API
- `deploy_frontend`: deploy Firebase Hosting
- `dry_run`: validate/build only, no productive deploy

Required GitHub environment secrets:
- `GCP_WORKLOAD_IDENTITY_PROVIDER`
- `GCP_SERVICE_ACCOUNT_EMAIL`
- `GCP_PROJECT_ID`
- `GCP_REGION`
- `GCP_CLOUD_RUN_SERVICE`
- `GCP_ARTIFACT_REGISTRY_HOST`
- `GCP_ARTIFACT_REGISTRY_REPO`
- `OIDC_ISSUER_URI`
- `OIDC_ROLES_CLAIM`
- `FIREBASE_PROJECT_ID`
- `VITE_OIDC_AUTHORITY`
- `VITE_OIDC_CLIENT_ID`
- `VITE_OIDC_REDIRECT_URI`
- `VITE_OIDC_SCOPE`
- `VITE_OIDC_POST_LOGOUT_REDIRECT_URI`

Recommended first rollout sequence:
1. `dry_run=true`, `deploy_backend=true`, `deploy_frontend=true`
2. verify Cloud Build image creation and frontend build log
3. set `dry_run=false` for `test`
4. functional test in test environment
5. set `dry_run=false` for `prod` after approval

## 4) OIDC callback URLs

In your IdP client config, allow:
- `https://<your-firebase-host>/auth/callback`
- `https://<your-firebase-host>/`

If your IdP uses additional endpoints/domains, keep CSP `connect-src` aligned.

## 5) Single-practice mode

The backend now supports single-practice mode:
- `SINGLE_PRACTICE_ENABLED=true`
- `SINGLE_PRACTICE_AUTO_PROVISION=true`

On startup the app ensures exactly one active institution.

## 6) DSGVO / SGB V hardening checklist

### Mandatory before production

- EU-only regions for all services:
  - Firebase Hosting site (EU project governance)
  - Cloud Run backend (`europe-west3` recommended)
  - Database in EU region only
- AVV/DPA and TOM documentation with all processors in place.
- No PII in logs:
  - app logs reviewed
  - access logs retention minimized and documented
- OIDC in production only (no local/dev auth mode exposed).
- Tenant/scope enforcement active (`/api/v1/**` requires institution context).
- Security headers active (`firebase.json`).
- External HTTPS Load Balancer + IAP in front of Cloud Run.
- HTTPS only, HSTS enabled.
- Backup/restore process tested and documented.
- Incident response + breach notification process documented.

### Recommended

- Cloud Armor / rate limiting for API edge.
- VPC egress restrictions and private DB connectivity.
- KMS-managed secrets, no static secrets in repo.
- Regular dependency and container vulnerability scans.


