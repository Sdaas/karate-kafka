#!/bin/bash

# Starts off a console consumer for the words-output topic
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic words-output \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer