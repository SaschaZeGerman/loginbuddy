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
    keytool -printcert -sslserver loginbuddy-oidcdr:445 -rfc > pubCert 2<&1
    connected=$(cat pubCert | grep -qi 'BEGIN' && echo TRUE || echo FALSE)
    if [ "TRUE" = "${connected}" ]
    then
      echo "connecting to loginbuddy-oidcdr"
      # Import loginbuddy-oidcdr certificates as trusted certificate
      #
      cat pubCert > /usr/local/tomcat/ssl/loginbuddy-oidcdr.crt
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