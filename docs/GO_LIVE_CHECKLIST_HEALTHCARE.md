# Go-Live Checklist (Healthcare, DSGVO / SGB V oriented)

Use this checklist before enabling real patient data entry.

## A) Legal & Governance

- [ ] AVV/DPA signed with all processors (Google Cloud, IdP, mail provider if used).
- [ ] TOMs documented and approved.
- [ ] Data processing register updated (Art. 30 DSGVO context).
- [ ] Incident response and breach notification process defined.

## B) Region & Data Residency

- [ ] Cloud Run region is EU (recommended `europe-west3`).
- [ ] Cloud SQL region is EU.
- [ ] Backup storage remains in EU.
- [ ] Cross-region replication (if enabled) remains EU-only.

## C) Access Security

- [ ] Cloud Run has `--ingress internal-and-cloud-load-balancing`.
- [ ] Cloud Run is `--no-allow-unauthenticated`.
- [ ] External HTTPS Load Balancer in front of backend.
- [ ] IAP enabled and tested.
- [ ] Access via least-privilege groups only (no broad user assignment).
- [ ] MFA required in IdP for privileged roles.

## D) Application Security

- [ ] OIDC production client configured with correct redirect/logout URIs.
- [ ] API tenant guard active (`/api/v1/**` requires institution context).
- [ ] Single-practice mode enabled (`SINGLE_PRACTICE_ENABLED=true`).
- [ ] Security headers active on hosting layer.
- [ ] No development login paths exposed in production.

## E) Logging & Monitoring

- [ ] Log review confirms no patient PII in technical logs.
- [ ] Log retention configured according to policy.
- [ ] Alerting configured for auth failures, backend errors, and availability.
- [ ] Audit trails available and accessible to authorized admins.

## F) Backup & Restore

- [ ] Automated backups enabled.
- [ ] PITR enabled.
- [ ] Backup encryption policy verified (default encryption + CMEK if required).
- [ ] Restore runbook exists and is approved.
- [ ] Restore drill executed successfully within target RTO.
- [ ] Documented RPO/RTO targets accepted by stakeholders.

## G) Operational Readiness

- [ ] CI/CD pipeline tested (backend + frontend deploy paths).
- [ ] Rollback strategy tested.
- [ ] Secrets in Secret Manager only (no plaintext secrets in repo/workflows).
- [ ] On-call/contact ownership clarified.

## H) Final Acceptance

- [ ] Security responsible approves launch.
- [ ] Data protection responsible approves launch.
- [ ] Medical/business owner approves launch.
- [ ] Production access opened for intended users only.

