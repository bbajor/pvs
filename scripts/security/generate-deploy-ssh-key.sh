#!/usr/bin/env bash
set -euo pipefail

# Generates an SSH keypair for the deployment user.
# Default: ed25519 key, no passphrase (CI-friendly). You can set a passphrase via KEY_PASSPHRASE.
# Prints public key for server authorized_keys and stores private key for GitHub Actions secret.
#
# Usage examples:
#   ./generate-deploy-ssh-key.sh
#   KEY_NAME=prod-deploy ./generate-deploy-ssh-key.sh
#   KEY_DIR=~/.ssh KEY_NAME=hetzner-prod KEY_PASSPHRASE="my pass" ./generate-deploy-ssh-key.sh
#
# Env vars:
#   KEY_DIR        - destination directory (default: ./secrets)
#   KEY_NAME       - base name of key files (default: prod-deploy)
#   KEY_COMMENT    - comment embedded in public key (default: "prod deploy key")
#   KEY_PASSPHRASE - optional passphrase for the key (default: empty)

KEY_DIR=${KEY_DIR:-"./secrets"}
KEY_NAME=${KEY_NAME:-"prod-deploy"}
KEY_COMMENT=${KEY_COMMENT:-"prod deploy key"}
KEY_PASSPHRASE=${KEY_PASSPHRASE:-""}

mkdir -p "$KEY_DIR"
PRIV_KEY_PATH="$KEY_DIR/$KEY_NAME"
PUB_KEY_PATH="$KEY_DIR/$KEY_NAME.pub"

if [[ -f "$PRIV_KEY_PATH" || -f "$PUB_KEY_PATH" ]]; then
  echo "Key files already exist at $PRIV_KEY_PATH(.pub). Refusing to overwrite." >&2
  exit 1
fi

# Create the key (ed25519)
if [[ -n "$KEY_PASSPHRASE" ]]; then
  ssh-keygen -t ed25519 -C "$KEY_COMMENT" -N "$KEY_PASSPHRASE" -f "$PRIV_KEY_PATH"
else
  ssh-keygen -t ed25519 -C "$KEY_COMMENT" -N '' -f "$PRIV_KEY_PATH"
fi

chmod 600 "$PRIV_KEY_PATH"
chmod 644 "$PUB_KEY_PATH"

cat <<EOF

Keypair generated:
- Private key: $PRIV_KEY_PATH
- Public key:  $PUB_KEY_PATH

Next steps:
1) Copy the public key to the server's deploy user authorized_keys:
   ssh-copy-id -i "$PUB_KEY_PATH" USER@SERVER
   # or append manually to /home/USER/.ssh/authorized_keys on the server

2) Store the private key in GitHub Actions secrets (e.g. SSH_PRIVATE_KEY).
   On the workflow runner, use it like:

   - name: Setup SSH key
     run: |
       mkdir -p ~/.ssh
       echo "$SSH_PRIVATE_KEY" > ~/.ssh/id_ed25519
       chmod 600 ~/.ssh/id_ed25519
       ssh -o StrictHostKeyChecking=no USER@SERVER "echo ok"

3) Configure your deployment job to use USER@SERVER and run your deploy steps.

Security tips:
- Restrict key usage with authorized_keys options if needed (no-port-forwarding,no-agent-forwarding,...)
- Rotate keys periodically; keep private keys out of repos.
EOF
