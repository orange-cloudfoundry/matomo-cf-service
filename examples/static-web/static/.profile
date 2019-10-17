echo "Inject credentials from VCAP_SERVICES into Matomo tracking code"
MSID=`echo ${VCAP_SERVICES} | jq '."matomo-service"[0].credentials."mcfs-siteId"'`
MURL=`echo ${VCAP_SERVICES} | jq '."matomo-service"[0].credentials."mcfs-matomoUrl"' | sed -e "s/\"//g"`
echo -e "siteId=${MSID}, matomoUrl=${MURL}"
cat ${HOME}/public/scripts/matomoInst.tpl.js | sed -e "s/MATOMO_URL/${MURL}/" -e "s/MATOMO_SITEID/${MSID}/" >${HOME}/public/scripts/matomoInst.js
