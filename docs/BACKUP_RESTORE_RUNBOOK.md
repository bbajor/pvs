# Backup & Restore Runbook (Encrypted, EU)

Scope: Cloud SQL PostgreSQL for PVS production data.

## Objectives

- Data protection: encrypted backups in EU
- Operational target:
  - RPO (data loss window): <= 24h baseline (adjust as needed)
  - RTO (restore target): <= 60 minutes baseline (adjust per SLA)

## Backup strategy

## 1) Automated backups (Cloud SQL)

- Enable automated daily backups.
- Enable point-in-time recovery (PITR).
- Keep retention according to policy (e.g., 30 days).
- Region must stay in EU.

## 2) Encryption

- At-rest encryption is active by default in Google Cloud.
- Recommended for healthcare workloads:
  - Use CMEK (Cloud KMS) for Cloud SQL if compliance policy requires customer-managed keys.
  - Limit KMS key access to least privilege IAM roles.

## 3) Export backups (optional secondary control)

- Scheduled logical exports to EU-located Cloud Storage bucket.
- Bucket controls:
  - versioning enabled
  - retention lock (as required)
  - CMEK (optional, policy-driven)

## Restore procedures

## A) PITR restore to timestamp

1. Identify incident window and target timestamp (UTC).
2. Restore to a new instance (never directly overwrite first).
3. Validate schema/version and critical data.
4. Switch application DB endpoint after validation.

Example:

```bash
gcloud sql instances restore-pitr <NEW_INSTANCE_NAME> \
  --restore-instance=<SOURCE_INSTANCE_NAME> \
  --restore-time="2026-04-28T08:00:00Z"
```

## B) Backup restore from automated backup

1. List backups.
2. Restore selected backup to new instance.
3. Run smoke tests and migration validation.
4. Cut over traffic.

Example:

```bash
gcloud sql backups list --instance=<SOURCE_INSTANCE_NAME>
gcloud sql backups restore <BACKUP_ID> \
  --restore-instance=<NEW_INSTANCE_NAME> \
  --backup-instance=<SOURCE_INSTANCE_NAME>
```

## Validation checklist after restore

- [ ] Application starts and health endpoint is green.
- [ ] Flyway baseline/version is correct.
- [ ] Authentication and authorization work.
- [ ] Patient search and IVOM list work.
- [ ] Recent critical records present.
- [ ] Audit log continuity verified.

## Regular restore drills

- Execute quarterly restore drills.
- Document actual RTO/RPO achieved.
- Update runbook based on drill results.

## Security notes

- Restore operations require privileged IAM; use just-in-time access where possible.
- All backup/restore activities must be audit logged.
- Never move backups outside approved EU locations.

