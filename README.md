# Authentication Microservices

**_Description_**

tbd.

---

- [Architecture](#architecture)
- [Examples and Usage](#examples-and-usage)
- [Build and Run](#build-and-run)
- [A Word on Security](#a-word-on-security)

## Architecture

tbd.

## Examples and Usage

Replace `$HOST` with the host name or IP address the service is running on.

ℹ️ For testing and debugging purposes you can access demo HTML forms for each microservice, e.g. `$HOST/register.html` or `$HOST/refresh.html`.

**Register and auto-login (create token pair):**
```
curl -XPOST \
     -H "Content-Type: application/json" \
     -d '{ "username": "john_doe", "secret": "secure-password" }'
     http://$HOST/auth/register
```
Returns the JWT (access token) and its expiration date in the response body, and a refresh token in the `Set-Cookie` header.
This process stores the credentials securely in the database (service: _credentials-store_) as well as the refresh token (service: _token-store_).
Credentials are stored without any `roles`; you can add them by calling the appropriate REST endpoint, see below.

**Register only (no login):**
Append `?no-login` to the register URL. The response contains only the user ID (the _internal_ user ID, which you may use for updating the credentials, see below).

**Login:**
```
curl -XPOST \
     -H "Content-Type: application/json" \
     -d '{ "username": "john_doe", "secret": "secure-password" }' \
     http://$HOST/auth/login
```

**Refresh token:**
```
curl -XPOST \
     -H "Content-Type: application/json" \
     --cookie $REFRESH_COOKIE \
     http://$HOST/auth/refresh
```

Replace `$REFRESH_COOKIE` with the refresh token value you received from the login/register endpoint.

**Update credentials:**

Username/Secret:
```
curl -XPUT \
     -H "Content-Type: text/plain" \
     -d "new_password" \
     http://$HOST/auth/$USER_ID/secret
```
Replace `secret` with `username` to change the username. You need a valid access token of the appropriate user or a token with the `ROLE_ADMIN` role.

Groups:
```
curl -XPUT \
     -H "Content-Type: application/json" \
     -d "[\"ROLE_ADMIN\", \"ROLE_USER\"]" \
     http://$HOST/auth/$USER_ID/groups
```
You need a valid access token with the `ROLE_ADMIN` role to change the groups.

## Build and Run

There are multiple ways for building and running these microservices.

- **[Build with Java and run inside JVM](#build-with-java-and-run-inside-jvm)**
    - ✅ Fastest compilation (some seconds),
    - ✅ neither GraalVM nor Docker required,
    - ❌ but JVM (Java 11+) required,
    - ❌ slower than the native build binary at runtime + higher memory consumption and
    - ❌ noticeable slower startup and shutdown times (ca. 1 s).
- **[Build and run native binary locally](#build-and-run-native-binary-locally)**
    - ✅ Best runtime performance, little memory usage, native to your platform,
    - ✅ fastest startup and shutdown (less than 0.01 s),
    - ✅ no JVM required at runtime, no Docker required.
    - ❌ GraalVM required for compilation,
    - ❌ native builds not portable to other operating systems (rebuild required),
    - ❌ long compilation times (1+ min.).
    - ⚠️ The database-dependent microservices still need a JVM runtime for the embedded H2 database (though you are free to replace these services with a custom implementation without this constraint, of course).
- **[Build and run native images inside Docker container](#build-and-run-native-images-inside-docker-container)**
    - ✅ Docker is single requirement, everything else happens inside container.
    - ✅ As portable as Docker is.
    - ✅ Other advantages of native images apply.
    - ✅ System isolation and management via Docker (compose) file.
    - ❌ Even longer compilation times (minutes) and
    - ❌ general overhead that comes with Docker.
- **[Build Docker-native images locally and run inside Docker container](#build-docker-native-images-locally-and-run-inside-docker-container)**
    - ✅ Once compiled locally, same advantages of previous method apply,
    - ✅ but faster compilation if you have the JVM/GraalVM/Maven tool-chain installed anyway.
    - ❌ Same downsides of previous method apply,
    - ❌ plus JVM/GraalVM needed for compilation.

### Build With Java and Run Inside JVM

#### Build

**With the build helper script (recommended):** Run the `build-microservices.sh` script from the root directory.
Without arguments, the `package` goal is assumed, so you'll end up with JAR files. You can pass any goal and argument to the script (e.g. `-DskipTests`).

ℹ️ _Note: The first time, you need to run the `build-shared-modules.sh` script which installs the shared modules into your local_ .m2 _directory._

**Manually:** Build with Maven by running the `package` goal in every microservice directory. As shared modules in this project aren't distributed via a Maven repository, you'll need to `install` them locally first.

#### Run

Run all generated microservices from the `target/` directories of each microservice (`*-runner.jar`).

### Build and Run Native Binary Locally

#### Build

Same as JVM method but also pass `-Pnative` to the build script or Maven.

#### Run

Run all generated microservices from the `target/` directories of each microservice (`*-runner`).

ℹ️ _Note: Microservices which are not native-compatible (the `-store` services) still need to be run in a JVM (`java -jar *-runner.jar`)._

### Build and Run Native Images Inside Docker Container

See the [Quarkus Docs](https://quarkus.io/guides/building-native-image#creating-a-container-with-a-multi-stage-docker-build) on how to achieve such multi-stage Docker builds.

### Build Docker-native Images Locally and Run Inside Docker Container

**With the build helper script (recommended):**
```
./build-microservices.sh package -Pnative -Dquarkus.native.container-build=true
```

This creates Docker-native builds of all microservices and runs `docker build` on all Dockerfiles.
After that, you can run all containers.

**Run with docker-compose (recommended):**
```
docker-compose up
```

The compose-file only exposes the login-server microservice, via port 8080.

See the `docker-compose.yml` file in the root directory for details.

**Manually:**
```
docker run -i --rm -p $PORT:8080 $SERVICE
```
Replace `$PORT` with the local port you want to be forwarded to the container.
Replace `$SERVICE` with the microservice name (docker tag, as created by the previous command).
Remove the `--rm` flag if you want to persist the container between runs.

Without docker-compose, you also need to create a bridge network. See the [Docker Docs](https://docs.docker.com/network/bridge/) for instructions.

## A Word on Security

tbd.
