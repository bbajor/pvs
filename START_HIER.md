# 🚀 Start hier - Server Setup

## Deine Server-Info:

**IPv6:** `2a01:4f8:1c1a:6d24::/64`

**⚠️ WICHTIG:** Prüfe im Hetzner Dashboard, ob es eine **IPv4-Adresse** gibt (meist zusätzlich vorhanden).

## Option 1: IPv4 vorhanden (Empfohlen)

1. **Im Hetzner Dashboard prüfen:**
   - Server → Network → **Primary IPv4**
   - Adresse notieren (z.B. `123.45.67.89`)

2. **SSH-Zugriff testen:**
   ```bash
   ssh root@<IPv4-ADRESSE>
   ```

3. **Setup ausführen:**
   ```bash
   scp setup-server.sh root@<IPv4-ADRESSE>:/root/
   ssh root@<IPv4-ADRESSE> "chmod +x /root/setup-server.sh && /root/setup-server.sh"
   ```

## Option 2: Nur IPv6

SSH mit IPv6:
```bash
ssh -6 root@2a01:4f8:1c1a:6d24::1
```

**Oder IPv4 aktivieren:**
- Hetzner Dashboard → Server → Network → "Add IPv4 Address"
- Dann Option 1 nutzen

## Schnellcheck: Welche IPs hat der Server?

Im Hetzner Dashboard unter "Server Details" → "Network" solltest du sehen:
- ✅ Primary IPv4: `xxx.xxx.xxx.xxx`
- ✅ Primary IPv6: `2a01:4f8:1c1a:6d24::/64`

**Gib mir die IPv4-Adresse, dann starten wir mit dem Setup!** 😊

