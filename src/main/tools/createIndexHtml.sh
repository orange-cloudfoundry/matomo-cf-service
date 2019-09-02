#!/bin/sh
# turn on debugging as needed
# display executed commands
#set -x

export NOCOL='\033[0m'
export GREEN='\033[0;32m'
export RED='\033[0;31m'
export BLUE='\033[0;34m'
export YELLOW='\033[0;33m'

GENERATE_DIR=`dirname $0`
LPWD=`pwd`
cd ${GENERATE_DIR}/../../../target/classes/static
export GENERATE_DIR=`pwd`
cd ${LPWD}

# Create bootstrap.php file
echo -n "
<!DOCTYPE html>
<html>
<head>
<meta charset=\"ISO-8859-1\">
<title>Matomo CF Releases</title>
</head>

<body>
<table style=\"width:100%\">
  <tr>
    <th align=\"left\" width=\"33%\">
		<a href=\"https://matomo.org/\">
		  <img src=\"img/Matomo_Logo.png\" alt=\"Matomo Logo\" style=\"width:256px;height:128px;border:0\">
		</a>
    </th>
    <th width=\"34%\">Matomo CF Releases</th>
    <th align=\"right\" width=\"33%\">
		<a href=\"https://www.cloudfoundry.org/\">
		  <img src=\"img/CloudFoundryCorp_vertical.png\" alt=\"CloudFoundry Logo\" style=\"width:256px;height:128px;border:0\">
		</a>
    </th>
  </tr>
  <tr>
    <td colspan=\"3\">This deployed matomo-cf-service supports several Matomo releases that have been prepared to be deployed by the Matomo service broker.</td>
  </tr>
  <tr>
    <td align=\"left\">Available versions are (default is " >${GENERATE_DIR}/index.html
 cat ${GENERATE_DIR}/matomo-releases/DefaultVersion >>${GENERATE_DIR}/index.html
 echo -n "):</td>
    <td></td>
    <td></td>
  </tr>" >>${GENERATE_DIR}/index.html
for i in `cat ${GENERATE_DIR}/matomo-releases/VersionsCR`
do
	echo -n "
  <tr>
    <td></td>
    <td align=\"left\">- ${i}</td>
    <td align=\"left\">
		<a download=\"CHANGELOG.md\" href=\"matomo-releases/${i}/CHANGELOG.md\">
		  Change Log
		</a>
    </td>
  </tr>" >>${GENERATE_DIR}/index.html
done
echo -n "
</table> 
</body>
</html>" >>${GENERATE_DIR}/index.html
