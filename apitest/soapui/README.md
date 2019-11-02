# Tips and tricks for using SOAPUI

## Scripts assertions

### Extract header and stick into variable

    //Find the 'Location' header of the response
    def location = testRunner.testCase.testSteps["test-step-name"].testRequest.response.responseHeaders["Location"][0]
    
    // Write the 'location' into the 'endpoint' of the testStep 'test-step-target'
    def groovyUtils = new com.eviware.soapui.support.GroovyUtils( context )
    groovyUtils.setPropertyValue("test-step-target", "Endpoint", location.toString())
    
### Extract header and assert value

    //Find the 'Location' header of the response
    def location = testRunner.testCase.testSteps["test-step-name"].testRequest.response.responseHeaders["Location"][0]
    assert location.startsWith('value-to-assert');