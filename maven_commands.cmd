rem --run application without Docker
mvn spring-boot:run
rem --view dependency tree
mvn dependency:tree

rem -- build and run all tests
mvn clean install

rem --run unit tests + INTEGRATION tests (go to WSL in project folder!!)
mvn verify -DskipITs=false
rem --run specific IT Resource Service
mvn test -Dtest=ResourceRepositoryIT
mvn test -Dtest=CloudStorageServiceIT
mvn test -Dtest=ResourceUploadedEventPublisherIT
rem -- Resource Processor IT
mvn test -Dtest=ResourceUploadedConsumerIT
rem -- Song Service IT
mvn test -Dtest=SongRepositoryIT

rem --run unit tests + COMPONENT tests (go to WSL in project folder!!)
mvn verify -DskipCTs=false

rem --run unit tests + CONTRACT tests (go to WSL in project folder!!)
mvn verify -DskipContracts=false

rem --run E2E test
mvn -f resource-service/pom.xml test -Dtest="ResourceServiceE2E" --no-transfer-progress

rem --run any test via shell script, just set test name and service
./run-integration-test.sh SongControllerComponentIT song-service                               │
./run-integration-test.sh ProcessorCT resource-processor                                   │
./run-integration-test.sh ResourceRepositoryIT resource-service

rem --check if ports are in use
netstat -ano | findstr :8081
rem --check if ports are in use and filter for LISTENING state
netstat -aon | findstr LISTENING | findstr 8081
rem --kill process using port 8081
taskkill /PID 0 /F

rem Eureka Server dashboard
http://localhost:8761/

rem LocalStack dashboard
https://app.localstack.cloud/inst/default/overview
