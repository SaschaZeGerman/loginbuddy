FROM tomcat:alpine

# We do not want to keep the default applications, remove them
#
RUN rm -rf /usr/local/tomcat/webapps/*

# add our own application as the one and only
#
COPY docker-build/add-ons/web /usr/local/tomcat/webapps/ROOT

# copy the entrypoint script to run tomcat ith security manager
#
COPY docker-build/add-ons/server/loginbuddy.sh /opt/docker/

# overwrite some configuration to 'harden' tomat. 'logindbuddy.xml' is required since we run tomcat with security manager
# without 'loginbuddy.xml' we would use a 'META-INF/context.xml' file
#
COPY docker-build/add-ons/server/loginbuddy.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml
COPY docker-build/add-ons/server/catalina.policy /usr/local/tomcat/conf/catalina.policy
COPY docker-build/add-ons/server/server.xml /usr/local/tomcat/conf/server.xml
COPY docker-build/add-ons/server/web.xml /usr/local/tomcat/conf/web.xml

# create non-priviliged user (with its own group) to run tomcat
#
RUN addgroup tomcat
RUN adduser -SG tomcat tomcat
RUN chown -R tomcat:tomcat /usr/local/tomcat/

# set permissions
#
RUN chmod 755 /opt/docker/loginbuddy.sh
RUN chown tomcat:tomcat /opt/docker/loginbuddy.sh