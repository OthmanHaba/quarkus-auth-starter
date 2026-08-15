#!/usr/bin/env bash
# Scaffold a new Quarkus+Kotlin app from the auth starter.
#
#   ./create-app.sh ~/sites/shop com.acme.shop                     # from a local checkout
#   curl -fsSL https://raw.githubusercontent.com/OthmanHaba/quarkus-auth-starter/main/create-app.sh \
#     | bash -s -- ~/sites/shop com.acme.shop                      # straight from GitHub
#
# Both arguments are prompted for if omitted and a terminal is available.
set -euo pipefail

REPO_URL="https://github.com/OthmanHaba/quarkus-auth-starter.git"

# Run from a checkout when there is one, otherwise fetch the starter.
SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" 2>/dev/null && pwd || true)"
if [[ -f "${SELF_DIR:-/nonexistent}/settings.gradle.kts" ]]; then
  STARTER="$SELF_DIR"
else
  STARTER="$(mktemp -d)"
  trap 'rm -rf "$STARTER"' EXIT
  echo "Fetching starter..."
  git clone --depth 1 --quiet "$REPO_URL" "$STARTER"
fi

TARGET="${1:-}"
PKG="${2:-}"
if [[ -z "$TARGET" || -z "$PKG" ]]; then
  # stdin is the script itself under `curl | bash`, so prompt on the terminal.
  [[ -e /dev/tty ]] || { echo "Usage: create-app.sh <target-dir> <base-package>" >&2; exit 1; }
  [[ -z "$TARGET" ]] && read -rp "Target directory (e.g. ~/sites/shop): " TARGET < /dev/tty
  [[ -z "$PKG" ]] && read -rp "Base package (e.g. com.acme.shop): " PKG < /dev/tty
fi

TARGET="${TARGET/#\~/$HOME}"
[[ "$PKG" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || { echo "Bad package: $PKG" >&2; exit 1; }
[[ -e "$TARGET" ]] && { echo "Already exists: $TARGET" >&2; exit 1; }

NAME="$(basename "$TARGET")"
SLUG="${NAME//[^a-zA-Z0-9]/_}"          # postgres db/user/volume identifier
GROUP="${PKG%.*}"                        # package minus last segment
PKG_PATH="${PKG//.//}"

# ponytail: tar-pipe beats rsync/git-archive here; the exclude list is short
# enough to stay obvious. LICENSE is excluded so the new app picks its own.
mkdir -p "$TARGET"
tar -cf - -C "$STARTER" \
  --exclude=build --exclude=.gradle --exclude=.kotlin --exclude=.git \
  --exclude=.idea --exclude=.omc --exclude=create-app.sh --exclude=LICENSE . \
  | tar -xf - -C "$TARGET"

# Move the package directories in every source root.
for root in main test native-test; do
  src="$TARGET/src/$root/kotlin"
  [[ -d "$src/com/example/starter" ]] || continue
  mkdir -p "$src/$(dirname "$PKG_PATH")"
  mv "$src/com/example/starter" "$src/$PKG_PATH"
  find "$src/com" -type d -empty -delete 2>/dev/null || true
done

# Rewrite identifiers. grep picks the files, so binaries (the wrapper jar) are skipped.
sub() {
  grep -rlI --null -F "$1" "$TARGET" 2>/dev/null \
    | FROM="$1" TO="$2" xargs -0 -r perl -pi -e 's/\Q$ENV{FROM}\E/$ENV{TO}/g' || true
}
sub 'com.example.starter'   "$PKG"
sub 'group = "com.example"' "group = \"$GROUP\""
sub 'quarkus-auth-starter'  "$NAME"
sub 'auth-starter'          "$NAME"
sub 'auth_starter'          "$SLUG"

# Drop starter-only README sections from the generated app.
perl -0pi -e 's/<!-- starter-only:start -->.*?<!-- starter-only:end -->\n//gs' "$TARGET/README.md"

chmod +x "$TARGET/gradlew"
git -C "$TARGET" init -q
git -C "$TARGET" add -A
git -C "$TARGET" commit -qm "Initial commit from quarkus-auth-starter"

cat <<EOF

Created $NAME at $TARGET  (package $PKG)

  cd $TARGET
  docker compose up -d
  ./gradlew quarkusDev

Then set your real frontend origin in src/main/resources/application.properties
(app.auth.stateful-origins and quarkus.http.cors.origins still say app.example.com).
EOF
