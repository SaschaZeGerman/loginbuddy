FROM saschazegerman/loginbuddy-base:latest

# add our own application as the one and only
#
# Default loginbuddy services
#
COPY net.loginbuddy.service/target/service-1.0.0 /usr/local/tomcat/webapps/ROOT

COPY docker-build/add-ons/server/conf/server.xml /usr/local/tomcat/conf/server.xml
COPY docker-build/add-ons/server/loginbuddy.sh /opt/docker/loginbuddy.sh