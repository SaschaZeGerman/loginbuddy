<%@ page import="org.apache.http.client.HttpClient" %>
<%@ page import="org.apache.http.impl.client.HttpClientBuilder" %>
<%@ page import="org.apache.http.HttpResponse" %>
<%@ page import="org.apache.http.util.EntityUtils" %>
<%@ page import="org.apache.http.NameValuePair" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.apache.http.message.BasicNameValuePair" %>
<%@ page import="org.apache.http.client.methods.HttpPost" %>
<%@ page import="org.apache.http.client.entity.UrlEncodedFormEntity" %>
<%@ page import="net.loginbuddy.common.config.Constants" %>
<%@ page import="net.loginbuddy.common.util.MsgResponse" %>
<%@ page import="net.loginbuddy.common.cache.LoginbuddyCache" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%--
  ~ Copyright (c) 2018. . All rights reserved.
  ~
  ~ This software may be modified and distributed under the terms of the Apache License 2.0 license.
  ~ See http://www.apache.org/licenses/LICENSE-2.0 for details.
  ~
  --%>

<%--<%!--%>
<%--    private String displayResult(String code, String state, String error, String error_description) {--%>

<%--        // usually 'state' needs to be used to look up the session ... .--%>

<%--        StringBuilder resp = new StringBuilder();--%>

<%--        if(error != null) {--%>
<%--            resp.append("<h2>An error occured</h2><p><strong>Error: </strong>").append(error).append(", <strong>error_description: </strong>");--%>
<%--            if(error_description != null) {--%>
<%--                resp.append(error_description);--%>
<%--            } else {--%>
<%--                resp.append("Unfortunately, no details are available");--%>
<%--            }--%>
<%--            resp.append("</p>");--%>
<%--        } else if(code != null) {--%>
<%--            // build POST request--%>
<%--            List<NameValuePair> formParameters = new ArrayList<NameValuePair>();--%>
<%--            formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), code));--%>
<%--            try {--%>
<%--                HttpPost req = new HttpPost("https://"+System.getenv("HOSTNAME_LOGINBUDDY")+"/exchange"); // calling itself--%>

<%--                HttpClient httpClient = HttpClientBuilder.create().build();--%>
<%--                req.setEntity(new UrlEncodedFormEntity(formParameters));--%>

<%--                HttpResponse response = httpClient.execute(req);--%>
<%--                MsgResponse msgResp = new MsgResponse(response.getHeaders("Content-Type")[0].getValue(), EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());--%>
<%--                if (msgResp.getMsg() != null) {--%>
<%--                    resp.append(msgResp.getMsg()).append("</strong>");--%>
<%--                }--%>
<%--            } catch (Exception e) {--%>
<%--                e.printStackTrace();--%>
<%--                return null;--%>
<%--            }--%>
<%--        } else {--%>
<%--            resp.append("<h2>Nothing returned</h2>");--%>
<%--        }--%>
<%--        return resp.toString();--%>
<%--    }--%>

<%--%>--%>
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
</head>
<body>

<div class="container" id="content">

    <h1>Welcome to Loginbuddy!</h1>
    <p>This is a demo client of the opensource project <a href="https://github.com/SaschaZeGerman/loginbuddy" target="_blank"><strong>Loginbuddy</strong></a>.
        It is meant for demo purposes only! This client is not collecting data or remembers user interactions or tries to sell ads!</p>
    <hr/>
    <h2>Provider response</h2>
    <p>Below are the values returned by the social platform which the user has chosen.</p>
    <hr/>
    <%=LoginbuddyCache.getInstance().remove(request.getParameter("state"))%>
<%--    <%=displayResult(request.getParameter("code"), request.getParameter("state"), request.getParameter("error"), request.getParameter("error_description"))%>--%>
    <hr/>
    <p><a href="democlientApp.jsp"><strong>Try it again!</strong></a></p>

</div>
</body>
</html>