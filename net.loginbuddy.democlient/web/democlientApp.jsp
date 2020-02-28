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
    <script>
        function selectClientId() {
            var clientId = document.getElementById('client_id');
            if(document.getElementById('idResponseAsJwt').checked) {
                clientId.setAttribute('value', 'clientIdForTestingPurposesJwt');
            } else {
                clientId.setAttribute('value', 'clientIdForTestingPurposes');
            }
        }
        function obfuscateProviderToken() {
            var obfuscate = document.getElementById('obfuscate_token');
            if(document.getElementById('idObfuscateToken').checked) {
                obfuscate.setAttribute('value', 'true');
            } else {
                obfuscate.setAttribute('value', 'false');
            }
        }
    </script>
</head>
<body>

<div class="container" id="content">

    <h1>Welcome to Loginbuddy-Democlient!</h1>
    <p>This is a demo client of the opensource project <a href="https://github.com/SaschaZeGerman/loginbuddy" target="_blank"><strong>Loginbuddy</strong></a>.
        This client simulates an application that a developer would build.</p>
    <hr/>
    <p>Selecting <strong>Submit</strong> takes you to a page where you can select a social provider. In the end you will see, which information about you is available and how it looks like.</p>
    <form action="initialize" method="post" enctype="application/x-www-form-urlencoded">
        <div class="form-group">
            <label for="provider">Provider (type 'server_loginbuddy' to skip the next screen. Leave it blank the first time you try)</label>
            <input type="text" id="provider" name="provider" class="form-control" size="80">
        </div>
        <input type="hidden" id="client_id" name="client_id" size="80" readonly class="form-control" value="clientIdForTestingPurposes">
        <input type="hidden" id="obfuscate_token" name="obfuscate_token" size="5" class="form-control" readonly value="false">
        <input type="hidden" name="provider_addition" value="">
        <button type="submit" class="btn btn-primary">Submit</button>
        <br/>
        <input class="form-check-input" type="checkbox" value="" id="idResponseAsJwt" onclick="return selectClientId();">
        <label class="form-check-label" for="idResponseAsJwt"> receive final response as signed JWT</label>
        <br/>
        <input class="form-check-input" type="checkbox" value="" id="idObfuscateToken" onclick="return obfuscateProviderToken();">
        <label class="form-check-label" for="idObfuscateToken"> obfuscate identity providers access_token and refresh_token</label>
    </form>
</div>
</body>
</html>