#!/bin/sh
#set -x
# Copyright 2019 Orange and the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

export NOCOL='\033[0m'
export GREEN='\033[0;32m'
export RED='\033[0;31m'
export BLUE='\033[0;34m'
export YELLOW='\033[0;33m'

TARGETDIR=`dirname $0`/../../../../target/classes/static/matomo-releases
mkdir -p ${TARGETDIR}
if [ -f `dirname $0`/../../../../releases.txt ] ; then
	INREL=`dirname $0`/../../../../releases.txt
else
	INREL=`dirname $0`/releases.txt
fi
if [ -f `dirname $0`/../../../../default-release.txt ] ; then
	INDEFREL=`dirname $0`/../../../../default-release.txt
else
	INDEFREL=`dirname $0`/default-release.txt
fi
while IFS= read -r vers; do
	echo -e "${BLUE}Prepare release $vers to be used in Matomo CF service:${NOCOL}"
	if [ $vers = "latest" ] ; then
		`dirname $0`/piwik2cf.sh
		RES=$?
		LATEST=`ls -al ${TARGETDIR}/latest | awk -F '-> ' '{print $2}'`
		if [ ${RES} -eq 0 ] ; then
			if [ -z ${FIRST+x} ]; then
				FIRST=${LATEST}
			fi
		fi
	else
		`dirname $0`/piwik2cf.sh -v $vers
		if [ $? -eq 0 ] ; then
			if [ -z ${FIRST+x} ]; then
				FIRST=$vers
			fi
		fi
	fi
done < ${INREL}
if [ -z ${FIRST+x} ]; then
	echo -e "${RED}Did not succeed to prepare any release!!${NOCOL}"
	exit 1
fi
while IFS= read -r defv; do
	echo -e "${BLUE}Looking for default release${NOCOL}"
	if [ $defv = "latest" ] ; then
		if [ -z ${LATEST+x} ]; then
			echo -e "${YELLOW}Request latest release as the default one but not a target!${NOCOL}"
		else
			echo ${LATEST} > ${TARGETDIR}/DefaultVersion
			DEF=${LATEST}
			break
		fi
	fi
	(cd ${TARGETDIR}; ls -d */ | sed -e "s/\///") >${TARGETDIR}/rels
	while IFS= read -r curv; do
		if [ $curv = $defv ] ; then
			echo $defv > ${TARGETDIR}/DefaultVersion
			DEF=$defv
			break
		fi
	done < ${TARGETDIR}/rels
	rm -f ${TARGETDIR}/rels
	if [ -z ${DEF+x} ]; then
		echo -e "${YELLOW}Request ${defv} release as the default one but not a target!${NOCOL}"
	fi
done < ${INDEFREL}
if [ -z ${DEF+x} ]; then
 	echo ${FIRST} > ${TARGETDIR}/DefaultVersion
 	DEF=${FIRST}
fi
echo -e "${BLUE}Release ${DEF} is the default one.${NOCOL}"