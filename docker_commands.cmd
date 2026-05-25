rem --build images:
docker build -t resource-service:1.0 .
docker build -t resource-processor:1.0 .
docker build -t song-service:1.0 .
docker build -t eureka-server:1.0 .

rem --run containers from compose.yaml
docker compose up -d
rem --build and run containers from compose.yaml
docker compose up -d --build
docker compose up -d --build localstack
rem --build and run containers from compose.yaml and run 2 song-service containers
docker compose up -d --build --scale song-service=2
rem --run specific services from compose.yaml
docker compose up -d resource-db song-db
docker compose up -d resource-db song-db eureka-server localstack rabbitmq api-gateway
docker compose up -d eureka-server
rem just containers for resource-service
docker compose up -d resource-db
docker compose up -d song-service
docker compose up -d resource-processor
docker compose up -d localstack
docker compose up -d rabbitmq
docker compose up -d api-gateway
rem you can run only resource-service and it will trigger dependency services to run as well
docker compose up -d resource-service

rem --go into container
docker exec -it 5b71c610f633f4f78722d6cd230153b1d018def9651ffad7e009cc850b83ad0c bash
rem --create s3 bucket
awslocal s3 mb s3://my-bucket
docker exec -it 5b71c610f633f4f78722d6cd230153b1d018def9651ffad7e009cc850b83ad0c ls -l /etc/localstack/init-scripts

rem --restart containers
docker compose restart rabbitmq

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

rem -- docker port <container_id> to check which ports are mapped to the container
docker port 6c1f109e40f0

rem --stop containers
docker stop microservice_architecture_overview-resource-db-1
docker stop microservice_architecture_overview-song-db-1
docker stop resource-service
docker stop song-service

rem --Before running the application in Docker, stop any previously running containers
docker compose down
rem --The -v flag removes all anonymous volumes declared in compose.yaml
docker compose down -v

rem --remove unused images, -a flag removes all images without at least one container associated to them
docker image prune -a

rem --check running containers
docker ps