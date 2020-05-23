<%@ page import="net.loginbuddy.common.cache.LoginbuddyCache" %>
<%@ page import="net.loginbuddy.common.net.loginbuddy.service.config.Constants" %>
<%@ page import="net.loginbuddy.common.util.MsgResponse" %>
<%@ page import="org.json.simple.JSONObject" %>
<%@ page import="org.json.simple.parser.JSONParser" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>Loginbuddy - Demo Client</title>

    <!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">

    <link href="favicon.ico" rel="icon" type="image/x-icon"/>
    <link href="favicon.ico" rel="shortcut icon" type="image/x-icon"/>
    <link href="css/prism.css" rel="stylesheet"/>

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
    <script src="js/prism.js"></script>
</head>
<body onload="Prism.highlightAll(false, null);">

<div class="container" id="content">

    <h1>Welcome back to Loginbuddy-Democlient!</h1>
    <hr/>
    <h2>Provider response</h2>
    <p>Below are the values returned by the social platform which the user has chosen. Since the window is pretty small (for now), copy the content and paste it into a JSON capable editor. Or at jsonlint.com.</p>
    <hr/>
    <%
        String result = "{\"error\":\"session_expired\"}";

        Map<String, Object> sessionValues = (Map<String, Object>) LoginbuddyCache.CACHE
                .remove(request.getParameter("state"));

        String error = request.getParameter(Constants.ERROR.getKey());
        String error_description = request.getParameter(Constants.ERROR_DESCRIPTION.getKey());

        if (error != null) {
            result = String.format("{\"error\":\"%s\", \"error_description\":\"%s\"}", error, error_description);
        } else if (sessionValues != null) {
            MsgResponse msgResp = (MsgResponse) sessionValues.get("msgResponse");
            if(msgResp.getContentType().startsWith("application/json")) {
                result = ((JSONObject) new JSONParser().parse(msgResp.getMsg())).toJSONString();
            } else {
                result = msgResp.getMsg(); // jwt
            }
        }
    %>
    <div>
        <pre><code class="language-json" id="idProviderResponse"><%=result%></code></pre>
    </div>
    <p>In addition to values of the social platform, Loginbuddy has added the following:</p>
    <ol>
        <li><strong>details_provider: </strong>the validated id_token payload, the chosen provider, the userinfo response</li>
        <li><strong>details_loginbuddy: </strong>values of the Loginbuddy session</li>
        <li><strong>details_normalized: </strong>a normalized version of the userinfo response. Your application could use that and would not have to worry about potential differences</li>
    </ol>
    <hr/>
    <p>Try again! <a href="democlientApp.jsp"><strong>Web Application!</strong></a>, <a href="spa.html"><strong>SPA!</strong></a></p>

</div>
</body>
</html>