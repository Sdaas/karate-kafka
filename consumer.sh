#!/bin/bash
kafkacat -b localhost:9092 -t test-topic -C \
  -f '\n
  Key (%K bytes): %k
  Value (%S bytes): %s
  Timestamp: %T Partition: %p Offset: %o
  Headers: %h\n'

#kafka-console-consumer \
#    --bootstrap-server localhost:9092 \
#    --topic test-topic \
#    --from-beginning \
#    --formatter kafka.tools.DefaultMessageFormatter \
#    --property print.key=true \
#    --property print.value=true \
#    --property print.timestamp=true \
#    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
#    --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer