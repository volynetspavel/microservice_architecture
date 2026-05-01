rem --run application without Docker
mvn spring-boot:run
rem --view dependency tree
mvn dependency:tree

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