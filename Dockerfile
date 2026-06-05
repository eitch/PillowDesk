FROM tomcat:10-jdk25

RUN rm -rf /usr/local/tomcat/webapps/*

COPY pillow-desk-web/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
ENV CATALINA_OPTS="$CATALINA_OPTS --enable-native-access=ALL-UNNAMED"
CMD ["catalina.sh", "run"]
