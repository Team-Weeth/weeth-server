FROM eclipse-temurin:21-jre-alpine

ARG OTEL_JAVA_AGENT_VERSION=2.26.1

WORKDIR /app

ADD https://repo.maven.apache.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent-${OTEL_JAVA_AGENT_VERSION}.jar /otel/opentelemetry-javaagent.jar

COPY build/libs/*.jar app.jar

ENTRYPOINT ["sh", "-c", "if [ \"${OTEL_JAVAAGENT_ENABLED:-true}\" = \"true\" ]; then exec java -javaagent:/otel/opentelemetry-javaagent.jar -jar /app/app.jar; else exec java -jar /app/app.jar; fi"]
