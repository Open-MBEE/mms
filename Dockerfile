FROM eclipse-temurin:17-ubi9-minimal as build
COPY . /mms
WORKDIR /mms

RUN if [ -d "./certs" ]; then \
        mv certs/*.pem /etc/pki/ca-trust/source/anchors/ ; \
        /usr/bin/update-ca-trust extract ; \
        ln -sf /etc/pki/ca-trust/extracted/java/cacerts "$JAVA_HOME/lib/security/cacerts" ; \
    fi

RUN microdnf install -y findutils

RUN ./gradlew --no-daemon bootJar --warning-mode all

RUN find . -type f -name example-*.jar -not -iname '*javadoc*' -not -iname '*sources*' -exec cp '{}' '/app.jar' ';'

FROM eclipse-temurin:17-ubi9-minimal

WORKDIR /opt/mms
COPY --from=build /app.jar /opt/mms/app.jar
COPY --from=build /etc/pki/ca-trust /etc/pki/

ENV JDK_JAVA_OPTIONS "-XX:MaxRAMPercentage=90.0"
ENV USE_SYSTEM_CA_CERTS "true"

CMD ["java", "-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/opt/mms/app.jar"]
EXPOSE 8080
