#!/bin/bash

## Start up a single single Kafka broker (along with Zookeeper) and create a topic called test-topic
docker-compose -f kafka-single-broker.yml up -d
docker ps
echo "*** sleeping for 15 seconds (give time for containers to spin up)"
sleep 15
echo "*** the following topic were created ...."
kcat -L -b localhost:9092
#kafka-topics -zookeeper 127.0.0.1:2181 --list