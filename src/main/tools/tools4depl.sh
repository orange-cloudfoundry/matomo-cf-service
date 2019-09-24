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

TARGETDIR=`dirname $0`/../../../target/classes/tools
mkdir -p ${TARGETDIR}
sed -n 1,24p `dirname $0`/scanrel.sh >`dirname $0`/scanrel.sh.tmp
sed -n 28,28p `dirname $0`/scanrel.sh >>`dirname $0`/scanrel.sh.tmp
sed -n 33,33p `dirname $0`/scanrel.sh >>`dirname $0`/scanrel.sh.tmp
sed -n 35,\$p `dirname $0`/scanrel.sh >>`dirname $0`/scanrel.sh.tmp
sed -e "s/\.\.\/\.\.\/target\/classes\///" `dirname $0`/scanrel.sh.tmp >${TARGETDIR}/scanrel.sh
rm -f `dirname $0`/scanrel.sh.tmp
chmod a+x ${TARGETDIR}/scanrel.sh
sed -e "s/\.\.\/\.\.\/target\/classes\///" `dirname $0`/piwik2cf.sh >${TARGETDIR}/piwik2cf.sh
chmod a+x ${TARGETDIR}/piwik2cf.sh
cp `dirname $0`/*.txt ${TARGETDIR}
