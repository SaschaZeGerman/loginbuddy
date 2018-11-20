<%@ page import="java.util.Base64" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%--
  ~ Copyright (c) 2018. . All rights reserved.
  ~
  ~ This software may be modified and distributed under the terms of the Apache License 2.0 license.
  ~ See http://www.apache.org/licenses/LICENSE-2.0 for details.
  ~
  --%>

<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>Loginbuddy - Demo Client</title>

    <!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">

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

    <h1>Welcome to loginbuddy demo client!</h1>
    <p>Below are the values provided by the social platform which the user has chosen.</p>
    <hr/>
    <%
        StringBuilder htmlPage = new StringBuilder();

        if (request.getParameter("userinforesponse") != null) {
            String userInfoResponse = request.getParameter("userinforesponse");
            String idToken = request.getParameter("id_token");
            String state = request.getParameter("state");
            htmlPage.append("<p> UserInfo: ").append(new String(Base64.getDecoder().decode(userInfoResponse))).append("</p>");
            htmlPage.append("<p> ID Token: ").append(idToken).append("</p>");
            htmlPage.append("<p> State: ").append(state).append("</p>");
        }
    %>

    <%=htmlPage%>
    <hr/>
    <p><a href="exampleApp.jsp"><strong>Try it again!</strong></a></p>

</div>
</body>
</html>