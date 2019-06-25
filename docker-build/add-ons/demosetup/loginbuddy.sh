#!/bin/bash

#
# For development purposes, this script generates two private keys on the fly. One for the test client, one for the test server.
# SNI (Server Name Indication) is used to lookup the matching certificate (private key).

# Create a directory for ssl certificates
#
mkdir /usr/local/tomcat/ssl

# Create private key

# Key for demosetup:
#
keytool -genkey -alias demosetup -keystore /usr/local/tomcat/ssl/demosetup.p12 -storetype PKCS12 -keyalg RSA -storepass changeit -keypass changeit -validity 1 -keysize 2048 -dname "CN=local.loginbuddy.net" -ext san=dns:demoserver.loginbuddy.net,dns:democlient.loginbuddy.net

# Export the public certificates
#
keytool -export -alias demosetup -file /usr/local/tomcat/ssl/demosetup.crt -keystore /usr/local/tomcat/ssl/demosetup.p12 -storepass changeit

# Import the certs as trusted certificates
#
keytool -importcert -alias demosetup -file /usr/local/tomcat/ssl/demosetup.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security