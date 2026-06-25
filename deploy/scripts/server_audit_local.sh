#!/bin/bash
# Read-only server purity / security baseline audit
# Run directly ON the server (Baota Terminal / SSH with key):
#   bash server_audit_local.sh

set -euo pipefail

RED='\033[0;31m'; YEL='\033[1;33m'; GRN='\033[0;32m'; NC='\033[0m'
issues=0

section() { echo; echo "========== $1 =========="; }
warn() { echo -e "${YEL}[WARN]${NC} $*"; issues=$((issues+1)); }
crit() { echo -e "${RED}[CRIT]${NC} $*"; issues=$((issues+2)); }
ok()   { echo -e "${GRN}[OK]${NC} $*"; }
info() { echo "[INFO] $*"; }

section "System"
cat /etc/os-release 2>/dev/null | head -5
uname -a
uptime
who -b 2>/dev/null || true

days=$(uptime | grep -oP 'up \K[0-9]+(?= days)' || echo 0)
if [ "${days:-0}" -gt 180 ] 2>/dev/null; then
  warn "Uptime ${days} days — consider reboot after patching"
else
  ok "Uptime acceptable (${days:-?} days)"
fi

section "UID=0 accounts"
uid0=$(awk -F: '($3==0){print}' /etc/passwd)
echo "$uid0"
uid0_count=$(echo "$uid0" | grep -c . || true)
if [ "$uid0_count" -gt 1 ]; then
  crit "Multiple UID=0 accounts"
else
  ok "Single root account"
fi

section "SSH authorized_keys (root)"
if [ -f /root/.ssh/authorized_keys ]; then
  keys=$(grep -v '^#' /root/.ssh/authorized_keys | grep -v '^$' | wc -l)
  info "root authorized_keys count: $keys"
  head -5 /root/.ssh/authorized_keys
else
  info "No /root/.ssh/authorized_keys"
fi

section "Recent logins"
last -15 2>/dev/null || true
echo "--- failed ---"
lastb -10 2>/dev/null || true

section "Listening ports (public bind)"
ss -lntp 2>/dev/null || netstat -lntp 2>/dev/null || true
pub=$(ss -lntp 2>/dev/null | grep -E '0\.0\.0\.0:|:::' || true)
if echo "$pub" | grep -qE ':(3306|5432|6379|5672|9000|27017|9200)\b'; then
  crit "Database/cache ports exposed on 0.0.0.0"
  echo "$pub" | grep -E ':(3306|5432|6379|5672|9000|27017|9200)\b' || true
else
  ok "No common DB ports on 0.0.0.0 (from ss output)"
fi

section "Top CPU processes"
ps aux --sort=-%cpu | head -15

if ps aux | grep -iE 'xmrig|kdevtmpfsi|kinsing|miner' | grep -v grep >/dev/null; then
  crit "Suspicious miner-like process detected"
else
  ok "No obvious miner process names"
fi

section "Cron / persistence"
crontab -l 2>/dev/null || info "no root crontab"
ls -la /etc/cron.* 2>/dev/null || true
grep -R . /var/spool/cron/ 2>/dev/null | head -30 || true

if crontab -l 2>/dev/null | grep -iE 'curl|wget|/tmp/|base64|bash -i' >/dev/null; then
  warn "Suspicious patterns in root crontab"
fi

section "Web root (/www/wwwroot)"
if [ -d /www/wwwroot ]; then
  ls -la /www/wwwroot/
  info "Recently modified files (14d):"
  find /www/wwwroot -type f -mtime -14 2>/dev/null | head -40
  if find /www/wwwroot -type f -mtime -14 \( -name '*.php' -o -name '*.jsp' \) 2>/dev/null | head -1 | grep -q .; then
    warn "Recent PHP/JSP changes under webroot — review manually"
  fi
else
  info "/www/wwwroot not found"
fi

section "Docker"
if command -v docker >/dev/null; then
  docker ps -a 2>/dev/null || true
else
  info "Docker not installed"
fi

section "Pending updates (yum)"
yum check-update 2>/dev/null | head -20 || true

section "Disk / Memory"
df -hT / /www 2>/dev/null || df -h
free -h 2>/dev/null || true

section "Baota panel"
if [ -d /www/server/panel ]; then
  info "Baota panel directory exists"
  bt default 2>/dev/null | head -5 || true
else
  info "Baota panel path not found"
fi

echo
echo "=========================================="
if [ "$issues" -ge 4 ]; then
  echo -e "${RED}RESULT: NOT CLEAN — fix CRIT/WARN items before production${NC}"
elif [ "$issues" -ge 1 ]; then
  echo -e "${YEL}RESULT: USABLE WITH CAUTION — address warnings first${NC}"
else
  echo -e "${GRN}RESULT: BASELINE OK — still review Baota 16 security items manually${NC}"
fi
echo "Issue score: $issues (higher = more concerns)"
echo "=========================================="
