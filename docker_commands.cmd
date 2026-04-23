rem --build images:
docker build -t resource-service:1.0 .
docker build -t song-service:1.0 .
docker build -t eureka-server:1.0 .

rem --run containers from compose.yaml
docker compose up -d
rem --build and run containers from compose.yaml
docker compose up -d --build
rem --build and run containers from compose.yaml and run 2 song-service containers
docker compose up -d --build --scale song-service=2
rem --run specific services from compose.yaml
docker compose up -d resource-db song-db
docker compose up -d resource-db song-db eureka-server
docker compose up -d eureka-server
rem just containers for resource-service
docker compose up -d resource-db
docker compose up -d resource-service
docker compose up -d localstack

rem --run container
docker run -d --name resource-service -p 8081:8081 resource-service:1.0
docker run -d --name song-service -p 8082:8082 song-service:1.0
docker run -d --name eureka-server -p 8761:8761 eureka-server:1.0

rem --run container with network, use this
docker run -d --name resource-service --net=microservice_architecture_overview_resource-network -p 8081:8081 resource-service:1.0
docker run -d --name song-service --net=microservice_architecture_overview_song-network -p 8082:8082 song-service:1.0

rem --connect to network between resource-service and song-service
docker network connect microservice_architecture_overview_default resource-service
docker network connect microservice_architecture_overview_default song-service

rem --stop containers
docker stop microservice_architecture_overview-resource-db-1
docker stop microservice_architecture_overview-song-db-1
docker stop resource-service
docker stop song-service

rem --Before running the application in Docker, stop any previously running containers
docker compose down

rem --remove unused images, -a flag removes all images without at least one container associated to them
docker image prune -a