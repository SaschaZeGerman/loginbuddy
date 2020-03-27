# About Loginbuddy 

This project is simplifying the job for developers who need social login for their applications. Social login has in recent years become the norm for many app developers as it provides a number of benefits to end users, developers and enterprises. Less passwords to remember (and manage), less fritction in user onboarding to app, and richer use data for app developers to use in app.

Most social login providers use OpenID Connect and OAuth 2.0 to establish their OpenID Provider. However, these technologies offer multiple flows and scenarios and implementations often use different parameters. This makes it hard for developers who need to integrate to multiple OpenID Providers and provide a consistent user experience across their apps. 

The Loginbuddy is a container based solution that  implements an OpenID Connect client that can be used as proxy between an application (your application) and  an OpenID Provider. Your application only needs to communicate with Loginbuddy. After finishing the authentication and authorization with providers, Loginbuddy provides single response through a stable, normalized, interface to the application.

The high level design looks like this:

![alt overview](doc/simple_overview_01.png)



## Benefits
* **Simple to use** solution for social login letting you focus on your most important aspects of your app value 
* Adding new OpenID Provider **within minutes** through configuration instead of error prone coding 
* Provides a **normalized and stable interface** for your app through protocol and parameter mapping behind the scene
* The web application **only needs to connect to Loginbuddy locally** and not to external servers
*** Flexible deployment options** with Loginbuddy running as standalone OpenID Connect proxy server or as a sidecar container. 
* Support of **OpenID Connect Dynamic Registration** to provide users with option to use the OpenID Configuration URL of their own provider.
* **Enhanced security** as Loginbuddy validates a request as strict as possible in order to reduce the number of invalid requests being sent to OpenID Providers. 
* **Enforces HTTPS** and runs with a security manager to leverage best practices security polices with minimal privilage. 
* **No stored private keys** by leveraging on-the fly key-generation with Let's Enrypt 

# Getting started 
Loginbuddy offers multiple ways of getting started depending on your objective. You can run online demos or take pre-canned demo for a spin on local machine. You can clone the repo and start setting up Loginbuddy for your application or you can start contributing to Loginbuddy with your own use cases. 

## Run the online demo 
There is an online demo of Loginbuddy here: [https://client.loginbuddy.net](https://client.loginbuddy.net). 
The demo simulates a client, a social login provider (called 'FAKE') and uses Loginbuddy. 

Note: It can be offline at times if something breaks or new updates are being pushed and the SLA is best-effort. 


## Run the pre-canned demo setup locally 
The sample setup consists of three components:
- Loginbuddy
- Sample web application
- Sample OpenID Provider

The instructions are made for Docker on a MacBook and may need to be adjusted for windows users.

- Preparation
  - modify your hosts file, add **127.0.0.1 local.loginbuddy.net demoserver.loginbuddy.net democlient.loginbuddy.net**
  - for MacBooks this would be done at ```sudo /etc/hosts```
- Run ```docker run -p 80:80 -p 443:443 -d saschazegerman/loginbuddy:latest-demo```
  - this will pull the latest demo image from docker hub
  - this will use ports 80 (http), 443 (https)
- Open a browser
  - go to **https://democlient.loginbuddy.net** and follow the prompts

The demo simulates a client, a social login provider (called 'FAKE') and uses Loginbuddy!

The last page displays the type of message Loginbuddy would return to your application. (the window is very small. Copy the content, paste it into [JSONLINT](https://jsonlint.com) and click 'Validate JSON').

Since the demo uses self-signed certificates, confirm the SSL security screens in your browser, three times, once per component. 



## Run your own development setup 

To develop with LoginBuddy you will need a couple of tools: 

* java jdk8
* maven
* docker
* docker-compose

With these tools installed the steps to build :

* Preparation
	* Clone the Loginbuddy repo:  `git clone https://github.com/SaschaZeGerman/loginbuddy.git` 
	* Modify your hosts file, add **127.0.0.1 democlient.loginbuddy.net demoserver.loginbuddy.net local.loginbuddy.net**
	* For MacBooks this would be done at `sudo /etc/hosts`
* Run `mvn clean install`
	* This will compile all sources
* Run `docker-compose -f docker-compose-demosetup-dev.yml build --no-cache`
	* This will simply build the docker image (without a tag)
* Run `docker-compose -f docker-compose-demosetup-dev.yml up`
	* This will launch a container, configured for remote debugging and JMX support
	* This will also create a private key within the container, on the fly, used for testing and development purposes. See 'docker-build/add-ons/demosetup/loginbuddy.sh' for details
	* This will use ports 80 (http), 443 (https), 8000 (remote debugging), 9010 (jmx)
* Open a browser
	* Go to https://democlient.loginbuddy.net
	* The screen displays some info, click *Demo Client* (**NOTE: you will have to confirm SSL violations 3 times since 3 systems are simulated!**)
	* The next screen displays parameters that client would usually send. Simply click *Submit*
	* The following screen will display an image saying *FAKE* which is the demo provider. Just click it ...
		* The demo takes you through the (simulated) typical authentication/ authorization flow
	* On the *EMail* address screen type an email address
	* On the *Password* screen type any sentence
	* Confirm the consent screen
	* The response at the end is completely fake but it represents the type of message structure that can be expected by your client

That's it! In a real life scenario the *FAKE* image would be replaced by images of real providers!

Tip: if you build docker images often, run this command from time to time to remove dangling images: `docker rmi $(docker images -f "dangling=true" -q)`




## Add your own OpenID Provider within minutes

It's really simple to add a new OpenID Provider to Loginbuddy. 

1. Sign up to the OpenID Provider you want to add (For example Google).
	* Register an OAuth application using these details:
redirect_uri: https://local.loginbuddy.net/callback
	* Note these values that (google) generates:
		* client_id
		* client_secret
2. Clone this project:  `git clone https://github.com/SaschaZeGerman/loginbuddy.git` 
3.  Configure the Loginbuddy with credeentials from OpenID Provider 
4.  Build the Loginbuddy containers 

For more details see [WIKI](https://github.com/SaschaZeGerman/loginbuddy/wiki/Quick-Start)



## Digging deeper 

### Configuration

Loginbuddy requires four items to be configured:

- **OpenID Providers**: Providers that you want Loginbuddy to support
- **Clients**: Clients of Loginbuddy (that would be your web application or single-page app (SPA))
- **Permissions**: Endpoints you want Loginbuddy to connect to (this would be endpoints of supported providers) have to be registered (exception: dynamically registered providers)
- **OpenID Connect Discovery**: Loginbuddy itself provides a `/.well-known/openid-configuration` endpoint and needs it for its own configuration

See more details on how to configure the LoginBuddy on [WIKI](https://github.com/SaschaZeGerman/loginbuddy/wiki/Configuration)


### Deployments 

The small footprint allows for flexible deployment options. See pro and cons of different options on [WIKI](https://github.com/SaschaZeGerman/loginbuddy/wiki/Deployment)


### API and Protocols 

Loginbuddy is built to support OpenID Connect and OAuth 2.0 specifications:

* OAuth 2.0
* OpenID Connect Core
* OpenID Connect Discovery
* OpenID Connect Dynamic Registration
* OAuth 2.0 Pushed Authorization Requests

For more details on the APIs supported see [WIKI](https://github.com/SaschaZeGerman/loginbuddy/wiki/Protocols-and-APIs)



### Other Resources

The latest docker image is always available at [docker hub](https://hub.docker.com/r/saschazegerman/loginbuddy/).

To get a better idea how it works I have published a few videos about Loginbuddy on youtube: [Loginbuddy playlist](https://www.youtube.com/playlist?list=PLcX_9uDXp_CR5vXTT8lxI94x7Esl8O78E)

# Acknowledgements

The development of Loginbuddy is greatly supported by Jetbrains [IntelliJ IDEA ![alt - IntelliJ IDEA](doc/intellij-logo.png)](https://www.jetbrains.com/?from=loginbuddy)

# License

Copyright (c) 2020. All rights reserved.

This software may be modified and distributed under the terms of the Apache License 2.0 license. See the [LICENSE](/LICENSE) file for details.

