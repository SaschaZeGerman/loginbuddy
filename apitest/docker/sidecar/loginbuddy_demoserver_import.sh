#!/bin/bash

# Wait for loginbuddy-demoserver to import its certificate:
#
MAX_RETRIES=5
INTERVAL=5
retry_count=0

connected=false
until [ $retry_count -eq $MAX_RETRIES ] || [ $connected == true ]
do
  keytool -printcert -sslserver loginbuddy-demoserver:443 -rfc > pubCert 2<&1
  connected=$(cat pubCert | grep -qi 'BEGIN' && echo TRUE || echo FALSE)
  if [ "TRUE" = "${connected}" ]
  then
    echo "connecting to loginbuddy-demoserver"
    # Import loginbuddy-demoserver certificates as trusted certificate
    #
    cat pubCert > /usr/local/tomcat/ssl/loginbuddy-demoserver.crt
    keytool -importcert -alias loginbuddy-demoserver -file /usr/local/tomcat/ssl/loginbuddy-demoserver.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt
    exit 0
  else
    echo "waiting for loginbuddy-demoserver..."
    /bin/sleep $INTERVAL
    retry_count=$(( retry_count + 1 ))
  fi
done