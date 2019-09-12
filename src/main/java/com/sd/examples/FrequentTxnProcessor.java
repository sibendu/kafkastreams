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

import java.time.Duration;
import java.util.Properties;

/**
 * In this example, we implement a simple WordCount program using the high-level Streams DSL
 * that reads from a source topic "streams-plaintext-input", where the values of messages represent lines of text,
 * split each text line into words and then compute the word occurence histogram, write the continuous updated histogram
 * into a topic "streams-wordcount-output" where each record is an updated count of a single word.
 */
public class FrequentTxnProcessor {

    public static void main(String[] args) throws Exception {
    	System.out.println("Starting ..");
    	
    	String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
    	String inputTopic = args.length > 1 ? args[1] : "input";
    	String outputTopic = args.length > 2 ? args[2] : "alert_accounts";    
    	
		if(System.getenv("KAFKA_BROKER_URL") != null) {
			bootstrapServers = System.getenv("KAFKA_BROKER_URL");
		}
		
		if(System.getenv("KAFKA_TOPIC") != null) {
			inputTopic = System.getenv("KAFKA_TOPIC");
		}
		
		if(System.getenv("KAFKA_TOPIC_OUT") != null) {
			outputTopic = System.getenv("KAFKA_TOPIC_OUT");
		}
    	
        final Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "anomaly-detection-example");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "anomaly-detection-example-client");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        // Set the commit interval to 500ms so that any changes are flushed frequently. The low latency
        // would be important for anomaly detection.
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);

        final Serde<String> stringSerde = Serdes.String();
        final Serde<Long> longSerde = Serdes.Long();

        final StreamsBuilder builder = new StreamsBuilder();

        // Read the source stream.  In this example, we ignore whatever is stored in the record key and
        // assume the record value contains the username (and each record would represent a single
        // click by the corresponding user).
        final KStream<String, String> views = builder.stream(inputTopic);

        final KTable<Windowed<String>, Long> anomalousUsers = views
          // map the user name as key, because the subsequent counting is performed based on the key
          .map((ignoredKey, username) -> new KeyValue<>(username, username))
          // count users, using one-minute tumbling windows;
          // no need to specify explicit serdes because the resulting key and value types match our default serde settings
          .groupByKey()
          .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
          .count()
          // get users whose one-minute count is >= 3
          .filter((windowedUserId, count) -> count >= 3);

        // Note: The following operations would NOT be needed for the actual anomaly detection,
        // which would normally stop at the filter() above.  We use the operations below only to
        // "massage" the output data so it is easier to inspect on the console via
        // kafka-console-consumer.
        final KStream<String, Long> anomalousUsersForConsole = anomalousUsers
          // get rid of windows (and the underlying KTable) by transforming the KTable to a KStream
          .toStream()
          // sanitize the output by removing null record values (again, we do this only so that the
          // output is easier to read via kafka-console-consumer combined with LongDeserializer
          // because LongDeserializer fails on null values, and even though we could configure
          // kafka-console-consumer to skip messages on error the output still wouldn't look pretty)
          .filter((windowedUserId, count) -> count != null)
          .map((windowedUserId, count) -> new KeyValue<>(windowedUserId.toString(), count));

        // write to the result topic
        anomalousUsersForConsole.to(outputTopic, Produced.with(stringSerde, longSerde)); 

        final KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);
        // Always (and unconditionally) clean local state prior to starting the processing topology.
        // We opt for this unconditional call here because this will make it easier for you to play around with the example
        // when resetting the application for doing a re-run (via the Application Reset Tool,
        // http://docs.confluent.io/current/streams/developer-guide.html#application-reset-tool).
        //
        // The drawback of cleaning up local state prior is that your app must rebuilt its local state from scratch, which
        // will take time and will require reading all the state-relevant data from the Kafka cluster over the network.
        // Thus in a production scenario you typically do not want to clean up always as we do here but rather only when it
        // is truly needed, i.e., only under certain conditions (e.g., the presence of a command line flag for your app).
        // See `ApplicationResetExample.java` for a production-like example.
        streams.cleanUp();
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        System.out.println("Reached end");
    }
}
