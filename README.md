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

## How it's implemented

Loginbuddy is a simple java web application running in tomcat. The Docker image is based on tomcat:alpine. The Dockerfile shows all details.

It uses no database, only an internal cache. It runs with a security manager (this makes life a little more complicated, but hey, 
it would be too easy otherwise)! All default tomcat applications are removed, only loginbuddy is installed.

When loginbuddy communicates with an OpenID Provider it always uses **response_type=code** with **PKCE (RFC 7636)**. It produces a 
random **nonce** and **state** value, it validates received **id_token (JWT)**. Loginbuddy always returns the received 
id_token but also the response of a call to **/userinfo** to your application. It does **NOT** expose or persist or cache received access_token or 
refresh_token. Once responses are returned to your application, loginbuddy remembers nothing ... .

Loginbuddy only communicates with OpenID Providers using **TLS/SSL**.

Loginbuddy itself can be configured easily to use a certificate signed by **Let's Encrypt**. This allows you to run loginbuddy 
in public with no TLS/SSL issues!

With that, it follows best practices for OAuth and OpenID Connect.
 
# Getting started

To build loginbuddy you need these tools:

- java jdk8
- maven
- docker
- docker-compose

## Building and using the development container of loginbuddy:

I have only used loginbuddy on a MacBook, if you run it on Windows, some instructions may need to be adjusted!

- Preparation
  - modify your hosts file, add **127.0.0.1 local.loginbuddy.net server.loginbuddy.net**
  - for MacBooks this would be done at ```sudo /etc/hosts```
- Run ```mvn package```
  - this will compile all sources and it will copy the content of **./web** to **./docker-build/web**
  - that web content is used when the docker image is built
- Run ```docker-compose -f docker-compose-dev.yml build --no-cache```
  - this will simply build the docker image (without a tag)
- Run ```docker-compose -f docker-compose-dev.yml up -d```
  - this will create a container, configured for remote debugging
  - this will also create a private key on the fly, used for testing and development purposes. See 'docker-build/add-ons/local/loginbuddy.sh' for details
  - the private key's subject is **CN=local.loginbuddy.net**. The subject alternative name is **server.loginbuddy.net** to simulate a provider
  - this will use ports 80 (http), 443 (https), 8000 (debugging)
- Open a browser
  - go to **http://local.loginbuddy.net** and follow the prompts
  - the following screen will display an image saying 'fake' which is the demo provider. Just click it ...
  - the demo takes your through the (simulated) typical authentication/ authorization flow
  - the response at the end is completely fake but it represents the type of content that can be expected 

That's it! In a real life scenario the 'fake' image would be replaced by images of real providers!

## Configuring 'real' OpenID Providers

Loginbuddy is able to communicate with any OpenID Provider. The only requirements are these:

- you need to register loginbuddy as an OAuth client at desired providers
- use **https://local.loginbuddy.net/callback** as redirect_uri (reply_url) to make your life simple, at least, during your first steps with loginbuddy

Once registered, you only need to add a JSON based configuration as shown below:

1. **loginbuddy**: this is the root key
2. **clients**: this is an array of your clients (applications) that may use loginbuddy. Each client has a 'client_uri' and a 'redirect_uri'
3. **providers**: an array of OpenID Providers (OP)

The document looks like this:
```
{
	"loginbuddy": {
		"clients": [{
			"client_uri": "...",
			"redirect_uri": "..."
		}],
		"providers": [{
			"provider": "provider",
			"issuer": "https://......",
			"client_id": "...",
			"client_secret": "...",
			"redirect_uri": "...",
			"response_type": "code",
			"scope": "...",
			"openid_configuration_uri": "https://..."
			"authorization_endpoint": "https://...",
			"token_endpoint": "https://...",
			"userinfo_endpoint": "https://...",
			"jwks_uri": "https://...",
		}]
	}
}
```
 
The required configuration is simpler if the target system supports [OpenID Connect Discovery](https://openid.net/specs/openid-connect-discovery-1_0.html)

The overview below displays the JSON configuration keys. Depending on the support for OpenID Connect Discovery some may not be required:

- X: required
- O: optional
- ---: Not applicable

| keys                     | w/ OpenID Discovery | w/o OpenID Discovery   | Note  |
| ------------------------ |:-------------------:| :--------------------: | --- |
| provider                 | X                   | X                      | Loginbuddy uses this value to identify the desired provider |
| issuer                   | X                   | X                      | This must be optained from the OP|
| client_id                | X                   | X                      | This must be obtained from the OP|
| client_secret            | X                   | X                      | This must be obtained from the OP. Loginbuddy is a 'confidential' client and presents the client_secret when exchanging the 'code' for a token |
| redirect_uri             | X                   | X                      | This must point to loginbuddy, e.g.: 'https://local.loginbuddy.net/callback'. The OP will send all responses to this API |
| openid_configuration_uri | X                   | ---                    | Loginbuddy uses this absolute URI instead of concatenating {issuer}/.well-known/openid-configuration | 
| response_type            | O                   | O                      | Loginbuddy always uses 'code'. Any configuration here is ignored and meant for future purposes only. For those reasons this is optional |
| scope                    | O                   | O                      | In most cases this will be 'openid profile email'. This is also the default if it is left empty |
| authorization_endpoint   | ---                 | X                      | An absolute URI of the authorization endpoint |
| token_endpoint           | ---                 | X                      | An absolute URI of the token endpoint |
| userinfo_endpoint        | ---                 | X                      | An absolute URI of the userinfo endpoint |
| jwks_uri                 | ---                 | X                      | An absolute URI of the jwks_uri for id_token (JWT) validations |

Once this configuration document has been created, place it at **web/config/config.json**. 

In addition a **.png** image has to be provided. Loginbuddy will always search for a provider image by concatenating two 
values: **{config.json.PROVIDER}.{png}**. Place the image here: **web/images**.

### Configuration - security manager

As I mentioned above, the security manager makes the life a little more difficult. Open this file:

- **docker-build/add-ons/server/catalina.policy**

Search for **[Loginbuddy]**. The section below that label requires additional entries if new providers are used. Simply add a 
SocketPermission as done already in that section. If you try it without, you will see how the container fails to connect.
**TIP:** Find out what the hostnames are when loginbuddy connects to **{config.json.TOKEN_ENDPOINT}, {config.json.USERINFO_ENDPOINT}, {config.json.JWKS_ENDPOINT}** 

With that, **Rebuilt loginbuddy as shown above** and give it a try!

### Debugging

If you are using IntelliJ, create a Tomcat Debug configuration like this:

```
Run Configurations .... Tomcat Remote ... Debug:
-agentlib:jdwp=transport=dt_socket,address=8000,suspend=n,server=y
```

Here are the images:

[IntelliJ - Debug Configuration - 01:](doc/intellij_debug_config.png)

[IntelliJ - Debug Configuration - 02:](doc/intellij_debug_config_2.png)

After you have launched the container, click the **Debug** button in IntelliJ's UI. You will now be connected and are able to 
debug any code found in **src/main/***.

## Building clients (applications) that connect to Loginbuddy

**IMPORTANT:** Use the example browser client at **http://local.loginbuddy.net** to try out your configurations with your desired providers. 
This gives you and idea how loginbuddy works without having to write a single line of code! Once you have done that, get going, it is easy!

Loginbuddy is an API based solution which means no special libraries are required.

Loginbuddy provides exactly one API to initialize the flow. This must be called using a browser, the response is HTML:

**Request**

- URL: /provider.jsp
- Method: POST/ GET
- Content-Type: application-x-www-form-urlencoded
- Parameters:
  * *redirect_uri*: this must match one of the values configured in **config.json[loginbuddy.clients]**
  * *state*: a state value, opaque to loginbuddy but used for session handling
  * *provider* (optional): if provided, it must match one of the values configured at **config.json[loginbuddy.providers]**. If 
  this value is provided loginbuddy will skip the **provider-selection-screen**. Otherwise an HTML page will be presented to the resource_owner.

**OAuth dance between loginbuddy, resource_owner and OpenID Provider**

- there is nothing your application has to do. Just sit and wait

**Response** to your redirect_uri of your application, after the login process has finished

- *userinforesponse*: The base64 encoded response the provider responded as a request to **/userinfo** 
- *id_token*: the id_token (jwt) as returned by the provider
- *state*: the value as given by your application

That's all!

## Using TLS/SSL certificates signed by Let's Encrypt

To use loginbuddy as part of a real system, it needs to have a valid SSL certificate. Achieving this is quite simple. 
[Let's Encrypt](https://letsencrypt.org) is a Certificate Authority (CA) that issues certificates for free! The signed certificates are valid for 3 months and can 
be renewed as often as needed.

However, you need to satisfy these requirements:

- you need to own a domain name that you are in control of
  - meaning, you need to register the desired hostname
- you need a platform that supports docker and is accessible on the internet

### Digitalocean

This is not an advertisement for [digitalocean.com](https://www.digitalocean.com), but I have tried the described approach below 
on that platform and it worked very well. You can do the following:

- register yourself at digitalocean and create a so-called *droplet*
- in the options to choose from, select *one-click apps*
  - here you will find the option *Docker \<version\>*. This is a docker enabled Ubuntu droplet
  - choose the cheapest one, USD $5,--/ month
- once that is launched, follow digitalocean's instructions on creating valid users and enabling ssh
- when this is done, the menu *Networking* on the left side of the screen allows you to manage domain names and associate them with your droplet
  - to make this work, you have to update the DNS registration at your domains registrar
  - at the registrar configure it so that digitalocean manages the DNS names

Now, if you got this working, which took you probably a few hours for the first time (not because it is complicated, but because it is ... well ... the first time)
 you can login to your droplet via ssh and install *certbot-auto*. That is the client that will provide the signed certificate. The good news is 
 that digialocean has a very good API that let's you automate practically all required steps later on if you like!
 
The main idea is to install a signed cert in your droplet and map loginbuddy to it via a volume in docker-compose. Here are the steps:

On the droplet console:

**Create directories:**

```
$ mkdir /opt/certs
$ mkdir /opt/certbot
$ mkdir /opt/loginbuddy
```
### Let's Encrypt

**Install certbot-auto:** 
 
```
$ curl -o /opt/certbot/certbot-auto https://dl.eff.org/certbot-auto
$ chmod a+x /opt/certbot/certbot-auto
```

**Create your private key and a CSR:**

```
$ cd /opt/certs
$ openssl req -out server.csr -new -newkey rsa:2048 -nodes -keyout server.key -subj "/CN=your-hostname" // You can certainly include C... ST... L... O... in your subject
```

**Verify that port 80 is open:**

Certbot will start a standalone web server for verification purposes and will place a file on your system. It will then 
try to retrieve that file via an http request.
  
```
$ ufw status  // port 80 is most likey not listed
$ ufw allow 80/tcp // opens port 80
```

**Run certbot-auto:**

The command below *simulates* the process. Use this option '--staging' while you are testing with certbot. Otherwise 
you will run into ratelimit issues and BOOOOOMMM you are banned for some time!

The command below executes certbot in a non-interactive mode. The signed certificate will be placed at */opt/certs/signed.pem*. 

```
$ cd /opt/certbot
$ ./certbot-auto certonly --csr /opt/certs/server.csr --standalone --staging --email your@email.com --non-interactive --agree-tos --cert-path /opt/certs/signed.pem
``` 

The first time cerbot-auto runs it will download some libraries. Nevertheless, at the end you should see a message like this:

*Congratulations! Your certificate and chain have been saved ...*

If you run ```ls -l``` you should find files called **0000_chain.pem 0001_chain.pem**. The first file is the CA, in this 
case *CN = Fake LE Root X1*, the second one is your signed cert in combination with the CA cert!

In addition, **/opt/certs/signed.pem** was created which is your signed cert.

**Run certbot-auto - the real thing:**

Remove the test-certificates and get your real signed certificate!

```
$ cd /opt/certs
$ rm signed.pem

$ cd /opt/certbot
$ remove 000*

# the switch --staging has been removed!
$ ./certbot-auto certonly --csr /opt/certs/server.csr --standalone --email your@email.com --non-interactive --agree-tos --cert-path /opt/certs/signed.pem
```

*Congratulations! Your certificate and chain have been saved ...*

This time Let's Encrypt has signed your certificate using the 'real' CA! Check it by running this command, look for *Issuer* within the print out:

```
$ openssl x509 -in /opt/certs/signed.pem -text -noout
```

Now, lets update the private key and create a p12 file which will be used by tomcat (You have to provide a password! For 
testing, just use 'changeit'. In all other cases use a unique password!).

To make this work we can either use **0001_chain.pem** or combine **signed.pem** and **0001_chain.pem**:

```
$ cd /opt/certs
$ cat signed.pem /opt/certbot/0000_chain.pem > loginbuddy_signed.pem // creating the certificate chain
$ openssl pkcs12 -export -in loginbuddy_signed.pem -inkey server.key -name "loginbuddy" -out loginbuddy.p12
$ openssl pkcs12 -info -in loginbuddy.p12 // view the content of the p12 file
``` 

Very good! You have now got everything you need to get your first loginbuddy container running, including a 'real' SSL certificate!

### Launching loginbuddy

To make your life easy, copy a few example files onto your droplet.

Copy the local file **docker-build/add-ons/server/catalina.policy** to your droplet into the directory **/opt/loginbuddy/catalina.policy**. 
Modify the copy to end with this line:

```
  permission ...
  permission java.net.SocketPermission "your-hostname", "connect,resolve";
};
```

Copy the local file **web/config/config.json** to your droplet into the directory **/opt/loginbuddy/config.json**. Update 
all occurences of *local.logindbuddy.net* and *server.loginbuddy.net* with your hostname!

Copy the provided example file **docker-compose.yml** and place it on your droplet at **/opt/loginbuddy/docker-compose.yml**.

Uncomment all lines as of *version: '3.4'* and update the hostname!

Now open port 443:

```
$ ufw allow 443/tcp
$ ufw status // it should appear in the list of open ports
```

Finally, it is time to give it a try!

```
$ cd /opt/loginbuddy
$ docker-compose up -d
```

**NOTE:** It may take a few minutes for loginbuddy to start up! Be patient, we have chosen the cheapest droplet!

Open a browser and open loginbuddy at **http://your-hostname**.

# Current state

Please check the project page: [projects](https://github.com/SaschaZeGerman/loginbuddy/projects).

# License

Copyright (c) 2018 CA. All rights reserved.

This software may be modified and distributed under the terms of the Apache License 2.0 license. See the [LICENSE](/LICENSE) file for details.