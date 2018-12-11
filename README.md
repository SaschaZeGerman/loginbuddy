# About

A container based solution that helps developers support social login for their applications in a simple way!

The latest docker image is always available at [docker hub](https://hub.docker.com/r/saschazegerman/loginbuddy/).

A running version of the latest loginbuddy is here: [http://latest.loginbuddy.net](http://latest.loginbuddy.net). (If the URL is not working, I am trying to figure out something new)

# Loginbuddy - Your authenticator

This project implements an OpenID Connect client that can be used as proxy between an application (your application) and 
an OpenID Provider. This is useful for cases where social login should be an option for users to login to your 
application. Your application needs to make two simple calls only and the response will be users details. Loginbuddy 
takes care of the complete OAuth/ OpenID Connect protocol.

This is the visual overview of the design:

![alt overview](doc/overview_700.png)

# Getting started

## Running the latest and greatest container of Loginbuddy:

I have only used loginbuddy on a MacBook, if you run it on Windows, some instructions may need to be adjusted!

- Preparation
  - modify your hosts file, add **127.0.0.1 local.loginbuddy.net server.loginbuddy.net**
  - for MacBooks this would be done at ```sudo /etc/hosts```
- Download or clone this project
  - ```git clone https://github.com/SaschaZeGerman/loginbuddy.git```
  - Switch into the directory *.../loginbuddy*
- Run ```docker-compose -f docker-compose-start.yml up -d```
  - this will pull the latest image from docker hub
  - this will also create a private key, within the container, on the fly, used for testing and development purposes. See 'docker-build/add-ons/local/loginbuddy.sh' for details
  - the private key's subject is **CN=local.loginbuddy.net**. The subject alternative name is **server.loginbuddy.net** to simulate a provider
  - this will use ports 80 (http), 443 (https)
- Open a browser
  - go to **http://local.loginbuddy.net** and follow the prompts
  - the following screen will display an image saying **fake** which is the demo provider. Just click it ...
  - the demo takes your through the (simulated) typical authentication/ authorization flow
  - the response at the end is completely fake but it represents the type of content that can be expected 

That's it! In a real life scenario the 'fake' image would be replaced by images of real providers!

# Current state

Please check the project page: [projects](https://github.com/SaschaZeGerman/loginbuddy/projects).

To find out more details on how to build and configure Loginbuddy, please visit the [WIKI](https://github.com/SaschaZeGerman/loginbuddy/wiki) pages!

# License

Copyright (c) 2018 CA. All rights reserved.

This software may be modified and distributed under the terms of the Apache License 2.0 license. See the [LICENSE](/LICENSE) file for details.