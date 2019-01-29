#!/bin/bash
#
# For development purposes, this script generates two private keys on the fly. One for the test client, one for the test server.
# SNI (Server Name Indication) is used to lookup the matching certificate (private key).

# Create a directory for ssl certificates
#
mkdir /usr/local/tomcat/ssl

# Create private keys
#
# Key for loginbuddy:
#
keytool -genkey -alias loginbuddy -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storetype PKCS12 -keyalg RSA -storepass changeit -keypass changeit -validity 1 -keysize 2048 -dname "CN=local.loginbuddy.net"

# Key for 'fake' provider (server):
keytool -genkey -alias loginbuddy-server -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storetype PKCS12 -keyalg RSA -storepass changeit -keypass changeit -validity 1 -keysize 2048 -dname "CN=server.loginbuddy.net"

# FYI: to create a key with a subject alternative name, add this to the command:
# ... "CN=local.loginbuddy.net" -ext san=dns:nonlocal.loginbuddy.net(,dns:another.hostname.com)*

# Export the public certificates
#
keytool -export -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storepass changeit
keytool -export -alias loginbuddy-server -file /usr/local/tomcat/ssl/loginbuddy-server.crt -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storepass changeit

# Import the certs as trusted certificates
#
keytool -importcert -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt
keytool -importcert -alias loginbuddy-server -file /usr/local/tomcat/ssl/loginbuddy-server.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security