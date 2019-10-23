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

FORCE=0
DEBUG=0
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

    -v
    --version
            The version number (e.g., 3.9.1) of the Matomo release to prepare.

    -f
    --force
    		If Matomo releases have already been prepared, force overiding previous preparation.

    -d
    --debug
    		Prepare Matomo releases to be run in debug mode.
    "
    exit 0
    ;;
    -v|--version)
    PIWIKVERSION="$2"
    shift # past argument
    shift # past value
    ;;
    -f|--force)
    FORCE=1
    shift # past argument
    ;;
    -d|--debug)
    DEBUG=1
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

GENERATE_DIR=`dirname $0`
LPWD=`pwd`
mkdir -p ${GENERATE_DIR}/../../../target/classes/static/matomo-releases
cd ${GENERATE_DIR}/../../../target/classes/static/matomo-releases
export GENERATE_DIR=`pwd`
cd ${LPWD}
TMPDIR=${GENERATE_DIR}/tmp
rm -rf ${TMPDIR}
if [ -z ${PIWIKVERSION+x} ]; then
	PIWIKVERSION=LatestVersion
fi

if [ -e ${GENERATE_DIR}/${PIWIKVERSION} ]; then
	if [ ${FORCE} -eq 0 ]; then
		echo -e "	${RED}Matomo version ${PIWIKVERSION} has already been prepared: skip${NOCOL}"
		exit 0
	fi
fi

mkdir -p ${TMPDIR}
SOURCEDIR=${TMPDIR}/matomo

############################################################################################################################
# Fetch PIWIK from Internet and adapt it to CF
if [ ${PIWIKVERSION} = "LatestVersion" ]; then
	SOURCEURL="https://builds.matomo.org/matomo.zip"
	echo -e "	- ${YELLOW}Fetch latest Matomo release from Internet${NOCOL}"
else
	SOURCEURL="https://builds.matomo.org/matomo-${PIWIKVERSION}.zip"
	echo -e "	- ${YELLOW}Fetch Matomo release ${PIWIKVERSION} from Internet${NOCOL}"
fi

curl ${SOURCEURL} >${TMPDIR}/piwik.zip 2>/dev/null
if [ "`grep "404 Not Found" ${TMPDIR}/piwik.zip`" == "<title>404 Not Found</title>" ]; then
	echo "Matomo version ${PIWIKVERSION} does not exist"
	rm ${TMPDIR}/piwik.zip
	rm -rf ${TMPDIR}
	exit 1
fi
echo -e "	- ${YELLOW}Unzip Matomo package${NOCOL}"
unzip -d ${TMPDIR} ${TMPDIR}/piwik.zip >/dev/null
rm ${TMPDIR}/piwik.zip
CODEPIWIKVERSION=`grep VERSION ${TMPDIR}/matomo/core/Version.php | awk -F "'" '{print $2}'`
if [ ${PIWIKVERSION} = "LatestVersion" ]; then
#	PIWIKVERSION=`awk '/## Matomo/{print}' ${TMPDIR}/matomo/CHANGELOG.md | sed -n 1,1p | awk -F ' ' '{print $3}'`
	PIWIKVERSION=${CODEPIWIKVERSION}
	rm -f ${GENERATE_DIR}/DefaultVersion
	(cd ${GENERATE_DIR}; echo -n ${PIWIKVERSION} >${GENERATE_DIR}/DefaultVersion)
	LATEST=1
else
	PIWIKVERSION=${CODEPIWIKVERSION}
	if [ ! -f ${GENERATE_DIR}/DefaultVersion ]; then
		(cd ${GENERATE_DIR}; echo -n ${PIWIKVERSION} >${GENERATE_DIR}/DefaultVersion)
	fi
	LATEST=0
fi
RELEASEDIR=${GENERATE_DIR}/${PIWIKVERSION}
mkdir -p ${RELEASEDIR}
VMIN=`echo ${PIWIKVERSION} | awk -F '.' '{print $2}'`

# Remove Composer files
rm ${SOURCEDIR}/composer.json
rm ${SOURCEDIR}/composer.lock

echo -e "	- ${YELLOW}Adapt it to CloudFoundry${NOCOL}"
# Create bootstrap.php file
echo "<?php
  \$_ENV[\"SQLDB\"] = NULL;
  \$_ENV[\"SQLHOST\"] = NULL;
  \$_ENV[\"SQLPORT\"] = NULL;
  \$_ENV[\"SQLUSER\"] = NULL;
  \$_ENV[\"SQLPASSWORD\"] = NULL;
  \$_ENV[\"MAILUSER\"] = NULL;
  \$_ENV[\"MAILPASSWORD\"] = NULL;
  \$_ENV[\"MAILHOST\"] = NULL;
  \$_ENV[\"MAILPORT\"] = NULL;

  \$application = getenv(\"VCAP_APPLICATION\");
  \$application_json = json_decode(\$application,true);

  if (isset(\$application_json[\"application_uris\"])) {
    \$_ENV[\"APPURIS\"] = \$application_json[\"application_uris\"];
  }

  \$cfbindenv[\"SQLSRV\"] = getenv(\"MCFS_DBSRV\");
  \$cfbindenv[\"SQLDB\"] = getenv(\"MCFS_DBNAME\");
  \$cfbindenv[\"SQLHOST\"] = getenv(\"MCFS_DBHOST\");
  \$cfbindenv[\"SQLPORT\"] = getenv(\"MCFS_DBPORT\");
  \$cfbindenv[\"SQLUSER\"] = getenv(\"MCFS_DBUSER\");
  \$cfbindenv[\"SQLPASSWORD\"] = getenv(\"MCFS_DBPASSWD\");
  \$cfbindenv[\"MAILSRV\"] = getenv(\"MCFS_MAILSRV\");
  \$cfbindenv[\"MAILUSER\"] = getenv(\"MCFS_MAILUSER\");
  \$cfbindenv[\"MAILPASSWORD\"] = getenv(\"MCFS_MAILPASSWD\");
  \$cfbindenv[\"MAILHOST\"] = getenv(\"MCFS_MAILHOST\");
  \$cfbindenv[\"MAILPORT\"] = getenv(\"MCFS_MAILPORT\");

  \$services = getenv(\"VCAP_SERVICES\");
  \$services_json = json_decode(\$services,true);

  if (isset(\$services_json)) {
    if (isset(\$services_json[\$cfbindenv[\"SQLSRV\"]][0][\"credentials\"])) {
      \$mysql_config = \$services_json[\$cfbindenv[\"SQLSRV\"]][0][\"credentials\"];
      \$_ENV[\"SQLDB\"] = \$mysql_config[\$cfbindenv[\"SQLDB\"]];
      \$_ENV[\"SQLHOST\"] = \$mysql_config[\$cfbindenv[\"SQLHOST\"]];
      \$_ENV[\"SQLPORT\"] = \$mysql_config[\$cfbindenv[\"SQLPORT\"]];
      \$_ENV[\"SQLUSER\"] = \$mysql_config[\$cfbindenv[\"SQLUSER\"]];
      \$_ENV[\"SQLPASSWORD\"] = \$mysql_config[\$cfbindenv[\"SQLPASSWORD\"]];
    }
    if (isset(\$services_json[\$cfbindenv[\"MAILSRV\"]][0][\"credentials\"])) {
        \$smtp_config = \$services_json[\$cfbindenv[\"MAILSRV\"]][0][\"credentials\"];
        \$_ENV[\"MAILUSER\"] = \$smtp_config[\$cfbindenv[\"MAILUSER\"]];
        \$_ENV[\"MAILPASSWORD\"] = \$smtp_config[\$cfbindenv[\"MAILPASSWORD\"]];
        \$_ENV[\"MAILHOST\"] = \$smtp_config[\$cfbindenv[\"MAILHOST\"]];
        \$_ENV[\"MAILPORT\"] = \$smtp_config[\$cfbindenv[\"MAILPORT\"]];
    }
  }
?>" > ${SOURCEDIR}/bootstrap.php

# Change setup of datasource to be retrieved from CF environment - File: FormDatabaseSetup.php
cp ${SOURCEDIR}/plugins/Installation/FormDatabaseSetup.php ${TMPDIR}/workingfile
if [[ ${VMIN} -gt 7 ]]; then
  LNSRCH=`grep -n '\$defaults = array' ${TMPDIR}/workingfile | awk -F ':' '{print $1}'`
else
  LNSRCH=`grep -n '\$this->addDataSource' ${TMPDIR}/workingfile | awk -F ':' '{print $1}'`
fi
sed -n 1,${LNSRCH}p ${TMPDIR}/workingfile >${SOURCEDIR}/plugins/Installation/FormDatabaseSetup.php
echo "                                                                    'host'          => \$_ENV[\"SQLHOST\"].':'.\$_ENV[\"SQLPORT\"],
                                                                    'username'      => \$_ENV[\"SQLUSER\"],
                                                                    'password'      => \$_ENV[\"SQLPASSWORD\"],
                                                                    'dbname'        => \$_ENV[\"SQLDB\"]," >>${SOURCEDIR}/plugins/Installation/FormDatabaseSetup.php
LNSRCH=$((LNSRCH + 2))
sed -n ${LNSRCH},\$p ${TMPDIR}/workingfile >>${SOURCEDIR}/plugins/Installation/FormDatabaseSetup.php

# Change setup of trustedHosts to be retrieved from CF environment - File: Controller.php
cp ${SOURCEDIR}/plugins/Installation/Controller.php ${TMPDIR}/workingfile
LNSRCH=`grep -n 'trustedHosts = array()' ${TMPDIR}/workingfile | awk -F ':' '{print $1}'`
LNSRCH=$((LNSRCH - 1))
LNSRCH2=`grep -n '!DbHelper' ${TMPDIR}/workingfile | awk -F ':' '{print $1}'`
LNSRCH2=$((LNSRCH2 + 3))
sed -n 1,${LNSRCH2}p ${TMPDIR}/workingfile >${SOURCEDIR}/plugins/Installation/Controller.php
echo "        # Improved Security
        # With SSL ALWAYS available for all Bluemix apps, let's require all requests
        # to be made over SSL (https) so that data is NOT sent in the clear.
        # Non-ssl requests will trigger a 
        #    Error: Form security failed. 
        #    Please reload the form and check that your cookies are enabled
        # Reference: http://piwik.org/faq/how-to/faq_91/
        \$config->General['assume_secure_protocol'] = 1;
        \$config->General['force_ssl'] = 1;
         
        # Setup proxy_client_headers to accurately detect GeoIPs of visiting clients
        \$config->General['proxy_client_headers'] = array(\"HTTP_X_CLIENT_IP\",\"HTTP_X_FORWARDED_FOR\",\"HTTP_X_CLUSTER_CLIENT_IP\",\"HTTP_CLIENT_IP\");
 
        \$config->General['proxy_host_headers'] = \"HTTP_X_FORWARDED_HOST\";
  
        # Let us have this Piwik deploy track itself to get some early data and success 🙂
        \$config->Debug['enable_measure_piwik_usage_in_idsite'] = 1;
  
        # Let's setup the config files trusted hosts entries to handle
        # 1...N amount of user-defined IBM Bluemix app routes
        if (isset(\$_ENV[\"APPURIS\"])) {
              foreach (\$_ENV[\"APPURIS\"] as \$application_uri) {
                \$this->addTrustedHosts(\"https://\" . \$application_uri);
              }
        }
 
        # Emailing the easy way with IBM Bluemix + the SendGrid Service
        if (isset(\$_ENV[\"MAILHOST\"])) {
            \$config->mail['transport']=\"smtp\";
            \$config->mail['port']=\$_ENV[\"MAILPORT\"];
            \$config->mail['type']=\"Plain\";
            \$config->mail['host']=\$_ENV[\"MAILHOST\"];
        }
" >>${SOURCEDIR}/plugins/Installation/Controller.php
LNSRCH2=$((LNSRCH2 + 1))
sed -n ${LNSRCH2},${LNSRCH}p ${TMPDIR}/workingfile >>${SOURCEDIR}/plugins/Installation/Controller.php
echo "        \$trustedHosts = Config::getInstance()->General['trusted_hosts'];

        if (!is_array(\$trustedHosts)) {
            \$trustedHosts = array();
        }" >>${SOURCEDIR}/plugins/Installation/Controller.php
LNSRCH=$((LNSRCH + 2))
sed -n ${LNSRCH},\$p ${TMPDIR}/workingfile >>${SOURCEDIR}/plugins/Installation/Controller.php

# Force SSL mode
cp ${SOURCEDIR}/config/global.ini.php ${TMPDIR}/workingfile
if [ ${DEBUG} -eq 1 ]; then
  sed -e "s/debug = 0/debug = 1/" -e "s/log_level = WARN/log_level = DEBUG/" -e "s/force_ssl = 0/force_ssl = 1/" ${TMPDIR}/workingfile >${SOURCEDIR}/config/global.ini.php
else
  sed -e "s/force_ssl = 0/force_ssl = 1/" ${TMPDIR}/workingfile >${SOURCEDIR}/config/global.ini.php
fi

# Disable Composer
echo "/composer.*" >${SOURCEDIR}/.cfignore

# Add PHP config for CF
mkdir -p ${SOURCEDIR}/.bp-config/php/php.ini.d
echo "{
  \"PHP_VERSION\": \"{PHP_72_LATEST}\"
}" >${SOURCEDIR}/.bp-config/options.json
echo "
extension=mysqli.so
extension=pdo.so
extension=pdo_mysql.so
extension=mbstring.so
" >${SOURCEDIR}/.bp-config/php/php.ini.d/matomo.ini
(cd ${SOURCEDIR}; cp -r . ${RELEASEDIR} >/dev/null)
echo -e "	- ${YELLOW}Update supported versions${NOCOL}"
if [ $LATEST -eq 1 ] ; then
	(cd ${GENERATE_DIR}; rm -f LatestVersion; echo -n -e "${PIWIKVERSION}" >LatestVersion)
fi
rm -rf ${TMPDIR}
echo -n >${GENERATE_DIR}/Versions
SEP=""
addVersion()
{
	if [ $1 = "." ] ; then
		return
	fi
	VERSFROMDIR=`echo $1 | sed -e "s/^\.\///"`
	echo -n $SEP >>${GENERATE_DIR}/Versions
	echo -n $VERSFROMDIR >>${GENERATE_DIR}/Versions
	SEP=";"
}
(cd ${GENERATE_DIR}; find . -maxdepth 1 -type d | while read dir; do addVersion "$dir"; done)
echo -e "	${GREEN}DONE${NOCOL}"
exit 0
