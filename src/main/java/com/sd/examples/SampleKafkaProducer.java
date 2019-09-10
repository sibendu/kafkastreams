package com.sd.examples;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
 
import java.util.Properties;
import java.util.concurrent.ExecutionException;
 
/**
* Producer Example in Apache Kafka
* @author www.tutorialkart.com
*/
public class SampleKafkaProducer {
    private final KafkaProducer producer;
    private final String topic;
    private String clientId;
    
    public SampleKafkaProducer(String url, String topic, String clientId) {
    	url = url == null? "localhost:9092": url;
    	clientId = clientId == null?"SampleKafkaProducer":clientId;
    	
    	this.topic = topic;
        this.clientId = clientId;
        
        Properties properties = new Properties();
        properties.put("bootstrap.servers", url);
        properties.put("client.id", clientId);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer(properties);
    }
 
    public void send(String key, String message) throws Exception {
    	producer.send(new ProducerRecord(topic,key,message)).get();
        System.out.println("Sent to -> "+topic+"; "+key+" : "+message);
    }
}
