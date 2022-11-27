#!/bin/bash

# This script generates an initial keypair for development purposes.
# Once created, the Loginbuddy containers will reuse this key until this script is run again.

# Exporting environment variables for key creation
#
export $(cat .env | grep HOSTNAME_LOGINBUDDY)
export $(cat .env | grep SSL_PWD)

# Remove an existing keypair,  a new one will be created
#
rm -f dev/loginbuddy.p12

# Create private key
#
keytool -genkey \
  -alias loginbuddy \
  -keystore dev/loginbuddy.p12 \
  -storetype PKCS12 \
  -keyalg RSA -storepass ${SSL_PWD} \
  -keypass ${SSL_PWD} \
  -validity 365 \
  -keysize 2048 \
  -dname "CN=${HOSTNAME_LOGINBUDDY}" \
  -ext san=dns:${HOSTNAME_LOGINBUDDY},dns:localhost

# Remove all variables
#
unset HOSTNAME_LOGINBUDDY
unset SSL_PWD