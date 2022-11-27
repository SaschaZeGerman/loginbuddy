# About Loginbuddy 

**Latest update: 27. November 2022! See what has changed here: [WIKI](https://github.com/SaschaZeGerman/loginbuddy/wiki/Latest-and-Greatest)**

This project is simplifying the job for developers who need social login for their applications. Social login has in recent years become the norm for many app developers as it provides a number of benefits to end users, developers and enterprises. Less passwords to remember (and manage), less fritction in user onboarding to app, and richer use data for app developers to use in app.

Most social login providers use OpenID Connect and OAuth 2.0 to establish their OpenID Provider. However, these technologies offer multiple flows and scenarios and implementations often use different parameters. This makes it hard for developers who need to integrate to multiple OpenID Providers and provide a consistent user experience across their apps. 

Loginbuddy is a container based solution that implements an OpenID Connect client that can be used as proxy between an application (your application) and an OpenID Provider. Your application only needs to communicate with Loginbuddy. After finishing the authentication and authorization flow with providers, Loginbuddy generates a single response through a stable, normalized, interface to the application.

The high level design looks like this:

![alt overview](doc/simple_overview_01.png)

## Benefits

* **Simple to use** solution for social login letting you focus on your most important aspects of your app value 
* Adding new OpenID Provider **within minutes** through configuration instead of error prone coding 
* Provides a **normalized and stable interface** for your app through protocol and parameter mapping behind the scene
* The web application **only needs to connect to Loginbuddy** and not to external servers
* **Flexible deployment options** with Loginbuddy running as standalone OpenID Connect proxy server or as a sidecar container
* Support of **OpenID Connect Dynamic Registration** to provide users with option to use the OpenID Configuration URL of their own provider
* **Enhanced security** as Loginbuddy validates a request as strict as possible in order to reduce the number of invalid requests being sent to OpenID Providers
* **Enforces HTTPS** and runs with a security manager to leverage best practices security polices with minimal privileges
* **No stored private keys** by leveraging on-the fly key-generation

# Getting started 

Loginbuddy offers multiple ways for getting started depending on your objective. You can run the online demo or take pre-canned demos for a spin on your local machine. You can clone the repo and start setting up Loginbuddy for your application or you can start contributing to Loginbuddy with your own use cases. 

Loginbuddy includes configuration templates for common OpenID providers which simplifies the configuration effort:

- Google
- GitHub
- LinkedIn
- Sign In with Apple
- PingOne (Ping Identity)
- Amazon (Login with Amazon)
- OpenID Dynamic registration
- self-issued

## No time to read? Try the online demo

There is an online demo of Loginbuddy here: [https://client.loginbuddy.net](https://client.loginbuddy.net).
The demo simulates a client, a social login provider (called 'FAKE') and uses Loginbuddy. The resulting page displays the content your client would receive after
a user would complete an authentication/ authorization flow!

**Note**: It can be offline at times if something breaks or new updates are being pushed.

## Run the pre-canned demo setup locally 

The sample setup consists of three components:
- Loginbuddy
- Sample web application
- Sample OpenID Provider

The instructions are made for Docker on a MacBook and may need to be adjusted for Windows users.

- Add this line to your hosts file: **127.0.0.1 local.loginbuddy.net demoserver.loginbuddy.net democlient.loginbuddy.net**
  - for MacBooks this would be done at: `/etc/hosts`
  - for Windows this would be done at: `C:\Windows\System32\drivers\etc\hosts`
- Run `docker run --name loginbuddy-demo -p 80:80 -p 443:443 -d saschazegerman/loginbuddy-demo:latest`
  - this will pull the latest demo image from docker hub
  - this will use ports 80 (http) and 443 (https)
- Open a browser
  - go to **https://democlient.loginbuddy.net** and follow the prompts

The demo simulates a client, a social login provider (called 'FAKE') and uses Loginbuddy!

The last page displays the type of message Loginbuddy would return to your application (if you have used the web app demo client, copy the content, paste it into [JSONLINT](https://jsonlint.com) and click 'Validate JSON' to make it look nice).

Since the demo uses self-signed certificates, confirm the SSL security screens in your browser, three times, once per component. 

To stop the docker container when you are done, run the following:

- `docker stop loginbuddy-demo`

### Run the demosetup using http

If your browser does not accept websites that use self-signed SSL certificates, try Safari!
If that is not an option, please follow the guide for running the demosetup with http here:
- [WIKI - Quick Start](https://github.com/SaschaZeGerman/loginbuddy/wiki/Quick-Start), look for **Using http**

# API and Protocols 

Loginbuddy is built to support OpenID Connect and OAuth 2.0 specifications:

* OAuth 2.0
* OpenID Connect Core
* OpenID Connect Discovery
* OpenID Connect Dynamic Registration
* OAuth 2.0 Pushed Authorization Requests

For more details on the APIs supported see [WIKI - Protocols and APIs](https://github.com/SaschaZeGerman/loginbuddy/wiki/Protocols-and-APIs)

# Other Resources

The latest docker images are always available at [docker hub](https://hub.docker.com/search?q=loginbuddy&type=image).

To get a better idea how it works I have published a few videos about Loginbuddy on YouTube: [Loginbuddy playlist](https://www.youtube.com/playlist?list=PLcX_9uDXp_CR5vXTT8lxI94x7Esl8O78E)

# WIKI

All documentation for Loginbuddy can be found in the local [WIKI](https://github.com/SaschaZeGerman/loginbuddy/wiki/HOME)

# License

Copyright (c) 2022. All rights reserved.

This software may be modified and distributed under the terms of the Apache License 2.0 license. See the [LICENSE](/LICENSE) file for details.