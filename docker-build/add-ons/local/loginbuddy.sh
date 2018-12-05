#!/bin/bash
#
# For development purposes, this script generates a private key on the fly.

# Create a directory for ssl certificates
#
mkdir /usr/local/tomcat/ssl

# Create private key, including subject alternative name
#
keytool -genkey -alias loginbuddy -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storetype PKCS12 -keyalg RSA -storepass changeit -keypass changeit -validity 1 -keysize 2048 -dname "CN=local.loginbuddy.net" -ext san=dns:server.loginbuddy.net

# Export the public certificate
#
keytool -export -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storepass changeit

# Import that cert as trusted certificate
#
$JAVA_HOME/bin/keytool -importcert -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt

# run the original tomcat entrypoint command as specified in Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security