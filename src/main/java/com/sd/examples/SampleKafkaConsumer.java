package com.sd.examples;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
 
import java.util.Collections;
import java.util.Properties;

/**
 * Producer Example in Apache Kafka
 * 
 * @author www.tutorialkart.com
 */
public class SampleKafkaConsumer extends Thread {
	private final KafkaConsumer consumer;
	private final String topic;
	private String clientId;

	public static void main(String[] args) throws Exception {
		String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
    	String inputTopic = args.length > 1 ? args[1] : "alert_accounts";
    	//String outputTopic = args.length > 2 ? args[2] : "alert_accounts"; 
    	
		if(System.getenv("KAFKA_BROKER_URL") != null) {
			bootstrapServers = System.getenv("KAFKA_BROKER_URL");
		}
		
		if(System.getenv("KAFKA_TOPIC") != null) {
			inputTopic = System.getenv("KAFKA_TOPIC");
		}
    	
		System.out.println("Starting consumer..");
		String clientId = "SampleKafkaConsumer";
		
		SampleKafkaConsumer consumer = new SampleKafkaConsumer(bootstrapServers, inputTopic, clientId);
		consumer.run();
	}

	public SampleKafkaConsumer(String url, String topic, String clientId) {
		this.topic = topic;
		this.clientId = clientId;

		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, clientId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
 
		consumer = new KafkaConsumer(props);
	}

	public void run() {
		while (true) {
			consumer.subscribe(Collections.singletonList(this.topic));
			ConsumerRecords<Integer, String> records = consumer.poll(1000);
			for (ConsumerRecord record : records) {
				System.out.println("Received message: (" + record.key() + ", " + record.value() + ") at offset "
						+ record.offset());
			}
		}
	}
	
}
