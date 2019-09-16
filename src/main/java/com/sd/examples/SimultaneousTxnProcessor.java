/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sd.examples;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.Joined;
import java.time.Duration;
import java.util.Properties;

/**
 * In this example, we implement a simple WordCount program using the high-level
 * Streams DSL that reads from a source topic "streams-plaintext-input", where
 * the values of messages represent lines of text, split each text line into
 * words and then compute the word occurence histogram, write the continuous
 * updated histogram into a topic "streams-wordcount-output" where each record
 * is an updated count of a single word.
 */
public class SimultaneousTxnProcessor {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting ..");

		String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
		String inputTopic_atm = args.length > 1 ? args[1] : "input";
		String inputTopic_web = args.length > 1 ? args[1] : "input_webtxn";
		String outputTopic = args.length > 2 ? args[2] : "alert_accounts";
		int interval = args.length > 3 ? Integer.parseInt(args[3]) : 30; // Default interval 30 seconds 

		if (System.getenv("KAFKA_BROKER_URL") != null) {
			bootstrapServers = System.getenv("KAFKA_BROKER_URL");
		}

		if (System.getenv("KAFKA_TOPIC_1") != null) {
			inputTopic_atm = System.getenv("KAFKA_TOPIC_1");
		}
		
		if (System.getenv("KAFKA_TOPIC_2") != null) {
			inputTopic_web = System.getenv("KAFKA_TOPIC_2");
		}

		if (System.getenv("KAFKA_TOPIC_OUT") != null) {
			outputTopic = System.getenv("KAFKA_TOPIC_OUT");
		}
		
		if (System.getenv("KAFKA_INTERVAL") != null) {
			interval = Integer.parseInt(System.getenv("KAFKA_INTERVAL"));
		}

		
		final Properties streamsConfiguration = new Properties();
		// Give the Streams application a unique name. The name must be unique in the
		// Kafka cluster
		// against which the application is run.
		streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "simul-txn-detection-example");
		streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "simul-txn-detection-example-client");
		// Where to find Kafka broker(s).
		streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		// Specify default (de)serializers for record keys and for record values.
		streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		// Set the commit interval to 500ms so that any changes are flushed frequently.
		// The low latency
		// would be important for anomaly detection.
		streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);

		final Serde<String> stringSerde = Serdes.String();
		final Serde<Long> longSerde = Serdes.Long();

		final StreamsBuilder builder = new StreamsBuilder();

		// Read the source stream. In this example, we ignore whatever is stored in the
		// record key and
		// assume the record value contains the username (and each record would
		// represent a single
		// click by the corresponding user).
		final KStream<String, String> views_atmtxn = builder.stream(inputTopic_atm);
		final KStream<String, String> views_webtxn = builder.stream(inputTopic_web);

		// In this example, we opt to perform an OUTER JOIN between the two streams. We
		// picked this
		// join type to show how the Streams API will send further join updates
		// downstream whenever,
		// for the same join key (e.g. "newspaper-advertisement"), we receive an update
		// from either of
		// the two joined streams during the defined join window.
		final KStream<String, String> simultaneousTxn = views_atmtxn.join(views_webtxn,
				 (key, account) -> "key=" + key + ", account=" + account, // ValueJoiner 
				// KStream-KStream joins are always windowed joins, hence we must provide a join
				// window.
				JoinWindows.of(Duration.ofSeconds(interval)),
				// In this specific example, we don't need to define join serdes explicitly
				// because the key, left value, and
				// right value are all of type String, which matches our default serdes
				// configured for the application. However,
				// we want to showcase the use of `Joined.with(...)` in case your code needs a
				// different type setup.
				Joined.with(Serdes.String(), /* key */
						Serdes.String(), /* left value */
						Serdes.String() /* right value */
				));
		
		// Write the results to the output topic.
		simultaneousTxn.to(outputTopic);

		final KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);
		streams.cleanUp();
		streams.start();

		// Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
		System.out.println("Reached end");
	}
}
