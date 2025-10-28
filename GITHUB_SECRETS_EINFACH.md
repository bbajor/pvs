# GitHub Secrets - Schritt für Schritt

## So trägst du die Secrets ein:

1. **Gehe zu deinem GitHub Repository**
   - https://github.com/bbajor/pvs

2. **Klicke auf "Settings"** (oben rechts im Menü)

3. **Links in der Sidebar:**
   - "Secrets and variables" 
   - → "Actions"

4. **Klicke auf "New repository secret"** (rechts oben)

5. **Für jedes Secret einzeln:**

### Secret 1: HETZNER_HOST
- **Name**: `HETZNER_HOST`
- **Secret**: `188.245.253.179`
- Klicke "Add secret"

### Secret 2: HETZNER_USER
- **Name**: `HETZNER_USER`
- **Secret**: `root`
- Klicke "Add secret"

### Secret 3: HETZNER_SSH_KEY
- **Name**: `HETZNER_SSH_KEY`
- **Secret**: (Füge den kompletten Private Key ein - siehe unten)
- Klicke "Add secret"

```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
QyNTUxOQAAACCFH5rQb56Zcrc3rF3Ip5fV0nJ9lyiX6RVg18yD6Q+v9QAAAKBELcUvRC3F
LwAAAAtzc2gtZWQyNTUxOQAAACCFH5rQb56Zcrc3rF3Ip5fV0nJ9lyiX6RVg18yD6Q+v9Q
AAAEDvUABOhWu8o3UTxluFJViuC/UMjJATT2hPqSvDSE9LZoUfmtBvnplytzesXcinl9XS
cn2XKJfpFWDXzIPpD6/1AAAAFmdpdGh1Yi1hY3Rpb25zLWhldHpuZXIBAgMEBQYH
-----END OPENSSH PRIVATE KEY-----
```

### Secret 4: PROD_DB_HOST
- **Name**: `PROD_DB_HOST`
- **Secret**: `localhost`
- Klicke "Add secret"

### Secret 5: PROD_DB_NAME
- **Name**: `PROD_DB_NAME`
- **Secret**: `pvs_prod`
- Klicke "Add secret"

### Secret 6: PROD_DB_USER
- **Name**: `PROD_DB_USER`
- **Secret**: `pvs_user`
- Klicke "Add secret"

### Secret 7: PROD_DB_PASSWORD
- **Name**: `PROD_DB_PASSWORD`
- **Secret**: `wLje1DKyHUUEMffCW09jCzS7neZlF2OV+MTO1TJGd+k=`
- Klicke "Add secret"

## ✅ Am Ende solltest du 7 Secrets sehen:

1. HETZNER_HOST
2. HETZNER_USER
3. HETZNER_SSH_KEY
4. PROD_DB_HOST
5. PROD_DB_NAME
6. PROD_DB_USER
7. PROD_DB_PASSWORD

## 🚀 Dann testen:

GitHub → **Actions** Tab → "Build and Push Docker Images (Hetzner)" → "Run workflow"

