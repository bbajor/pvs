# GitHub Secrets Konfiguration

## 🔐 Erforderliche Secrets

Gehe zu: GitHub Repository → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

### 1. HETZNER_HOST
```
188.245.253.179
```

### 2. HETZNER_USER
```
root
```

### 3. HETZNER_SSH_KEY
Private Key Inhalt (ganzen Inhalt kopieren):

```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
QyNTUxOQAAACCFH5rQb56Zcrc3rF3Ip5fV0nJ9lyiX6RVg18yD6Q+v9QAAAKBELcUvRC3F
LwAAAAtzc2gtZWQyNTUxOQAAACCFH5rQb56Zcrc3rF3Ip5fV0nJ9lyiX6RVg18yD6Q+v9Q
AAAEDvUABOhWu8o3UTxluFJViuC/UMjJATT2hPqSvDSE9LZoUfmtBvnplytzesXcinl9XS
cn2XKJfpFWDXzIPpD6/1AAAAFmdpdGh1Yi1hY3Rpb25zLWhldHpuZXIBAgMEBQYH
-----END OPENSSH PRIVATE KEY-----
```

### 4. PROD_DB_HOST
```
localhost
```

### 5. PROD_DB_NAME
```
pvs_prod
```

### 6. PROD_DB_USER
```
pvs_user
```

### 7. PROD_DB_PASSWORD
```
wLje1DKyHUUEMffCW09jCzS7neZlF2OV+MTO1TJGd+k=
```

## ✅ Checklist

- [ ] `HETZNER_HOST` gesetzt
- [ ] `HETZNER_USER` gesetzt
- [ ] `HETZNER_SSH_KEY` gesetzt (Private Key komplett)
- [ ] `PROD_DB_HOST` gesetzt
- [ ] `PROD_DB_NAME` gesetzt
- [ ] `PROD_DB_USER` gesetzt
- [ ] `PROD_DB_PASSWORD` gesetzt

## 🚀 Nach dem Setup

Teste das Deployment:
- GitHub → Actions → "Build and Push Docker Images (Hetzner)"
- "Run workflow" → Stage: `dev`

