#!/bin/bash
#
# Pick up inputs from the command line to run in debug mode if requested
# this should be used for development only
#
# To debug tomcat run 'export DEBUG=jdpa'
#
JPDA=$(echo $DEBUG)

if [ "$JPDA" = "jpda" ]
then
    # import our own certificate as trusted cert for local development (including debugging)
    #
    $JAVA_HOME/bin/keytool -importcert -alias loginbuddy -file /usr/local/tomcat/ssl/loginbuddy.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt
fi

# run the original tomcat entrypoint script but with security manager
#
sh /usr/local/tomcat/bin/catalina.sh $JDPA run -security