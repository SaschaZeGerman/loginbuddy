#!/bin/bash

# This script may be used to generate an initial private key for development purposes.
# Once created, Loginbuddy will reuse this key until this script is run again.

# clean up a previously script execution
#
rm -f .env
rm -f dev/loginbuddy.p12

# create the private keys secret
#
secret=$(openssl rand -base64 32 | tr -d '=' | tr -d '/' | tr -d '+')

# Copy dev/.env_template to .env
#
cp dev/.env_template .env

# Exporting hostnames as environment variables
#
export $(cat .env | grep HOSTNAME_LOGINBUDDY)
export $(cat .env | grep HOSTNAME_LOGINBUDDY_DEMOCLIENT)
export $(cat .env | grep HOSTNAME_LOGINBUDDY_DEMOSERVER)
echo "\nSSL_PWD=${secret}" >> .env

# Create private key
#
keytool -genkey \
  -alias loginbuddy \
  -keystore dev/loginbuddy.p12 \
  -storetype PKCS12 \
  -keyalg RSA -storepass ${secret} \
  -keypass ${secret} \
  -validity 365 \
  -keysize 2048 \
  -dname "CN=${HOSTNAME_LOGINBUDDY}" \
  -ext san=dns:${HOSTNAME_LOGINBUDDY},dns:${HOSTNAME_LOGINBUDDY_DEMOSERVER},dns:${HOSTNAME_LOGINBUDDY_DEMOCLIENT}