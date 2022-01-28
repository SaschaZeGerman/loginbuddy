#!/bin/bash

#
# a default hostname will be used if none was given
#
if [ -z "$HOSTNAME_LOGINBUDDY" ]
then
  printf "===============\n"
  printf "== Loginbuddy: Using the default hostname localhost for the HTTP demo\n"
  printf "== Loginbuddy: Please configure the enviroment variable HOSTNAME_LOGINBUDDY containing the hostname you want to use\n"
  printf "== Loginbuddy: use: docker run -e HOSTNAME_LOGINBUDDY=yourhostname saschazegerman/loginbuddy:latest\n"
  printf "== Loginbuddy: or add HOSTNAME_LOGINBUDDY=yourhostname in the environments section in docker-compose.yml\n"
  printf "===============\n"
  HOSTNAME_LOGINBUDDY=localhost
fi

if [ -z "$HOSTNAME_LOGINBUDDY_DEMOCLIENT" ]
then
  printf "===============\n"
  printf "== Loginbuddy: Using the default democlient hostname democlient.loginbuddy.net\n"
  printf "== Loginbuddy: Please configure the enviroment variable HOSTNAME_LOGINBUDDY_DEMOCLIENT containing the hostname you want to use\n"
  printf "== Loginbuddy: use: docker run -e HOSTNAME_LOGINBUDDY_DEMOCLIENT=yourhostname saschazegerman/loginbuddy-demo:latest\n"
  printf "== Loginbuddy: or add HOSTNAME_LOGINBUDDY_DEMOCLIENT=yourhostname in the environments section in docker-compose-demosetup.yml\n"
  printf "===============\n"
  HOSTNAME_LOGINBUDDY_DEMOCLIENT=democlient.loginbuddy.net
fi

if [ -z "$HOSTNAME_LOGINBUDDY_DEMOSERVER" ]
then
  printf "===============\n"
  printf "== Loginbuddy: Using the default demoserver hostname demoserver.loginbuddy.net\n"
  printf "== Loginbuddy: Please configure the enviroment variable HOSTNAME_LOGINBUDDY_DEMOSERVER containing the hostname you want to use\n"
  printf "== Loginbuddy: use: docker run -e HOSTNAME_LOGINBUDDY_DEMOSERVER=yourhostname saschazegerman/loginbuddy-demo:latest\n"
  printf "== Loginbuddy: or add HOSTNAME_LOGINBUDDY_DEMOSERVER=yourhostname in the environments section in docker-compose-demosetup.yml\n"
  printf "===============\n"
  HOSTNAME_LOGINBUDDY_DEMOSERVER=demoserver.loginbuddy.net
fi

# Find the policy file that contains socket permissions and add them to the default catalina.policy file
# default is located here: /usr/local/tomcat/conf/catalina.policy
#
cat /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/permissions.policy >> /usr/local/tomcat/conf/catalina.policy

# Check if hazelcast is used. In that case, add required permissions
#
if [ -z "$HAZELCAST" ]
then
  printf "Using local cache.\n"
else
  printf "Attempting to use remote cache with Hazelcast. Adding required permissions\n"
  cat /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/hazelcastPermissions.policy >> /usr/local/tomcat/conf/catalina.policy
fi

# specifying that 'none' is invalid for JWT signature algorithms
#
export CATALINA_OPTS="${SYSTEM_PROPS} -Dorg.jose4j.jws.default-allow-none=false"

# replace @@variable@@ in server.xml with the real values
#
cp /opt/docker/server_http.xml /usr/local/tomcat/conf/server.xml
sed -i "s/@@hostname@@"/${HOSTNAME_LOGINBUDDY}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@hostname_demoserver@@"/${HOSTNAME_LOGINBUDDY_DEMOSERVER}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@hostname_democlient@@"/${HOSTNAME_LOGINBUDDY_DEMOCLIENT}/g /usr/local/tomcat/conf/server.xml

# check if self issued providers should be supported
# this script is part of the default loginbuddy docker image
#
sh /opt/docker/loginbuddy_oidcdr_import.sh

# overwrite the variables since they are not needed anywhere anymore. For this demo we do not overwrite HOSTNAME_LOGINBUDDY!
#
#export HOSTNAME_LOGINBUDDY=
export UUID=

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security