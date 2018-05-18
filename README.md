# Legeerklaering

Repository for Legeerklaering. Application written in Kotlin used to receive legeerklæringer from external systems,
doing some validation, then pushing it to our internal systems.

## Technologies & Tools

* Kotlin
* CXF
* Gradle

For deployment:
* Docker (tested on 17.03.1-ce)

## Notes on local development

### Running locally with Docker

Utvikler-image (Windows) no-go due to disabled virtualization flags. Need access to Linux image.
Deployment on local machine is possible. Alternatively, provision a Linux server (or VDI) for 
building the Docker images.

* Build a JAR and output it in `target` subdirectory.
* Build Docker image using Dockerfile.
* Run the container and delete on exit.

#### Compile and build JARs + startup scripts:

`./gradlew clean installDist`

#### Build docker container
`docker build -f Dockerfile -t legeerklaering .`

#### Run
`docker run --rm -p 8080:8080 -it legeerklaering`

#### If "port already allocated" errors, find and stop existing containers:
`docker ps` then `docker stop <CONTAINER_NAMES>`

### Testing against Kafka test-rig
IPs and hostnames should be available on the #kafka Slack channel. Still WIP so they'll probably change. 
You might want to consider using a docker image or running the open source confluent suite locally.

### Contact us
#### Code/project related questions can be sent to 
* Kevin Sillerud, `kevin.sillerud@nav.no`
* Joakim Kartveit, `joakim.kartveit@nav.no`

#### For NAV employees
We are also available on the slack channel #integrasjon for internal communication.
