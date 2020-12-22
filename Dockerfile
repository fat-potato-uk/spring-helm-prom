# ----------------------------------- Build phase ----------------------------------- #
ARG JAVA_VERSION=15
FROM maven:3.6-openjdk-${JAVA_VERSION} AS build

# Resolve the dependencies as an independent layer first
COPY pom.xml /usr/src/app/pom.xml
WORKDIR /usr/src/app
RUN mvn dependency:go-offline

# Copy and build
COPY src /usr/src/app/src
RUN mvn clean package

# ----------------------------------- Deployment phase ----------------------------------- #
# Move artifact into slim container
FROM openjdk:${JAVA_VERSION}-alpine
WORKDIR /usr/src/app
RUN apk add --update ttf-dejavu && rm -rf /var/cache/apk/*
COPY --from=build /usr/src/app/target/spring-helm-prom-1.0-SNAPSHOT.jar /usr/app/spring-helm-prom-1.0-SNAPSHOT.jar
ENTRYPOINT ["java","--enable-preview","-jar","/usr/app/spring-helm-prom-1.0-SNAPSHOT.jar"]