#!/bin/sh
# turn on debugging as needed
# display executed commands
#set -x

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
    		If Matomo release has already been prepared, force overiding previous preparation.
    "
    exit 0
    ;;
    -v|--version)
    PIWIKVERSION="$2"
    shift # past argument
    shift # past value
    ;;
    -f|--force)
    FORCE="1"
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
export YELLOW='\033[0;33m'

GENERATE_DIR=`dirname $0`
LPWD=`pwd`
cd ${GENERATE_DIR}/../../../target/classes/static/matomo-releases
export GENERATE_DIR=`pwd`
cd ${LPWD}
TMPDIR=${GENERATE_DIR}/tmp
rm -rf ${TMPDIR}
mkdir -p ${TMPDIR}
SOURCEDIR=${TMPDIR}/matomo
PIWIKVERSION=latest

if [ -e ${GENERATE_DIR}/${PIWIKVERSION} ]; then
	if [ -z ${FORCE+x} ]; then
		echo "Matomo version ${PIWIKVERSION} has already been prepared: skip"
		rm -rf ${TMPDIR}
		exit 0
	fi
fi

############################################################################################################################
# Fetch PIWIK from Internet and adapt it to CF
if [ -z ${PIWIKVERSION+x} ]; then
	SOURCEURL="https://builds.matomo.org/matomo.zip"
	echo -e "${YELLOW}Fetch latest Matomo release from Internet and adapt it to CF${NOCOL}"
else
	SOURCEURL="https://builds.matomo.org/matomo-${PIWIKVERSION}.zip"
	echo -e "${YELLOW}Fetch Matomo release ${PIWIKVERSION} from Internet and adapt it to CF${NOCOL}"
fi

curl ${SOURCEURL} >${TMPDIR}/piwik.zip
if [ "`grep "404 Not Found" ${TMPDIR}/piwik.zip`" == "<title>404 Not Found</title>" ]; then
	echo "Matomo version ${PIWIKVERSION} does not exist"
	rm ${TMPDIR}/piwik.zip
	rm -rf ${TMPDIR}
	exit 0
fi
unzip -d ${TMPDIR} ${TMPDIR}/piwik.zip >/dev/null
rm ${TMPDIR}/piwik.zip
if [ ${PIWIKVERSION} = "latest" ]; then
	PIWIKVERSION=`awk '/## Matomo/{print}' ${TMPDIR}/matomo/CHANGELOG.md | sed -n 1,1p | awk -F ' ' '{print $3}'`
	rm -f ${GENERATE_DIR}/DefaultVersion
	(cd ${GENERATE_DIR}; echo -n ${PIWIKVERSION} >${GENERATE_DIR}/DefaultVersion)
	LATEST=1
else
	if [ ! -f ${GENERATE_DIR}/DefaultVersion ]; then
		(cd ${GENERATE_DIR}; echo -n ${PIWIKVERSION} >${GENERATE_DIR}/DefaultVersion)
	fi
	LATEST=0
fi
RELEASEDIR=${GENERATE_DIR}/${PIWIKVERSION}
mkdir ${RELEASEDIR}
VMIN=`echo ${PIWIKVERSION} | awk -F '.' '{print $2}'`

# Remove Composer files
rm ${SOURCEDIR}/composer.json
rm ${SOURCEDIR}/composer.lock

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

  \$services = getenv(\"VCAP_SERVICES\");
  \$services_json = json_decode(\$services,true);

  if (isset(\$services_json)) {
    if (isset(\$services_json[\"p-mysql\"][0][\"credentials\"])) {
      \$mysql_config = \$services_json[\"p-mysql\"][0][\"credentials\"];
      \$_ENV[\"SQLDB\"] = \$mysql_config[\"name\"];
      \$_ENV[\"SQLHOST\"] = \$mysql_config[\"hostname\"];
      \$_ENV[\"SQLPORT\"] = \$mysql_config[\"port\"];
      \$_ENV[\"SQLUSER\"] = \$mysql_config[\"username\"];
      \$_ENV[\"SQLPASSWORD\"] = \$mysql_config[\"password\"];
    }
    if (isset(\$services_json[\"o-smtp\"][0][\"credentials\"])) {
        \$smtp_config = \$services_json[\"o-smtp\"][0][\"credentials\"];
        \$_ENV[\"MAILUSER\"] = \$smtp_config[\"username\"];
        \$_ENV[\"MAILPASSWORD\"] = \$smtp_config[\"password\"];
        \$_ENV[\"MAILHOST\"] = \$smtp_config[\"host\"];
        \$_ENV[\"MAILPORT\"] = \$smtp_config[\"port\"];
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
sed -e "s/force_ssl = 0/force_ssl = 1/" ${TMPDIR}/workingfile >${SOURCEDIR}/config/global.ini.php

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
if [ $LATEST -eq 1 ] ; then
	(cd ${GENERATE_DIR}; ln -s ${PIWIKVERSION} latest)
fi
rm -rf ${TMPDIR}
echo -n >${GENERATE_DIR}/Versions
echo -n >${GENERATE_DIR}/VersionsCR
SEP=""
addVersion()
{
	if [ $1 = "." ] ; then
		return
	fi
	echo -n $SEP >>${GENERATE_DIR}/Versions
	echo -n $1 | sed -e "s/^\.\///" >>${GENERATE_DIR}/Versions
	echo $1 | sed -e "s/^\.\///" >>${GENERATE_DIR}/VersionsCR
	SEP=";"
}
(cd ${GENERATE_DIR}; find . -maxdepth 1 -type d | while read dir; do addVersion "$dir"; done)
