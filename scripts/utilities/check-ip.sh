#!/bin/bash
# Script zum Prüfen der Server-IPs

echo "🔍 Prüfe Server-IPs..."
echo ""
echo "IPv6: 2a01:4f8:1c1a:6d24::/64"
echo ""
echo "Bitte prüfe im Hetzner Dashboard:"
echo "1. Gibt es eine IPv4-Adresse? (meist zusätzlich zur IPv6)"
echo "2. SSH-Zugriff funktioniert?"
echo ""
echo "Falls nur IPv6 vorhanden:"
echo "- SSH: ssh -6 root@2a01:4f8:1c1a:6d24::/64"
echo "- Oder IPv4 aktivieren im Hetzner Dashboard"

