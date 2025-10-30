#!/usr/bin/env bash
set -euo pipefail

# Creates a restricted deployment user for production servers.
# - Non-root, no password, SSH-only
# - Home with ~/.ssh and correct permissions
# - Optional membership in the docker group for container-based deploys
# - Optional limited sudo for whitelisted commands only (NOPASSWD)
#
# Usage examples:
#   sudo ./create-prod-deploy-user.sh
#   sudo USERNAME=appdeploy ADD_TO_DOCKER=true ./create-prod-deploy-user.sh
#   sudo USERNAME=deployer ALLOW_SUDO=true SUDO_CMDS="/usr/bin/systemctl restart myapp.service" ./create-prod-deploy-user.sh
#
# Env vars:
#   USERNAME      - linux username to create (default: appdeploy)
#   SHELL_PATH    - login shell (default: /bin/bash)
#   ADD_TO_DOCKER - add user to docker group (true/false, default: true)
#   ALLOW_SUDO    - create sudoers entry (true/false, default: false)
#   SUDO_CMDS     - space-separated list of absolute command paths allowed via sudo NOPASSWD
#                   required if ALLOW_SUDO=true
#
# Security notes:
# - This script does NOT enable password login. Keys-only is recommended.
# - If ALLOW_SUDO=true, only whitelisted commands are allowed via NOPASSWD.
# - Review /etc/ssh/sshd_config to ensure PasswordAuthentication no (optional, global setting).

USERNAME="${USERNAME:-appdeploy}"
SHELL_PATH="${SHELL_PATH:-/bin/bash}"
ADD_TO_DOCKER="${ADD_TO_DOCKER:-true}"
ALLOW_SUDO="${ALLOW_SUDO:-false}"
SUDO_CMDS="${SUDO_CMDS:-}"

if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root (use sudo)." >&2
  exit 1
fi

if [[ "$ALLOW_SUDO" == "true" && -z "${SUDO_CMDS}" ]]; then
  echo "ALLOW_SUDO=true requires SUDO_CMDS to be set (absolute paths, space-separated)." >&2
  exit 1
fi

# Ensure required tools exist
command -v useradd >/dev/null || { echo "useradd not found" >&2; exit 1; }
command -v id >/dev/null || { echo "id not found" >&2; exit 1; }

if id -u "$USERNAME" >/dev/null 2>&1; then
  echo "User '$USERNAME' already exists. Skipping creation."
else
  echo "Creating user '$USERNAME'..."
  useradd \
    --create-home \
    --shell "$SHELL_PATH" \
    --user-group \
    --groups "" \
    "$USERNAME"
fi

HOME_DIR=$(eval echo "~$USERNAME")
SSH_DIR="$HOME_DIR/.ssh"
authorized_keys="$SSH_DIR/authorized_keys"

mkdir -p "$SSH_DIR"
chmod 700 "$SSH_DIR"
# Create empty authorized_keys if missing; key provisioning happens separately
if [[ ! -f "$authorized_keys" ]]; then
  touch "$authorized_keys"
fi
chmod 600 "$authorized_keys"
chown -R "$USERNAME":"$USERNAME" "$SSH_DIR"

echo "Ensuring no password is set for '$USERNAME' (SSH keys only)."
passwd -l "$USERNAME" >/dev/null 2>&1 || true

if [[ "$ADD_TO_DOCKER" == "true" ]]; then
  if getent group docker >/dev/null 2>&1; then
    echo "Adding '$USERNAME' to docker group..."
    usermod -aG docker "$USERNAME"
  else
    echo "Group 'docker' not found. Installing Docker is required for this option. Skipping group assignment."
  fi
fi

if [[ "$ALLOW_SUDO" == "true" ]]; then
  echo "Configuring limited sudo for '$USERNAME'..."
  # Validate command paths
  for cmd in $SUDO_CMDS; do
    if [[ ! -x "$cmd" ]]; then
      echo "Warning: '$cmd' is not executable or not found. It will still be added to sudoers." >&2
    fi
  done
  SUDOERS_FILE="/etc/sudoers.d/99-$USERNAME-deploy"
  {
    echo "# Limited sudo for deployment user $USERNAME (NOPASSWD for specific commands only)"
    echo -n "$USERNAME ALL=(root) NOPASSWD: "
    first=true
    for cmd in $SUDO_CMDS; do
      if $first; then
        echo -n "$cmd"
        first=false
      else
        echo -n ", $cmd"
      fi
    done
    echo
  } > "$SUDOERS_FILE"
  chmod 440 "$SUDOERS_FILE"
  visudo -cf "$SUDOERS_FILE" >/dev/null
fi

cat <<EOF

User '$USERNAME' is ready.
- Home: $HOME_DIR
- SSH dir: $SSH_DIR
- Add public key to: $authorized_keys
- Docker group: $ADD_TO_DOCKER
- Limited sudo: $ALLOW_SUDO

Next steps:
1) Generate SSH keypair for CI/CD and append public key to $authorized_keys
   (use scripts/security/generate-deploy-ssh-key.sh on your admin machine or on the runner)
2) Put the private key into GitHub Actions secrets (e.g. SSH_PRIVATE_KEY)
3) Configure your workflow to SSH as '$USERNAME' to the server.
EOF
