#!/bin/bash

## check if dynamic registration providers should be supported
#
iodcdr=$(echo "${SUPPORT_OIDCDR}" | grep -qi '^TRUE$' && echo TRUE || echo FALSE)
if [ "TRUE" = "${iodcdr}" ]
then

  # Wait for loginbuddy-oidcdr to import its certificate:
  #
  MAX_RETRIES=5
  INTERVAL=5
  retry_count=0

  connected=false
  until [ $retry_count -eq $MAX_RETRIES ] || [ $connected == true ]
  do
    nc -w 1 -v loginbuddy-oidcdr 445 > result 2>&1
    connected=$(cat result | grep -qi 'open' && echo TRUE || echo FALSE)
    if [ "TRUE" = "${connected}" ]
    then
      echo "connecting to loginbuddy-oidcdr"
      # Import loginbuddy-oidcdr certificates as trusted certificate
      #
      keytool -printcert -sslserver loginbuddy-oidcdr:445 -rfc > /usr/local/tomcat/ssl/loginbuddy-oidcdr.crt
      keytool -importcert -alias loginbuddy-oidcdr -file /usr/local/tomcat/ssl/loginbuddy-oidcdr.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt
      exit 0
    else
      echo "waiting for loginbuddy-oidcdr..."
      /bin/sleep $INTERVAL
      retry_count=$(( retry_count + 1 ))
    fi
  done
else
  echo "loginbuddy-oidcdr was not requested"
fi