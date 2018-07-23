# Pale

Repository for Pale. Application written in Kotlin used to receive legeerklæringer from external systems,
doing some validation, then pushing it to our internal systems.

### Technologies & Tools

* Kotlin
* CXF
* Gradle
* Ktor
* Spek

### Getting started
# Build and run tests
./gradlew installDist

# Running locally
The application can be ran locally using the integration test code using the class `no.nav.pale.PaleIT` in
test. This will create a isolated runtime with every external calls mocked. To send a LE through it it needs to be
packed in the fellesformat, and you can POST it to the diagnostics/rest mock server on the /input endpoint


### Contact us
#### Code/project related questions can be sent to 
* Kevin Sillerud, `kevin.sillerud@nav.no`
* Joakim Kartveit, `joakim.kartveit@nav.no`

#### For NAV employees
We are also available on the slack channel #integrasjon for internal communication.
