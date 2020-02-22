#!/bin/bash

## Start up a single single Kafka broker (along with Zookeeper) and create a topic called test-topic
docker-compose -f kafka-single-broker.yml up -d
docker ps
echo "*** sleeping for 5 seconds (give time for containers to spin up)"
sleep 5
echo "*** sleeping for 5 seconds (give time for all topics to be created)"
sleep 5
echo "*** the following topic were created ...."
kafka-topics.sh -zookeeper 127.0.0.1:2181 --list