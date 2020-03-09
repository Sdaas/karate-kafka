#!/bin/bash

# Starts off a console producer for the words-input topic
kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic words-input