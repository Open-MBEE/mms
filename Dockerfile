FROM openjdk:17.0.2-slim as build
COPY . /mms
WORKDIR /mms
RUN ./gradlew --no-daemon bootJar --warning-mode all

RUN find . -type f -name example-*.jar -not -iname '*javadoc*' -not -iname '*sources*' -exec cp '{}' '/app.jar' ';'
ENV JDK_JAVA_OPTIONS "-XX:MaxRAMPercentage=90.0"
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-javaagent:jacocoJars/jacocoagent.jar=destfile=jacocoOutput/jacoco-it.exec,output=file", "-jar", "/app.jar"]
EXPOSE 8080
