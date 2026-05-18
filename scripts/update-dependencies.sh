#!/bin/bash
#
# Dependency Management Script
#
# This script regenerates verification-metadata.xml for dependency verification.
# Run this script when adding new dependencies.
#
# Usage:
#   bash scripts/update-dependencies.sh
#
# Generated files:
#   - gradle/verification-metadata.xml (must be committed)
#

set -e  # Exit immediately on error

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIBRARIES_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=========================================="
echo "Dependency Management Script"
echo "=========================================="
echo ""

# Step 1: Remove existing files
echo "[Step 1/6] Removing existing files..."
cd "$LIBRARIES_DIR"
rm -f gradle/verification-metadata.xml
echo "✓ Step 1 completed"
echo ""

# Step 2: Clear dependency cache
echo "[Step 2/6] Clearing dependency cache..."
echo "Note: This may take a few minutes"
rm -rf ~/.gradle/caches/modules-2
echo "✓ Step 2 completed"
echo ""

# Step 3: Stop Gradle daemon
echo "[Step 3/6] Stopping Gradle daemon..."
./gradlew --stop
echo "✓ Step 3 completed"
echo ""

# Step 4: Generate verification-metadata.xml
echo "[Step 4/6] Generating verification-metadata.xml..."
echo "Note: This may take a few minutes"
./gradlew clean assembleDebug assembleRelease --write-verification-metadata sha256
echo "✓ Step 4 completed"
echo ""

# Step 5: Add trusted-artifacts
echo "[Step 5/6] Adding trusted-artifacts (trusting aapt2 as platform-independent)..."
sed -i '' '/<\/verify-signatures>/a\
      <trusted-artifacts>\
         <trust group="com.android.tools.build" name="aapt2"/>\
      </trusted-artifacts>
' gradle/verification-metadata.xml
echo "✓ Step 5 completed"
echo ""

# Step 6: Test verification
echo "[Step 6/6] Testing verification..."
./gradlew clean assembleDebug assembleRelease
echo "✓ Step 6 completed"
echo ""

echo "=========================================="
echo "✅ All steps completed"
echo "=========================================="
echo ""
echo "Generated files:"
echo "  - gradle/verification-metadata.xml"
echo ""
echo "Next steps:"
echo "  1. git add gradle/verification-metadata.xml"
echo "  2. git commit -m 'chore: update dependency verification metadata'"
echo ""
