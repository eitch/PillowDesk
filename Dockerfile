FROM tomcat:10-jdk25

RUN rm -rf /usr/local/tomcat/webapps/*

# Create a non-root user
ARG UID=1000
ARG GID=1000
RUN if ! getent group $GID >/dev/null; then groupadd -g $GID pillowuser; fi && \
    if ! getent passwd $UID >/dev/null; then useradd -u $UID -m -g $GID pillowuser; else usermod -g $GID $(getent passwd $UID | cut -d: -f1); fi

# Change ownership of Tomcat directories to the non-root user
RUN chown -R $UID:$GID /usr/local/tomcat

# Create the runtime directory and set ownership
RUN mkdir -p /pillow-desk-runtime && chown -R $UID:$GID /pillow-desk-runtime && chmod 775 /pillow-desk-runtime

COPY pillow-desk-web/target/PillowDesk /usr/local/tomcat/webapps/ROOT

# Ensure the war file is owned by pillowuser
RUN chown -R $UID:$GID /usr/local/tomcat/webapps/ROOT

USER $UID

EXPOSE 8080
ENV CATALINA_OPTS="$CATALINA_OPTS --enable-native-access=ALL-UNNAMED"
CMD ["catalina.sh", "run"]
