#!/bin/bash

##################################################
# Constants
##################################################

MODULES=(
  "core"
  "inappmessaging"
  "notifications"
  "variables"
  "visualtracking"
  "inbox"
  "inappframe"
  "gradle-plugin"
)

##################################################
# Functions
##################################################

function usage() {
  cat << EOS
Usage:
  $ bash ./scripts/bump_version.sh

Options:
  -h   Show help
EOS
}

function check_released_version() {
  MODULE=$1
  VERSION=`curl -s https://raw.githubusercontent.com/plaidev/karte-android-sdk/master/${MODULE}/version`

  echo " "
  echo "#########################"
  echo "# RELEASED VERSION"
  echo "#########################"
  echo $VERSION
}

function bump_version() {
  MODULE=$1

  echo " "
  echo "#########################"
  echo "# LOCAL MODULE VERSION"
  echo "#########################"
  ruby scripts/bump_version.rb current-version -t $MODULE
  echo " "

  PS3="Please select a number for the update method: "
  select METHOD in major minor patch
  do
    ruby scripts/bump_version.rb $METHOD -t $MODULE
    break
  done
}

##################################################
# Command
##################################################

while getopts h OPT; do
  case "$OPT" in
    h)
      usage
      exit 0
      ;;
  esac
done

cd `dirname $0`
cd ../

PS3="Please select a number of the module you want to update: "
select MODULE in ${MODULES[@]}
do
  check_released_version $MODULE
  bump_version $MODULE
  break
done
