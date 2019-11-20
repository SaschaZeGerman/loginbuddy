<%@ page import="net.loginbuddy.common.util.Sanetizer" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
  ~ Copyright (c) 2018. . All rights reserved.
  ~
  ~ This software may be modified and distributed under the terms of the Apache License 2.0 license.
  ~ See http://www.apache.org/licenses/LICENSE-2.0 for details.
  ~
  --%>

<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>Loginbuddy</title>

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
    <h2>An error occured</h2>
    <%
        // Analyze the servlet exception
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String errorMsg = (String) request.getAttribute("javax.servlet.error.message");
        String servletName = (String) request.getAttribute("javax.servlet.error.servlet_name");
        if (servletName == null) {
            servletName = "Unknown";
        }
        String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri"); // this is the URL path
        if (requestUri == null) {
            requestUri = "Unknown";
        }
        if (statusCode != 500) {
    %>
    <%="<h3>Error Details</h3>"%>
    <%="<strong>Requested URI:</strong> " + Sanetizer.checkForUrlPathPattern(requestUri, 256) + "<br>"%>
    <%="<strong>Error Message:</strong> " + errorMsg + "<br>"%>
    <%
        } else {
            response.getWriter().write("<h3>Exception Details</h3>");
            response.getWriter().write("<ul><li>Servlet Name:" + servletName + "</li>");
            response.getWriter().write("<li>Exception Name:" + throwable.getClass().getName() + "</li>");
            response.getWriter().write("<li>Requested URI:" + Sanetizer.checkForUrlPattern(requestUri, 256) + "</li>");
            response.getWriter().write("<li>Exception Message:" + throwable.getMessage() + "</li>");
            response.getWriter().write("</ul>");
        }

    %>
</div>

</body>
</html>