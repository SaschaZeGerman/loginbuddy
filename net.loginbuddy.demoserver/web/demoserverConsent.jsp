<%--
  ~ Copyright (c) 2018. . All rights reserved.
  ~                            
  ~ This software may be modified and distributed under the terms of the Apache License 2.0 license.
  ~ See http://www.apache.org/licenses/LICENSE-2.0 for details.
  ~
  --%>

<!--
~ Copyright (c) 2018. . All rights reserved.
~
~ This software may be modified and distributed under the terms of the Apache License 2.0 license.
~ See http://www.apache.org/licenses/LICENSE-2.0 for details.
~
-->
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>Loginbuddy - Consent</title>

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
    <p>This is a demo client of the opensource project <a href="https://github.com/SaschaZeGerman/loginbuddy"
                                                          target="_blank"><strong>Loginbuddy</strong></a>.
        It is meant for demo purposes only! This client is not collecting data or remembers user interactions or tries to sell ads!</p>
    <hr/>
    <h2>Fake-Provider</h2>
    <h3>Consent</h3>
    <p>Application <strong>Loginbuddy Test Client</strong> would like to access the following information:</p>
    <code>Your email-address, your profile and your ID (remember, this is completely fake for this demo!)</code>
    <form action="consent" enctype="application/x-www-form-urlencoded" method="post">
        <div class="form-group">
            <input type="hidden" name="session" value="<%=request.getParameter("session")%>">
            <input type="hidden" name="action" value="grant">
        </div>
        <button type="submit" class="btn btn-primary">Grant</button>
    </form>
    <form action="consent" enctype="application/x-www-form-urlencoded" method="post">
        <div class="form-group">
            <input type="hidden" name="session" value="<%=request.getParameter("session")%>">
            <input type="hidden" name="action" value="cancel">
        </div>
        <button type="submit" class="btn btn-primary">Cancel</button>
    </form>
</div>
</body>
</html>