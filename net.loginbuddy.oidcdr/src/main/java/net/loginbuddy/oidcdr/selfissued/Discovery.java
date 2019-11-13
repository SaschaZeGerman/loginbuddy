package net.loginbuddy.oidcdr.selfissued;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Discovery extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ParameterValidatorResult clientIdResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));

        response.setStatus(200);
        response.setContentType("application/json");
        response.getWriter().println(getDiscoveryResponse(clientIdResult.getValue()));
    }

    /**
     * The discovery response as specified at <a href="https://openid.net/specs/openid-connect-core-1_0.html#SelfIssuedDiscovery">OIDC Self-Issued Discovery</a>
     * @return
     */
    private String getDiscoveryResponse(String clientId) {
        return String.format("{\"authorization_endpoint\":\"openid://\",\n" +
                "\"issuer\":\"https://self-issued.me\",\n" +
                "\"jwks_uri\":\"\",\n" +
                "\"userinfo_endpoint\":\"\",\n" +
                "\"token_endpoint\":\"\",\n" +
                "\"scopes_supported\":[\"openid\", \"profile\", \"email\", \"address\", \"phone\"],\n" +
                "\"response_types_supported\":[\"id_token\"],\n" +
                "\"subject_types_supported\":[\"pairwise\"],\n" +
                "\"id_token_signing_alg_values_supported\":[\"RS256\"],\n" +
                "\"request_object_signing_alg_values_supported\":[\"none\", \"RS256\"],\n" +
                "\"registration_endpoint\": \"https://loginbuddy-oidcdr:445/selfissued/register?client_id=%s\"}", HttpHelper.urlEncode(clientId));
    }
}
