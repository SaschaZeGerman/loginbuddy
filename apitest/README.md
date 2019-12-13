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

- **docker:** this contains a docker-compose file and configuration, used with the tests
- **scripts:** this was only used when adding support for *response_type=id_token*
- **soapui:** this contains SOAPUI project and properties files

To run tests follow these steps:

- launch the Loginbuddy test setup
  - ```$ cd ./docker```
  - ```$ docker-compose -f docker-compose-test.yml```
- Launch SOAPUI
- Import the project files found at *./soapui/project*
- Double-click each project, select *TestSuites* and hit the green arrow
- Verify that everything is green
- You can also open the browser at *https://democlient.loginbuddy.net* for manual verifications. Not everything is automated yet!
- Done!

## Configure SOAPUI

Both SOAPUI projects are using properties instead of hard coded values. These can be found here:

```$ ./properties/template.properties```

If you want to use your own properties, simply copy that file and load them info SOAPUI for each project. 

## Known issues

- The SOAPUI project *Loginbuddy-Flows* has many duplicated test steps. This requires some effort to keep them in sync. When I get to it, I will do some refactoring
- Not everything is automated, but running the test indicates a high chance that most features are working as expected
- Manual verifications are required via the browser UI 

## Tips and tricks for using SOAPUI

Here are a few notes for using SOAPUI.

### Extract header from previous response and stick into variable

    //Find the 'Location' header of the response
    def location = testRunner.testCase.testSteps["test-step-name"].testRequest.response.responseHeaders["Location"][0]
    
    // Write the 'location' into the 'endpoint' of the testStep 'test-step-target'
    def groovyUtils = new com.eviware.soapui.support.GroovyUtils( context )
    groovyUtils.setPropertyValue("test-step-target", "Endpoint", location.toString())
    
### Extract header from previous response and assert value

    //Find the 'Location' header of the response
    def location = testRunner.testCase.testSteps["test-step-name"].testRequest.response.responseHeaders["Location"][0]
    assert location.startsWith('value-to-assert');

### Extract header from current response

    // Find the 'Location' header of the response
    assert messageExchange.responseHeaders["Location"] != null

    def location = messageExchange.responseHeaders["Location"][0]

    // Check if all expected parameters are included in the redirect_uri
    assert(location.contains("client_id"));

### Access message body of current test step

    def bodyAsString = new String().valueOf(messageExchange.responseContent)