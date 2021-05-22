package net.loginbuddy.example.sidecar;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class demonstrates how logindbuddy-sidecar could be used.
 * Assuming this class would be part of your application, only the two listed methods are needed:
 *
 * doPost: your user clicked a button such as 'login'
 *
 * After a user of your application has selected a button such as 'Login with google' call this method to initiate the authorization request to google.
 * If your users cannot choose between different providers you can also hard-code the value of 'provider' to an appropriate value.
 * The goal is to send a request to loginbuddy-sidecar (via http or https) which will return the authorizationUrl that is needed for the chosen provider.
 * The response will be status=201 and the Location header which contains the ready-to-use value for redirecting your user to the provider.
 *
 * doGet: the provider returns a response after the user has authenticated
 *
 * Whatever happens at the providers login page, the provider will return its response as a 302, redirect. When that response is received,
 * simply forward it as-is to loginbuddy-sidecar which handles success and error responses.
 * Loginbuddy will either return a success message which includes all details as documented in 'Configuration.md#Loginbuddys response - an example' in the WIKI,
 * or a failure consisting of error and error_description (JSON).
 *
 */
public class SideCarClient extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(SideCarClient.class));

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // for this purpose, catch all errors to display them in the browser
        try {

            String provider = request.getParameter("provider");  // this is the only required parameter for loginbuddy-sidecar and its value has to be found in 'config.json' as a valid provider
            String state = request.getParameter("state");  // this parameter is sent back to your client as-is

            List<NameValuePair> formParameters = new ArrayList<>();
            formParameters.add(new BasicNameValuePair("provider", provider));
            formParameters.add(new BasicNameValuePair("state", state));
//            formParameters.add(new BasicNameValuePair("scope", "if not included default scope will be used"));
//            formParameters.add(new BasicNameValuePair("nonce", "Loginbuddy will create its own value if skipped"));
//            formParameters.add(new BasicNameValuePair("prompt", "forwarded as is to the provider"));
//            formParameters.add(new BasicNameValuePair("login_hint", "forwarded as is to the provider"));
//            formParameters.add(new BasicNameValuePair("id_token_hint", "forwarded as is to the provider"));
//            formParameters.add(new BasicNameValuePair("signed_response_alg", "if set to 'RS256' your client receives a JWT (signed by loginbuddy-sidecar) as response instead of a JSON document"));
//            formParameters.add(new BasicNameValuePair("obfuscate_token", "the providers access_token and refresh_token will be obfuscated before returned to your application"));

//            HttpPost initAuthRequest = new HttpPost(String.format("https://loginbuddy-sidecar:444/sidecar/initialize"));
            HttpPost initAuthRequest = new HttpPost(String.format("http://loginbuddy-sidecar:8044/sidecar/initialize"));
            initAuthRequest.setEntity(new UrlEncodedFormEntity(formParameters));

            // initialize the authorization code flow. Loginbuddy will return the authorizationUrl that is valid for the selected provider
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse initAuthResponse = httpClient.execute(initAuthRequest);

            if (initAuthResponse.getStatusLine().getStatusCode() == 201) {
                // the location header contains the authorizationUrl taking a browser to the target provider the URL can be used as is
                response.sendRedirect(initAuthResponse.getHeaders("Location")[0].getValue());
            } else {
                // Error!
                // status=400 // or other than 201
                // Location=http://localhost/?error={error}&error_description={errorDescription}
                // do something useful
                LOGGER.warning(initAuthRequest.getHeaders("Location")[0].getValue());
                response.sendError(400, "Check the logs, please, something went wrong!");
            }
        } catch(Exception e) {
            LOGGER.warning(e.getMessage());
            response.sendError(400, e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // for this purpose, catch all errors to display them in the browser
        try {
//            HttpGet authResultRequest = new HttpGet(String.format("https://loginbuddy-sidecar:444/sidecar/callback?%s", request.getQueryString()));
            HttpGet authResultRequest = new HttpGet(String.format("http://loginbuddy-sidecar:8044/sidecar/callback?%s", request.getQueryString()));

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse authResultResponse = httpClient.execute(authResultRequest);

            String authResponseString = EntityUtils.toString(authResultResponse.getEntity());
            JSONObject jsonAuthResult = (JSONObject) new JSONParser().parse(authResponseString);

            if (authResultResponse.getStatusLine().getStatusCode() == 200) {
                // all good, user was successfully authenticated
                LOGGER.info(jsonAuthResult.toJSONString());  // this includes all details given by the provider, including the access_token and refresh_token
                response.setStatus(200);
                response.setContentType("application/json");
                response.getWriter().println(((JSONObject) jsonAuthResult.get("details_normalized")).toJSONString());
            } else {
                // something went wrong, or the user did not authenticate or denied access to his user details
                LOGGER.warning(jsonAuthResult.toJSONString());
                response.sendError(400, (String) jsonAuthResult.get("error_description"));
            }
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            response.sendError(400, e.getMessage());
        }
    }
}