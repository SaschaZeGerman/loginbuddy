# About

A container based solution that should help developers support social login for their applications in a simple way!

# Loginbuddy - Your authenticator

This project implements an OpenID Connect client that can be used as proxy between an application (your application) and 
an OpenID Provider. This is useful for cases where social login should be an option for users to login to your 
application. Your application needs to make two simple calls only and the response will be users details. Loginbuddy 
takes care of the complete OAuth/ OpenID Connect protocol.

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

## Building and using the development container of loginbuddy:

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

That's it! The response that is shown is completely fake but it represents the type of content that can be expected.

### Optional: overwrite test private key (ignore this for now)

If you would like to use your own private key for testing purposes, simply follow the instructions below:

Create a new, self-signed key. The referenced file **doc/openssl.conf** is a mini-configuration file to generate an 
subject alternative name. Doing that allows loginbuddy (local.loginbuddy.net, server.loginbuddy.net) to be client and 
server at the same time with just one private key/ public certificate:

```
$ openssl req -newkey rsa:2048 -nodes -keyout loginbuddy.key -x509 -days 365 -out loginbuddy.crt \
      -subj "/CN=local.loginbuddy.net" \
      -config doc/openssl.config \
      -extensions SAN
```

Next, generate the p12 file that will be used with tomcat. When prompted for a password use 'changeit'. If you would like to 
use a unique password, search and replace all occurrences of 'changeit' in this project:

```
$ openssl pkcs12 -export \
      -name "loginbuddy" \
      -inkey loginbuddy.key \
      -in loginbuddy.crt \
      -out loginbuddy.p12
```

Check the content of the generated p12 file:

```$ openssl pkcs12 -info -in loginbuddy.p12```

Now, move the files **loginbuddy.crt** and **loginbuddy.p12** to a new location, overwriting the existing files. The 
file 'loginbuddy.key' can be removed:

```
$ rm loginbuddy.key
$ mv loginbuddy.crt loginbuddy.p12 docker-build/add-ons/local
```

With that, **Rebuilt loginbuddy as shown above** and give it a try!

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

# Current state

Loginbuddy does not support all described features yet. Here is a high level (short) list of things that need to be done first:

- implement provider id_token validation
- return base64 encoded content of id_token payload
- create JWT by loginbuddy to return content back to application
- use SSL only. Redirect http traffic to https
- logging
- other stuff ...

# License

Copyright (c) 2018 CA. All rights reserved.

This software may be modified and distributed under the terms of the Apache License 2.0 license. See the [LICENSE](/LICENSE) file for details.