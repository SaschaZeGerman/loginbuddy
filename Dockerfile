FROM tomcat:alpine

# We do not want to keep the default applications, remove them
#
RUN rm -rf /usr/local/tomcat/webapps/*

# add our own application as the one and only
#
# Default loginbuddy services
#
COPY net.loginbuddy.service/target/service-1.0.0 /usr/local/tomcat/webapps/ROOT

COPY docker-build/add-ons/server/catalina.policy /usr/local/tomcat/conf/catalina.policy
COPY docker-build/add-ons/server/server.xml /usr/local/tomcat/conf/server.xml
COPY docker-build/add-ons/server/loginbuddy.sh /opt/docker/loginbuddy.sh
COPY docker-build/add-ons/server/loginbuddy_oidcdr_import.sh /opt/docker/loginbuddy_oidcdr_import.sh

# Create directory for holding SSL keys
#
RUN mkdir /usr/local/tomcat/ssl

# create non-priviliged user (with its own group) to run tomcat
#
RUN addgroup tomcat
RUN adduser -SG tomcat tomcat
RUN chown -R tomcat:tomcat /usr/local/tomcat/

# Run the entrypoint script to run tomcat with security manager
#
CMD ["/opt/docker/loginbuddy.sh"]