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

if [ -f `dirname $0`/context.sh ]; then
	. `dirname $0`/context.sh
else
	APPNAME=java-web
	MATOMOSERVINST=jw_matomo
	BUILDPACK=java_buildpack
	DOMAIN=cf.mydomain.com
fi

CLEAN=false
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -h|--help)
    echo "
Prepare the deployment of MATOMO to CloudFoundry. By default, it gets the last
release. The version to get can also be specified through the version option.

OPTIONS
    -h
    --help
            Display this help message.

    -c
    --clean
            Clean up the context (binding, service and app) at deployment start
    "
    exit 0
    ;;
    -c|--clean)
    CLEAN=true
    shift # past argument
    ;;
    *)    # unknown option
    shift # past argument
    ;;
esac
done

export NOCOL='\033[0m'
export GREEN='\033[0;32m'
export RED='\033[0;31m'
export BLUE='\033[0;34m'
export CYAN='\033[0;36m'
export YELLOW='\033[0;33m'

TARGETDIR=`dirname $0`/target
ADMINEMAIL=john.wayne@hatari.com
LOGS=`dirname $0`/deploy.log

DATE=`date`
echo -e "Deploy exec at ${DATE}" >${LOGS}
echo -e ">>>>>>>>>>>>>>>" >>${LOGS}
if [ "${CLEAN}" = "true" ]; then
	echo -n -e "${CYAN}Unbind service (if any) ${NOCOL}"
	cf us ${APPNAME} ${MATOMOSERVINST} >>${LOGS} 2>&1
	echo -e "${GREEN}DONE${NOCOL}"
	echo -n -e "${CYAN}Delete application (if any) ${NOCOL}"
	cf delete ${APPNAME} -f >>${LOGS} 2>&1
	echo -e "${GREEN}DONE${NOCOL}"
	echo -n -e "${CYAN}Delete service (if any) ${NOCOL}"
	cf ds ${MATOMOSERVINST} -f >>${LOGS} 2>&1
	SUCCESS=`cf service ${MATOMOSERVINST} 2>&1 | grep "status:" | awk -F ' ' '{print $4}'`
	SUCCESS="${SUCCESS}."
	while [ ${SUCCESS} = "progress." ]
	do
		echo -n -e "${YELLOW}.${NOCOL}"
		sleep 10
		SUCCESS=`cf service ${MATOMOSERVINST} 2>&1 | grep "status:" | awk -F ' ' '{print $4}'`
		SUCCESS="${SUCCESS}."
	done
	echo -e "${GREEN}DONE${NOCOL}"
fi
echo -n -e "${CYAN}Create service ${NOCOL}"
cf cs matomo-service global-shared-db ${MATOMOSERVINST} >>${LOGS} 2>&1
SUCCESS=`cf service ${MATOMOSERVINST} | grep "status:" | awk -F ' ' '{print $3}'`
SUCCESS="${SUCCESS}."
while [ ${SUCCESS} != "succeeded." ]
do
	echo -n -e "${YELLOW}.${NOCOL}"
	sleep 10
	SUCCESS=`cf service ${MATOMOSERVINST} | grep "status:" | awk -F ' ' '{print $3}'`
	SUCCESS="${SUCCESS}."
done
echo -e "${GREEN}DONE${NOCOL}"
PARAMS="{\"siteName\":\"${MATOMOSERVINST}\",\"trackedUrl\":\"http://${APPNAME}.${DOMAIN}\",\"adminEmail\":\"john.wayne@hatari.com\"}"
echo -n -e "${CYAN}Deploy application ${NOCOL}"
cf push ${APPNAME} -b ${BUILDPACK} -i 1 -m 740M --hostname ${APPNAME} -d ${DOMAIN} -p ${TARGETDIR}/java-web-0.0.1-SNAPSHOT.jar --no-start >>${LOGS} 2>&1
echo -e "${GREEN}DONE${NOCOL}"
INSTEXIST=`cf env ${APPNAME} | grep "\"instance_name\": \"${MATOMOSERVINST}\"" | awk -F '"' '{print $4}'`
INSTEXIST=${INSTEXIST}.
if [ ${INSTEXIST} = "." ]; then
	echo -n -e "${CYAN}Bind service ${NOCOL}"
	cf bs ${APPNAME} ${MATOMOSERVINST} -c ${PARAMS} >>${LOGS} 2>&1
	echo -e "${GREEN}DONE${NOCOL}"
fi
echo -n -e "${CYAN}Start application ${NOCOL}"
cf start ${APPNAME} >>${LOGS} 2>&1
echo -e "${GREEN}DONE${NOCOL}"
