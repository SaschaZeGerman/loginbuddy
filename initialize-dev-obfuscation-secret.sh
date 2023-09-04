#!/bin/bash

# This script generates a shared secret that is used to encrypt/ decrypt access_token and refresh_token.

# create a shared secret to encrypt/decrypt provider issued token when obfuscation is required
#
secret_obfuscation=$(openssl rand -base64 32 | tr -d '=' | tr -d '/' | tr -d '+')

# Only generate the secret for obfuscation if it does not exist yet
#
if [ -z "$(cat .env | grep SECRET_OBFUSCATION)" ]
then
  printf "\nSECRET_OBFUSCATION=${secret_obfuscation}" >> .env
fi