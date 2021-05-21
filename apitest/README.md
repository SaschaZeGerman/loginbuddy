# Running API tests for Loginbuddy

Loginbuddy uses SOAPUI for testing APIs. I got stuck with SOAPUI for many years and, although the name may not indicate it, it is good at testing REST APIs, too.

SOAPUI (Open Source) can be downloaded [here](https://www.soapui.org/downloads/soapui.html).

To run the tests a few pre-requisites are required:

- Update the hosts file and add these entries (you may already have some of them configured)
  - *127.0.0.1 loginbuddy-sidecar loginbuddy-oidcdr local.loginbuddy.net democlient.loginbuddy.net demoserver.loginbuddy.net soapui.loginbuddy.net*
- Launch SOAPUI and update these preferences:
  - check *Preferences - HTTP Settings - Pre-Encoded Endpoints*

## Run tests

Next to this file there are three directories:

- **docker:** this contains docker related content to stand-up the testing environments
- **scripts:** this was only used when adding support for *response_type=id_token*
- **soapui:** contains SOAPUI projects and properties files

### Docker

**Directory: ./docker**

This directory contains the 'default' test setup and tests most bits and pieces. The test scenario consists of these components:
- `loginbuddy-oidcdr`: testing dynamic registration
- `loginbuddy-demo`: simulating the backend
- `loginbuddy-sidecar`: testing the sidecar setup
- `loginbuddy-test`: provides helper services

In addition, this test setup uses a custom Loginbuddy config loader.

To run the first set of tests do the following (assuming the pre-requisites are satisfied):
- `cd ./docker`
- `make run-test`  // it copies test files and lauches the docker containers
- Launch SOAPUI and import the projects `./soapui/project/loginbuddy-basic.xml`, `.../loginbuddy-configManagement.xml`
- run the test by double-clicking the imported project, select 'TestSuites' and click the green 'run button'

You should only see green diagrams!

When this is done, run `make stop-test` to stop the test environment!

**TIP**: Run `make run-test-hazelcast`/ `make stop-test-hazelcast` to use a setup that leverages Hazelcast!

To run the second set of tests do the following (assuming the pre-requisites are satisfied):
- `cd ./docker`  // unless you ar ealready in that directory
- `make run-test-flows`  // it copies test files and lauches the docker containers
- Launch SOAPUI and import the projects `.../loginbuddy-flows.xml`
- run the test by double-clicking the imported project, select 'TestSuites' and click the green 'run button'

You should only see green diagrams!

When this is done, run `make stop-test-flows` to stop the test environment!

**Directory: ./docker/sidecar**

This directory stands-up a test environment specifically for the sidecar deployment of Loginbuddy. It is used slightly different than how it would be used in a 
real life scenario but this is to create tests that 'look into' Loginbuddy more. The test scenario consists of these components:
- `loginbuddy-sidecar`: receives request from SOAPUI as its client
- `loginbuddy-oidcdr`: used to test dynamic registration when loginbuddy-sidecar leverages that feature
- `demoserver.loginbuddy.net`: simulates an OpenID provider

All containers are configured for remote debugging!

The differences to 'real life' setups are these:
- `loginbuddy-sidecar`: the compose file exposes all ports of this container. This is required to connect from outside the docker network (SOAPUI). Since loginbuddy-sidecar 
would usually be launched with a container that leverages it, that container would be part of the same network and therefore could access it via port 444 by default!
- `loginbuddy-oidcdr`: when this container launches it imports the SSL vertificate of demoserver.loginbuddy.net. This is required because self-signed certificates are 
not accepted by default and tests would fail. This modification is preferred than implementing 'http' instead of using 'https'!
- `demoserver.loginbuddy.net`: when creating its DN for the self-signed certificate, it also includes 'loginbuddy-demoserver' as SAN name. This helps with DNS naming issues 
that arise from SOAPUI being outside of the docker network and the other containers being part of it.

To run the tests do the following (assuming the pre-requisites are satisfied):
- `cd ./docker/sidecar`
- `docker-compose up`
- Launch SOAPUI and import the project `./soapui/project/Loginbuddy-Sidecar.xml`
- run the test by double-clicking the imported project, select 'TestSuites' and click the green 'run button'

You should only see green diagrams!

When this is done, run `docker-compose down` to stop the test environment!

### Custom Configuration Loader

This testsuite is using a custom loader for loading clients and providers. The custom loader has been implemented in the module net.loginbuddy.test.

The class implementing the loader is configured here:

- file: `./docker/loginbuddy.properties`
- property: `config.loginbuddy.loader.default`

## Configure SOAPUI

All SOAPUI projects are using properties instead of hard coded values. These can be found here:

```$ ./soapui/properties/template.properties```

If you want to use your own properties, simply copy that file and load them into SOAPUI for each project.

## Known issues

- The SOAPUI project *Loginbuddy-Flows* has many duplicated test steps. This requires some effort to keep them in sync. When I get to it, I will do some refactoring
- Not everything is automated, but running the test indicates a high chance that most features are working as expected
- Manual verifications are required via the browser UI