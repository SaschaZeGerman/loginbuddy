#!/bin/bash

# configures the environment variables HOSTNAME_LOGINBUDDY to 'loginbuddy-sidecar' and SSL_PORT to 444
#
printf "===============\n"
printf "== Loginbuddy: Overwriting HOSTNAME_LOGINBUDDY and SSL_PORT to loginbuddy-sidecar and 444\n"
printf "== Loginbuddy: Currently these values are required for this setup\n"
printf "===============\n"

export HOSTNAME_LOGINBUDDY=loginbuddy-sidecar
export SSL_PORT=444

# generating a UUID as password for the generated keystore
#
UUID=$(cat /proc/sys/kernel/random/uuid)

# Create private key
#
keytool -genkey -alias loginbuddy -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storetype PKCS12 -keyalg RSA -storepass ${UUID} -keypass ${UUID} -validity 90 -keysize 2048 -dname "CN=${HOSTNAME_LOGINBUDDY}"

# Export the public certificates
#
#keytool -export -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storepass ${UUID}

# Import the certs as trusted certificates
#
#keytool -importcert -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt

# Find the policy file that contains socket permissions and add them to the default catalina.policy file
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

# check if dynamic registration should be supported
#
sh /opt/docker/loginbuddy_oidcdr_import.sh

# overwrite the variables since they are not needed anywhere anymore
#
export HOSTNAME_LOGINBUDDY=
export SSL_PORT=
export UUID=

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security