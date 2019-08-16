#!/bin/bash

# Create private key

# Keys for sidecar scenario:
#
keytool -genkey -alias sidecar -keystore /usr/local/tomcat/ssl/loginbuddy_sidecar.p12 -storetype PKCS12 -keyalg RSA -storepass changeit -keypass changeit -validity 90 -keysize 2048 -dname "CN=loginbuddy-sidecar"

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security