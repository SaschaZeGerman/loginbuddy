<%--
  ~ Copyright (c) 2018. . All rights reserved.
  ~
  ~ This software may be modified and distributed under the terms of the Apache License 2.0 license.
  ~ See http://www.apache.org/licenses/LICENSE-2.0 for details.
  ~
  --%>
<%@ page import="net.loginbuddy.common.cache.LoginbuddyCache" %>
<%@ page import="net.loginbuddy.common.config.Constants" %>
<%@ page import="net.loginbuddy.service.config.loginbuddy.Providers" %>
<%@ page import="net.loginbuddy.service.util.SessionContext" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="net.loginbuddy.service.config.loginbuddy.LoginbuddyUtil" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%!

    private static Logger LOGGER = Logger.getGlobal();

    private String createProvidersTable(HttpServletRequest request) {

        if (request.getParameter("session") == null || request.getParameterValues("session").length > 1 || request
                .getParameter("session").equals(request.getSession().getId())) {
            LOGGER.warning("The current session is invalid or it has expired!");
            throw new IllegalStateException("The current session is invalid or it has expired!");
        }
        String sessionId = request.getParameter("session");

        SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.get(sessionId);
        List<Providers> providerConfigs = null;
        try {
            providerConfigs = LoginbuddyUtil.UTIL.getProviders(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()));
        } catch (Exception e) {
          e.printStackTrace();
            // should never occur, this would have been caught in Providers
            LOGGER.severe("The system has not been configured yet!");
            throw new IllegalStateException("The system has not been configured yet!");
        }

        // Create a simple table that contains 3 provider images per row
        int count = 0;
        StringBuilder providers = new StringBuilder();
        providers.append("<table class=\"table table-condensed\">");
        for (Providers nextProvider : providerConfigs) {
            if (count % 3 == 0) {
                providers.append("<tr><td style=\"text-align: center; vertical-align: middle;\">");
            } else {
                providers.append("<td style=\"text-align: center; vertical-align: middle;\">");
            }
            providers.append("<form action=\"initialize\" enctype=\"application/x-www-form-urlencoded\" method=\"post\">");
            providers.append("<input type=\"hidden\" name=\"session\" value=\"").append(sessionId).append("\">");
            providers.append("<input type=\"hidden\" name=\"provider\" value=\"").append(nextProvider.getProvider()).append("\">");
            providers.append("<input type=\"hidden\" name=\"provider_addition\" value=\"\"/>");
            providers.append("<button type=\"submit\">");
            providers.append(String.format("<img alt=\"%s\" title=\"%s\" margin=\"0\" src=\"images/%s.png\"/></button></form></td>", nextProvider.getProvider(), nextProvider.getProvider(), nextProvider.getProvider()));
            if (count % 3 == 2) {
                providers.append("</tr>");
                count = 0;
            } else {
                count++;
            }
        }
        if (count % 3 != 2) {
            providers.append("</tr>");
        }
        providers.append("</table>");

        if (sessionCtx.getBoolean(Constants.CLIENT_ACCEPT_DYNAMIC_PROVIDER.getKey())) {
            providers.append("<hr/><h3>Use your own!<br/><small>Support for <a href=\"https://openid.net/specs/openid-connect-registration-1_0.html\" target=\"_blank\">OpenID Connect Dynamic Registration</a></small></h3>");
            providers
                    .append("<p>Provide the OpenID Connect <strong>issuer</strong> below to use a non listed but OpenID Connect compliant provider.</p>");
            providers.append("<p>The retrieved OpenID Provider configuration needs to include at least these values: <strong>issuer</strong> and <strong>registration_endpoint</strong></p>");
            providers.append("<p><strong>Note: </strong>The current implementation uses <strong>response_type=code, grant_type=authorization_code, token_endpoint_auth_method=client_secret_post</strong></p>");
            providers
                    .append("<form action=\"initialize\" enctype=\"application/x-www-form-urlencoded\" method=\"post\">");
            providers.append("<input type=\"hidden\" name=\"session\" value=\"").append(sessionId).append("\">");
            providers.append("<input type=\"hidden\" name=\"provider\" value=\"dynamic_provider\"/>");
            providers.append("<input type=\"hidden\" name=\"provider_addition\" value=\"\"/>");
            providers.append("<div class=\"form-group\"><label for=\"issuer\">Issuer: <small>(i.e.: https://myserver.com)</small></label><input type=\"url\" required maxlength=\"128\" class=\"form-control\" name=\"issuer\" id=\"issuer\"></div>");
            providers.append("<p>Add the OpenID Connect discovery endpoint if <strong>{issuer}.concat(/.well-known/openid-configuration)</strong> cannot be used to create the URL.</p>");
            providers.append("<div class=\"form-group\"><label for=\"discovery_url\">Discovery endpoint: <small>(i.e.: https://myserver.com/.well-known/openid-configuration)</small></label><input type=\"url\" maxlength=\"128\" class=\"form-control\" name=\"discovery_url\" id=\"discovery_url\"></div>");
            providers.append("<button type=\"submit\" class=\"btn btn-primary\">Submit</button>");
            providers.append("</form>");
        }

        return providers.toString();
    }
%>

<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>Loginbuddy - Providers</title>

    <!-- Bootstrap -->
    <link href="../css/bootstrap.min.css" rel="stylesheet">

    <link href="../favicon.ico" rel="icon" type="image/x-icon"/>
    <link href="../favicon.ico" rel="shortcut icon" type="image/x-icon"/>

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="../js/bootstrap.min.js"></script>
</head>
<body>

<div class="container" id="content">

    <h1>Welcome to Loginbuddy!<br/><small>Provider Selection Page</small></h1>
    <hr/>
    <h3>What do I see here?</h3>
    <p>In a real world scenario your application would host this page, making it look nicer. If not, Loginbuddy will generate this page, displaying all social providers that have been configured.</p>
    <p><strong>Note: </strong>This page only appears if no pre-selection of a provider has been made (via parameter 'provider').</p>
    <h3>Choose your provider</h3>
    <p>The images below represent configured and supported providers one can choose from.</p>
    <p><strong>FAKE</strong> is simulating a 'real' provider. Clicking it will result in an example response how it would look.</p>
    <p><strong>FAKE, DynamicRegistrationDemo</strong> is the same as FAKE but Loginbuddy registered itself at the provider using <strong>OpenID Dynamic Registration</strong>.</p>

    <%=
    createProvidersTable(request)
    %>

</div>
</body>
</html>