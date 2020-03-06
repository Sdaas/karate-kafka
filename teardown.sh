#!/bin/bash
echo "*** Stopping and delete all containers ..."
docker-compose -f kafka-single-broker.yml down

# The "docker-compose down" will stop and delete all the containers. To
# just stop the containers ( but leave the images ), use "stop".
# docker-compose -f kafka-single-broker.yml stop
# These containers can always be deleted manually as shown below
#for container in $(docker ps -a | grep "karate-kafka" | cut -f1 -d " "); do docker rm $container; done