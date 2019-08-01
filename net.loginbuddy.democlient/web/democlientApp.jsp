<%@ page import="java.util.UUID" %>
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

    <h1>Welcome to Loginbuddy-Democlient!</h1>
    <p>This is a demo client of the opensource project <a href="https://github.com/SaschaZeGerman/loginbuddy" target="_blank"><strong>Loginbuddy</strong></a>.
        It is meant for demo purposes only! This client is not collecting data or remembers user interactions or tries to sell ads!</p>
    <hr/>
    <p>If you are developing an application and if you want to support social login, loginbuddy can help achieving that.
        Once you have registered your application, your application only needs to communicate with loginbuddy whereas loginbuddy communicates with configured social platforms.</p>
    <p>Below is an example form, give it a try. It simulates an authorization request of your application (no need to modify anything ... and also not suggested):</p>
    <form action="initialize" method="post" enctype="application/x-www-form-urlencoded">
        <div class="form-group">
            <label for="client_id">Client_ID *</label>
            <input type="text" id="client_id" name="client_id" size="80" readonly class="form-control" value="clientIdForTestingPurposes">
        </div>
        <div class="form-group">
            <label for="response_type">Response_Type *</label>
            <input type="text" id="response_type" name="response_type" size="80" readonly class="form-control" value="code">
        </div>
        <div class="form-group">
            <label for="redirect_uri">Redirect_URI</label>
            <input type="text" id="redirect_uri" name="redirect_uri" size="80" readonly class="form-control" value="https://<%=System.getenv("HOSTNAME_LOGINBUDDY_DEMOCLIENT")%>/callback">
        </div>
        <div class="form-group">
            <label for="state">Nonce</label>
            <input type="text" id="nonce" name="nonce" size="80" class="form-control" readonly value="<%=UUID.randomUUID().toString()%>">
        </div>
        <div class="form-group">
            <label for="state">State</label>
            <input type="text" id="state" name="state" size="80" class="form-control" readonly value="<%=UUID.randomUUID().toString()%>">
        </div>
        <div class="form-group">
            <label for="state">Scope</label>
            <input type="text" id="scope" name="scope" size="80" class="form-control" readonly value="openid email profile">
        </div>
        <div class="form-group">
            <label for="provider">Provider (leave it blank first, then try 'server_loginbuddy')</label>
            <input type="text" id="provider" name="provider" class="form-control" size="80">
        </div>
        <button type="submit" class="btn btn-primary">Submit</button>
    </form>
    <hr/>
    <p>This is the description of above parameters:</p>
    <ol>
        <li><strong>client_id *: </strong>This MUST match the registered client_id.</li>
        <li><strong>response_type *: </strong>This MUST be set to <strong>code</strong>. In the future loginbuddy may support other response_types</li>
        <li><strong>redirect_uri: </strong>This is required for public clients and optional for confidential ones. However, if provided, it MUST match one of the registered redirect_uris</li>
        <li><strong>nonce: </strong>An opaque value that will be found in an id_token that was created at the social platform. It is also a claim in additional details created by loginbuddy</li>
        <li><strong>state: </strong>A recommended opaque value for loginbuddy, but required by the client to match sessions, keep them aligned</li>
        <li><strong>scope: </strong>This is required for public clients, optional for confidential ones. A space separated list of requested SCOPEs (permissions)</li>
        <li><strong>provider: </strong>If this value is included, loginbuddy will not display a provider list to the user. Loginbuddy will forward the
            authentication request to that platform. The value has to match one of the configured provider IDs, an error will be returned otherwise. For testing
            purposes, leave it blank and use <strong>server_loginbuddy</strong> afterwards if you like</li>
    </ol>

</div>
</body>
</html>