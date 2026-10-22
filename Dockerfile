# Stage 1 - Build
FROM sbtscala/scala-sbt:eclipse-temurin-17_1.x AS build
WORKDIR /app
COPY project/ project/
COPY build.sbt ./
RUN sbt update

# Sources come after dependencies so code changes reuse the cached download
COPY app/ app/
COPY conf/ conf/
COPY public/ public/
RUN sbt stage

# Stage 2 - Run
FROM eclipse-temurin:17-jre
RUN useradd --system app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/universal/stage ./
USER app
EXPOSE 9000
CMD ["bin/scala-api-store", "-Dpidfile.path=/dev/null"]