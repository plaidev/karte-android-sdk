#!/bin/bash -e

find -E core inappmessaging notifications variables visualtracking inbox -type f -iregex ".*\.(kt|java)" -exec ../karte-android-tools/scripts/rh/replace_header.sh {} \;
