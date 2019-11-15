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
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;

import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;

import java.util.Properties;
import java.util.HashMap;
import java.util.Map;


import java.time.Duration;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;


/**
 * In this example, we implement a simple WordCount program using the high-level Streams DSL
 * that reads from a source topic "streams-plaintext-input", where the values of messages represent lines of text,
 * split each text line into words and then compute the word occurence histogram, write the continuous updated histogram
 * into a topic "streams-wordcount-output" where each record is an updated count of a single word.
 */
public class FrequentTxnProcessor {

    public static void main(String[] args) throws Exception {
    	System.out.println("Starting ..");
    	
    	String bootstrapServers = args.length > 0 ? args[0] : "129.213.158.157:9092";
    	String inputTopic = args.length > 1 ? args[1] : "jdbc-mysql-account_txn";
    	String outputTopic = args.length > 2 ? args[2] : "alerts";    
    	int interval = args.length > 3 ? Integer.parseInt(args[3]) : 1;
    	
		if(System.getenv("KAFKA_BROKER_URL") != null) {
			bootstrapServers = System.getenv("KAFKA_BROKER_URL");
		}
		
		if(System.getenv("KAFKA_TOPIC") != null) {
			inputTopic = System.getenv("KAFKA_TOPIC");
		}
		
		if(System.getenv("KAFKA_TOPIC_OUT") != null) {
			outputTopic = System.getenv("KAFKA_TOPIC_OUT");
		}
		
		if(System.getenv("KAFKA_INTERVAL") != null) {
			interval = Integer.parseInt(System.getenv("KAFKA_INTERVAL"));
		}
    	
        final Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "frequent-txn");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "frequent-txn-client");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Set the commit interval to 500ms so that any changes are flushed frequently. The low latency
        // would be important for anomaly detection.
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);

        //streamsConfiguration.put("default.deserialization.exception.handler", LogAndContinueExceptionHandler.class);
        
        
        //streamsConfiguration.put("key.serializer", "org.apache.kafka.connect.json.JsonConverter");
        //streamsConfiguration.put("key.deserializer", "org.apache.kafka.connect.json.JsonConverter");
        //streamsConfiguration.put("value.serializer", "org.apache.kafka.connect.json.JsonConverter");
        //streamsConfiguration.put("value.deserializer", "org.apache.kafka.connect.json.JsonConverter");
        
        final Serde <String> stringSerde = Serdes.String();
        final Serde <Long> longSerde = Serdes.Long();
        
        //Make a Serde for AccountTxn
        Map <String, Object> serdeProps = new HashMap <> ();
        final Serializer <AccountTxn> accountTxnSerializer = new JsonPOJOSerializer<>();
        serdeProps.put("JsonPOJOClass", AccountTxn.class);
        accountTxnSerializer.configure(serdeProps, false);
        
        final Deserializer <AccountTxn> accountTxnDeserializer = new JsonPOJODeserializer<>();
        serdeProps.put("JsonPOJOClass", AccountTxn.class);
        accountTxnDeserializer.configure(serdeProps, false);
        
        //final Serde <AccountTxn> accountTxnSerde = Serdes.serdeFrom(accountTxnSerializer, accountTxnDeserializer);
        final Serde <AccountTxn> accountTxnSerde = Serdes.serdeFrom(accountTxnSerializer, accountTxnDeserializer);        
        System.out.println("accountTxnSerde :: " + accountTxnSerde.getClass().getName());
        
        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerde.getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, stringSerde.getClass().getName());
        
        
        final StreamsBuilder builder = new StreamsBuilder();
		
        // Read the source stream.  In this example, we ignore whatever is stored in the record key and
        // assume the record value contains the username (and each record would represent a single
        // click by the corresponding user).
        
        //System.out.println("1");
        final KStream<String, AccountTxn> views = builder.stream(inputTopic, Consumed.with(Serdes.String(), accountTxnSerde));
        //System.out.println("2");
        
        final KTable<Windowed<String>, Long> anomalousUsers = views
          // map the user name as key, because the subsequent counting is performed based on the key
          .map((ignoredKey, account) -> new KeyValue<>(account.getAccount(), account.getAccount()))
          // count users, using one-minute tumbling windows;
          // no need to specify explicit serdes because the resulting key and value types match our default serde settings
          .groupByKey()
          .windowedBy(TimeWindows.of(Duration.ofMinutes(interval)))
          .count()
          // get users whose one-minute count is >= 3
          .filter((windowedUserId, count) -> count >= 3);
        
        //System.out.println("3");
        
        // Note: The following operations would NOT be needed for the actual anomaly detection,
        // which would normally stop at the filter() above.  We use the operations below only to
        // "massage" the output data so it is easier to inspect on the console via
        // kafka-console-consumer.
        final KStream<String, String> anomalousUsersForConsole = anomalousUsers
          // get rid of windows (and the underlying KTable) by transforming the KTable to a KStream
          .toStream()
          // sanitize the output by removing null record values (again, we do this only so that the
          // output is easier to read via kafka-console-consumer combined with LongDeserializer
          // because LongDeserializer fails on null values, and even though we could configure
          // kafka-console-consumer to skip messages on error the output still wouldn't look pretty)
          .filter((windowedUserId, count) -> count != null)
          .map((windowedUserId, count) -> new KeyValue<>(windowedUserId.toString(), count + " transactions within 1 minute"));
        
        //System.out.println("4");
        
        // write to the result topic 
        anomalousUsersForConsole.to(outputTopic, Produced.with(stringSerde, stringSerde)); 

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
        System.out.println("Reached end ..");
    }
}
