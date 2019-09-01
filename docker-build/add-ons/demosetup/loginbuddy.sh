#!/bin/bash

#
# a default hostname will be used if none was given
#
if [ -z "$HOSTNAME_LOGINBUDDY" ]
then
  printf "===============\n"
  printf "== Loginbuddy: Using the default hostname local.loginbuddy.net\n"
  printf "== Loginbuddy: Please configure the enviroment variable HOSTNAME_LOGINBUDDY containing the hostname you want to use\n"
  printf "== Loginbuddy: use: docker run -e HOSTNAME_LOGINBUDDY=yourhostname saschazegerman/loginbuddy:latest\n"
  printf "== Loginbuddy: or add HOSTNAME_LOGINBUDDY=yourhostname in the environments section in docker-compose.yml\n"
  printf "===============\n"
  HOSTNAME_LOGINBUDDY=local.loginbuddy.net
fi

# setting the SSL port to 443 if none was given
#
if [ -z "$SSL_PORT" ]
then
  printf "===============\n"
  printf "== Loginbuddy: Using default SSL port 443\n"
  printf "===============\n"
  SSL_PORT=443
fi

# generating a UUID as password for the generated keystore
#
UUID=$(cat /proc/sys/kernel/random/uuid)

# Create private key
#
keytool -genkey -alias loginbuddy -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storetype PKCS12 -keyalg RSA -storepass ${UUID} -keypass ${UUID} -validity 1 -keysize 2048 -dname "CN=${HOSTNAME_LOGINBUDDY}" -ext san=dns:${HOSTNAME_LOGINBUDDY},dns:demoserver.loginbuddy.net,dns:democlient.loginbuddy.net

# Export the public certificates
#
keytool -export -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storepass ${UUID}

# Import the certs as trusted certificates
#
keytool -importcert -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt

## Find the policy file that contains socket permissions and add them to the default catalina.policy file
# default is located here: /usr/local/tomcat/conf/catalina.policy
#
cat /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/permissions.policy >> /usr/local/tomcat/conf/catalina.policy

# specifying that 'none' is invalid for JWT signature algorithms
#
export CATALINA_OPTS="${SYSTEM_PROPS} -Dorg.jose4j.jws.default-allow-none=false"

# replace @@variable@@ in server.xml with the real values
#
sed -i "s/@@hostname@@"/${HOSTNAME_LOGINBUDDY}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@sslport@@"/${SSL_PORT}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@sslpwd@@"/${UUID}/g /usr/local/tomcat/conf/server.xml

# Wait for loginbuddy-selfissued to import its certificate:
#
MAX_RETRIES=5
INTERVAL=5
retry_count=0

until nc -w 1 -v loginbuddy-selfissued 445; do
    if [ $retry_count -gt $MAX_RETRIES ]; then
        echo "Max retry exceeded while waiting for loginbuddy-selfissued to come up"
        exit 1
    fi
    echo "waiting for loginbuddy-selfissued..."
    /bin/sleep $INTERVAL
    retry_count=$(( retry_count + 1 ))
done

# Import loginbuddy-selfissued certificates as trusted certificate
#
keytool -printcert -sslserver loginbuddy-selfissued:445 -rfc > /usr/local/tomcat/ssl/loginbuddy-selfissued.crt
keytool -importcert -alias loginbuddy-selfissued -file /usr/local/tomcat/ssl/loginbuddy-selfissued.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt

# overwrite the variables since they are not needed anywhere anymore
# for this demo do wnot overwrite HOSTNAME_LOGINBUDDY!
#
export SSL_PORT=
export UUID=

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security