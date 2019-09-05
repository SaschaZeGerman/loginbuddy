#!/bin/bash

## check if self issued providers should be supported
#
selfissued=$(echo "${SUPPORT_SELFISSUED}" | grep -qi '^TRUE$' && echo TRUE || echo FALSE)
if [ "TRUE" = "${selfissued}" ]
then

  # Wait for loginbuddy-selfissued to import its certificate:
  #
  MAX_RETRIES=5
  INTERVAL=5
  retry_count=0

  connected=false
  until [ $retry_count -eq $MAX_RETRIES ] || [ $connected == true ]
  do
    nc -w 1 -v loginbuddy-selfissued 445 > result 2>&1
    connected=$(cat result | grep -qi 'open' && echo TRUE || echo FALSE)
    if [ "TRUE" = "${connected}" ]
    then
      echo "connecting to loginbuddy-selfissued"
      # Import loginbuddy-selfissued certificates as trusted certificate
      #
      keytool -printcert -sslserver loginbuddy-selfissued:445 -rfc > /usr/local/tomcat/ssl/loginbuddy-selfissued.crt
      keytool -importcert -alias loginbuddy-selfissued -file /usr/local/tomcat/ssl/loginbuddy-selfissued.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt
      exit 0
    else
      echo "waiting for loginbuddy-selfissued..."
      /bin/sleep $INTERVAL
      retry_count=$(( retry_count + 1 ))
    fi
  done
else
  echo "loginbuddy-selfissued was not requested"
fi