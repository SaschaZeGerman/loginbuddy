#!/bin/bash

# Wait for loginbuddy-demoserver to import its certificate:
#
MAX_RETRIES=5
INTERVAL=5
retry_count=0

connected=false
until [ $retry_count -eq $MAX_RETRIES ] || [ $connected == true ]
do
  nc -w 1 -v loginbuddy-demoserver 443 > result 2>&1
  connected=$(cat result | grep -qi 'open' && echo TRUE || echo FALSE)
  if [ "TRUE" = "${connected}" ]
  then
    echo "connecting to loginbuddy-demoserver"
    # Import loginbuddy-demoserver certificates as trusted certificate
    #
    keytool -printcert -sslserver loginbuddy-demoserver:443 -rfc > /usr/local/tomcat/ssl/loginbuddy-demoserver.crt
    keytool -importcert -alias loginbuddy-demoserver -file /usr/local/tomcat/ssl/loginbuddy-demoserver.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt
    exit 0
  else
    echo "waiting for loginbuddy-demoserver..."
    /bin/sleep $INTERVAL
    retry_count=$(( retry_count + 1 ))
  fi
done