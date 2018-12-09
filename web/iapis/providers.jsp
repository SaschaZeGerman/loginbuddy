<%@ page import="net.loginbuddy.config.LoginbuddyConfig" %>
<%@ page import="net.loginbuddy.config.ProviderConfig" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="net.loginbuddy.cache.LoginbuddyCache" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>


<%--
  ~ Copyright (c) 2018. . All rights reserved.
  ~
  ~ This software may be modified and distributed under the terms of the Apache License 2.0 license.
  ~ See http://www.apache.org/licenses/LICENSE-2.0 for details.
  ~
  --%>

<%!

    private static Logger LOGGER = Logger.getGlobal();

    private String createProvidersTable(HttpServletRequest request) {

        if(request.getParameter("session") == null || request.getParameterValues("session").length > 1 || request.getParameter("session").equals(request.getSession().getId())) {
            throw new IllegalStateException("The current session is invalid or it has expired!");
        }
        String session = request.getParameter("session");

        List<ProviderConfig> providerConfigs = null;
        try {
            providerConfigs = LoginbuddyConfig.getInstance().getConfigUtil().getProviders();
        } catch (Exception e) {
            // should never occur, this would have been caught in net.loginbuddy.oauth.server.Providers
            LOGGER.severe("The system has not been configured yet!");
            throw new IllegalStateException("The system has not been configured yet!");
        }

        // Create a simple table that contains 3 provider images per row
        int count = 0;
        StringBuilder providers = new StringBuilder();
        providers.append("<table class=\"table table-condensed\">");
        for (ProviderConfig nextProvider : providerConfigs) {
            if (count % 3 == 0) {
                providers.append("<tr><td style=\"text-align: center; vertical-align: middle;\">");
            } else {
                providers.append("<td style=\"text-align: center; vertical-align: middle;\">");
            }
            providers.append("<form action=\"initialize\" enctype=\"application/x-www-form-urlencoded\" method=\"post\">");
            providers.append("<input type=\"hidden\" name=\"session\" value=\"").append(session).append("\">");
            providers.append("<input type=\"hidden\" name=\"provider\" value=\"").append(nextProvider.getProvider()).append("\">");
            providers.append("<button type=\"submit\">");
            providers.append("<img width=\"100\" margin=\"0\" src=\"images/");
            providers.append(nextProvider.getProvider());
            providers.append(".png\"/></button></form></td>");
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

    <h1>Welcome to Loginbuddy!</h1>
    <p>This is a demo client of the opensource project <a href="https://github.com/SaschaZeGerman/loginbuddy"
                                                          target="_blank"><strong>Loginbuddy</strong></a>.
        It is meant for demo purposes only! This client is not collecting data or remembers user interactions or tries
        to sell ads!</p>
    <hr/>
    <h2>Choose your provider</h2>
    <p>The images below represent configured and supported providers one can choose from. <strong>fake</strong> is a
        place holder and does not do anything else
        than simulating a 'real' provider. Clicking it will result in an example response how it would look.</p>

    <%=
        createProvidersTable(request)
    %>

</div>
</body>
</html>