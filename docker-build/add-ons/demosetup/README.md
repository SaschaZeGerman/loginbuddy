# Demosetup

This setup combines the democlient, demoserver and Loginbuddy. The goal is to provide an easy to use starting point.

## Files

- **config:** includes demo configurations
- **discovery:** includes teh demosetup discovery endpoints
- **loginbuddy.sh:** an extended entrypoint script to configure Loginbuddy, democlient and demoserver
- **permissions.policy:** an extended permissions policy to configure Loginbuddy, democlient and demoserver
- **server.xml:**  an extended server.xml file to support three different domains: local.loginbuddy.net, democlient.loginbuddy.net, demoserver.loginbuddy.net
- **.png** provider images for Loginbuddys *Fake* provider

## Directories

- **http_demo**: contains files to support the usage of *localhost* for loginbuddy and *http* for running the demosetup locally

# Usage

All these files are used with:

- **/Dockerfile_demoserver**
- **/docker-compose-demosetup.yml**
- **/docker-compose-demosetup-http.yml**