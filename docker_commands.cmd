rem --run containers from compose.yaml
docker compose up -d
docker compose up -d --build
rem --run specific services from compose.yaml
docker compose up -d resource-db song-db

rem --build images: 
docker build -t resource-service:1.0 .
docker build -t song-service:1.0 .

rem --run container
docker run -d --name resource-service -p 8081:8081 resource-service:1.0
docker run -d --name song-service -p 8082:8082 song-service:1.0

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