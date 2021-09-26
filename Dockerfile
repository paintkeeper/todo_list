FROM gradle:6.3.0-jdk14 as build

COPY --chown=gradle:gradle src/ /home/gradle/app/src
COPY --chown=gradle:gradle build.gradle.kts /home/gradle/app
COPY --chown=gradle:gradle gradle.properties /home/gradle/app
COPY --chown=gradle:gradle settings.gradle.kts /home/gradle/app

WORKDIR /home/gradle/app

RUN gradle --no-daemon build -x test

FROM adoptopenjdk:14-jre-hotspot

EXPOSE 8080

COPY --from=build /home/gradle/app/build/libs/*.jar /app/application.jar

USER 1000

WORKDIR /app

CMD [ "java", \
      "-Xmx1024m", "-Djava.security.egd=file:/dev/./urandom", "-Dfile.encoding=UTF-8", \
      "-jar", "application.jar" ]