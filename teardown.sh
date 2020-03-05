#!/bin/bash
echo "*** Stopping all containers ..."
docker-compose -f kafka-single-broker.yml stop
echo "*** sleeping for 10 seconds (giving containers time to stop) ..."
sleep 10
echo "*** Deleting all the containers ..."
for container in $(docker ps -a | grep "karate-kafka" | cut -f1 -d " "); do docker rm $container; done