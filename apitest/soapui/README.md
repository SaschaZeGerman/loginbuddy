# Tips and tricks for using SOAPUI

## Scripts assertions

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