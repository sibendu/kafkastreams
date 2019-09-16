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
public class AlertHandler extends Thread {
	private final KafkaConsumer consumer;
	private final String topic;
	private String clientId;
	private AlertDAO dao;

	public static void main(String[] args) throws Exception {
		String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
		String inputTopic = args.length > 1 ? args[1] : "alert_accounts";
		// String outputTopic = args.length > 2 ? args[2] : "alert_accounts";

		if (System.getenv("KAFKA_BROKER_URL") != null) {
			bootstrapServers = System.getenv("KAFKA_BROKER_URL");
		}

		if (System.getenv("KAFKA_TOPIC") != null) {
			inputTopic = System.getenv("KAFKA_TOPIC");
		}

		System.out.println("Starting consumer..");
		String clientId = "SampleKafkaConsumer";


		AlertHandler consumer = new AlertHandler(bootstrapServers, inputTopic, clientId);
		consumer.run();
	}

	public AlertHandler(String url, String topic, String clientId) {
		this.topic = topic;
		this.clientId = clientId;

		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, clientId);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
		props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
		props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");

		consumer = new KafkaConsumer(props);
		
		dao = new AlertDAO(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PWD"));
	}

	public void run() {
		while (true) {
			consumer.subscribe(Collections.singletonList(this.topic));
			ConsumerRecords<Integer, String> records = consumer.poll(1000);
			for (ConsumerRecord record : records) {
				
				String account = record.key() == null? "": "@:" + record.key().toString();
				String value = record.value() ==null? "":record.value().toString();
				//String details = account + " has " + value + " transactions within 1 minute" ;
				
				System.out.println("Received message: (" + account + ", " + value + ") at offset "
						+ record.offset());
				
				processAlert(account, 0, value);
			}
		}
	}

	public void processAlert(String account, Integer no_access, String details) {
		try {
			String code = "FREQUENT_TXN";
			if(details != null && details.contains("across channels")) {
				code = "TXN_ACROSS_CHANNELS";
			}
			dao.processAlert(code, null, account, no_access, details, "NEW");
			System.out.println("Alert record created in DB");
		} catch (Exception e) {
			System.out.println("Error in inserting alert: " + e.getMessage());
		}
	}

}
