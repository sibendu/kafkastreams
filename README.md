# Kafka Streams Demo

This is a sample project with Kafka stream processors deployed on K8S. Kafka server is outside K8S
It is tested with OCI vm and OKE. 

## Kafka configuration 

Start the broker, and create three topics. 
1. input
2. input_webtxn
3. alert_accounts

## To generate stream processors, package and deploy them on k8s, use below steps - 

1. Generate executable jar: mvn clean install  -Dmain.Class="Main_Class"
2. Generate docker image:  docker build -t sibendudas/<image_name> .
3. Push to docker hub:  docker push sibendudas/<image_name> .
4. Deploy: kubectl apply -f deploy-***.yaml
  
This example has following three processors:

1. com.sd.examples.FrequentTxnProcessor : Checks on a topic for multiple txns on same account# within n minutes interval. Topic name and 
interval are configurable using deploy-stream-frequent-txn.yaml. Generates alert message in output topic.

2. com.sd.examples.SimultaneousTxnProcessor: Checks streams across two topics for records related to same account# within n seconds interval. Topic name and interval are configurable through deploy-stream-simultaneous-txn.yaml. Generates alert message in output topic

3. com.sd.examples.AlertHandler: Checks input topic (configurable parameter in deploy-account-alert-handler.yaml) for stream of alert messages, as produced by above two streams. Processes the alert message, inserts a record in the MySQL DB (parameters in yaml)

Few other classes that might be useful:
1. com.sd.examples.AcTxnGenerator : Generates dummy records; sends to KAFKA_BROKER_URL, KAFKA_TOPIC  (parameterized as env variables)
2. com.sd.examples.SampleKafkaProducer : Generates dummy records; can enter input through console 

## To visualize data

1. Configure Kafka broker to start with JMX exporter 
2. Install and run prometheus; configured to scrape JMX metrics
3. Grafana dashboard to visualize the txns, and the alert records in MySQL tables 

## Note

1. Need to configure security lists to allow traffic from OKE to Kafka vm

2. In order to make Kafka producers and consumers work on K8S, need to set advertised.listeners in $KAFKA_HOME/config/server.properties 
Uncomment and set :: #advertised.listeners=PLAINTEXT://your.host.name:9092

3. MySQL need to be configured for remote access
