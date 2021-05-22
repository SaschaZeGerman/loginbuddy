#!/bin/bash

printf "===============\n"
printf "== Loginbuddy: Overwriting HOSTNAME_LOGINBUDDY and PORTs to loginbuddy-sidecar and 444 (SSL) and 8044 (http)\n"
printf "== Loginbuddy: Currently these values are required for this setup\n"
printf "===============\n"

export HOSTNAME_LOGINBUDDY=loginbuddy-sidecar
export SSL_PORT=444
export HTTP_PORT=8044

# generating a UUID as password for the generated keystore
#
UUID=$(cat /proc/sys/kernel/random/uuid)

# Create private key
#
keytool -genkey -alias loginbuddy -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storetype PKCS12 -keyalg RSA -storepass ${UUID} -keypass ${UUID} -validity 90 -keysize 2048 -dname "CN=${HOSTNAME_LOGINBUDDY}"

# Find the policy file that contains socket permissions and add them to the default catalina.policy file
# default is located here: /usr/local/tomcat/conf/catalina.policy
#
cat /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/permissions.policy >> /usr/local/tomcat/conf/catalina.policy

# Check if hazelcast is used. In that case, add required permissions
#
if [ -z "$HAZELCAST" ]
then
  printf "Using local cache."
else
  printf "Attempting to use remote cache with Hazelcast. Adding required permissions\n"
  cat /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/hazelcastPermissions.policy >> /usr/local/tomcat/conf/catalina.policy
fi

# specifying that 'none' is invalid for JWT signature algorithms
#
export CATALINA_OPTS="${SYSTEM_PROPS} -Dorg.jose4j.jws.default-allow-none=false"

# replace @@variable@@ in server.xml with the real values
#
sed -i "s/@@hostname@@"/${HOSTNAME_LOGINBUDDY}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@sslport@@"/${SSL_PORT}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@httpport@@"/${HTTP_PORT}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@sslpwd@@"/${UUID}/g /usr/local/tomcat/conf/server.xml

# check if dynamic registration should be supported
#
sh /opt/docker/loginbuddy_oidcdr_import.sh

# overwrite the variables since they are not needed anywhere anymore
#
export HOSTNAME_LOGINBUDDY=
export SSL_PORT=
export HTTP_PORT=
export UUID=

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security