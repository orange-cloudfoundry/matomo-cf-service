#!/bin/sh

export NOCOL='\033[0m'
export GREEN='\033[0;32m'
export RED='\033[0;31m'
export BLUE='\033[0;34m'
export YELLOW='\033[0;33m'

while IFS= read -r vers; do
	echo -e "${BLUE}Prepare version $vers to be used in Matomo CF service${NOCOL}"
	if [ $vers = "latest" ] ; then
		`dirname $0`/piwik2cf.sh
	else
		`dirname $0`/piwik2cf.sh -v $vers
	fi
done < "`dirname $0`/../../../releases.txt"
