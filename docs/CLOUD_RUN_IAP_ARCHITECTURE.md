# Cloud Run + IAP Architecture (EU, External Access)

Goal: access from anywhere, no on-prem IT operation, while keeping patient data protected.

## Target architecture

- Frontend: Firebase Hosting (static React app)
- API: Cloud Run service `pvs-api`
- Entry point: External HTTPS Load Balancer
- Zero Trust gate: Identity-Aware Proxy (IAP) in front of Cloud Run
- Optional edge protection: Cloud Armor
- Data layer: Cloud SQL PostgreSQL in EU region

## Region policy (mandatory)

- Use EU regions only.
- Recommended baseline:
  - Cloud Run: `europe-west3`
  - Cloud SQL: `europe-west3`
  - Backup bucket (if custom): EU dual/multi region according to DPA

## Cloud Run deployment baseline

```bash
gcloud run deploy pvs-api \
  --image gcr.io/<PROJECT_ID>/pvs-api:latest \
  --region europe-west3 \
  --ingress internal-and-cloud-load-balancing \
  --no-allow-unauthenticated \
  --set-env-vars \
OIDC_ISSUER_URI=<ISSUER_URI>,OIDC_ROLES_CLAIM=roles,SINGLE_PRACTICE_ENABLED=true
```

## HTTPS Load Balancer + IAP

1. Create backend service targeting Cloud Run (serverless NEG).
2. Configure HTTPS frontend (managed certificate + custom domain).
3. Enable IAP on backend service.
4. Grant access only to required medical/administrative identity groups.

This gives internet reachability but blocks unauthenticated traffic before app code.

Detailed IAM role mapping (groups + service accounts + least privilege):
- `docs/IAM_ROLLENMATRIX_GCP.md`

## Identity model

- IAP: coarse access (who may enter the app)
- App OIDC + roles + institution guard: fine-grained permissions (what user may do)

## Security controls checklist

- [ ] IAP enabled, no public backend bypass
- [ ] Cloud Run unauthenticated access disabled
- [ ] API ingress `internal-and-cloud-load-balancing`
- [ ] EU region policy enforced for all services
- [ ] Cloud Armor policy attached (rate limit + geo/ip allow strategy where needed)
- [ ] Logging with PII minimization and documented retention
- [ ] Secret Manager for all secrets, no plaintext env files

