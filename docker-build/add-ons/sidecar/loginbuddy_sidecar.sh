#!/bin/bash

# configures the environment variables HOSTNAME_LOGINBUDDY to 'loginbuddy-sidecar' and SSL_PORT to 444
#
printf "===============\n"
printf "== Loginbuddy: Overwriting HOSTNAME_LOGINBUDDY and SSL_PORT to loginbuddy-sidecar and 444\n"
printf "== Loginbuddy: Currently these values are required for this setup\n"
printf "===============\n"

export HOSTNAME_LOGINBUDDY=loginbuddy-sidecar
export SSL_PORT=444

# run the original loginbuddy entrypoint script
#
sh /opt/docker/loginbuddy.sh