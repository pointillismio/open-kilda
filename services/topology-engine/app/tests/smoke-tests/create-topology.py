#!/usr/bin/python
from kafka import KafkaConsumer, KafkaProducer

bootstrapServer = 'kafka.pendev:9092'
topic = 'kilda-test'

producer = KafkaProducer(bootstrap_servers=bootstrapServer)
producer.send(topic, b'{"type": "INFO", "timestamp": 23478952134, "data": {"message_type": "switch", "switch_id": "00:00:00:00:00:00:00:01", "state": "ADD"}}')
producer.send(topic, b'{"type": "INFO", "timestamp": 23478952135, "data": {"message_type": "switch", "switch_id": "00:00:00:00:00:00:00:02", "state": "ADD"}}')
producer.send(topic, b'{"type": "INFO", "timestamp": 23478952136, "data": {"message_type": "isl", "latency_ns": 1123, "path": [{"switch_id": "00:00:00:00:00:00:00:01", "port_no": 20, "seq_id": "0", "segment_latency": 1123}, {"switch_id": "00:00:00:00:00:00:00:02", "port_no": 1, "seq_id": "1"}]}}')