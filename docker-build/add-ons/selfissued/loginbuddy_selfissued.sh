#!/bin/bash

# configures the environment variables HOSTNAME_LOGINBUDDY to 'loginbuddy-selfissued' and SSL_PORT to 445
#
printf "===============\n"
printf "== Loginbuddy: Overwriting HOSTNAME_LOGINBUDDY and SSL_PORT to loginbuddy-selfissued and 445\n"
printf "== Loginbuddy: Currently these values are required for this setup\n"
printf "===============\n"

export HOSTNAME_LOGINBUDDY=loginbuddy-selfissued
export SSL_PORT=445

# run the original loginbuddy entrypoint script
#
sh /opt/docker/loginbuddy.sh