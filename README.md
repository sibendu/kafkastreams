# kafkastreams
This is a sample project with Kafka stream processors deployed on K8S. Kafka server is outside K8S
It is tested with OCI vm and OKE. 

# To generate executable jar - 
mvn clean install  -Dmain.Class="<Main Class Name>"   

Following are the main classes in this project:
-----------------------------------------------
com.sd.examples.AcTxnGenerator : Generates dummy transactions; sends to KAFKA_BROKER_URL, KAFKA_TOPIC  (parameterized as env variables)
com.sd.examples.FrequentTxnProcessor : Stream processor, monitors KAFKA_TOPIC, if same more than 5 txns on same account within 1 minutes tumbling windows, posts a message to KAFKA_ALERT_QUEUE 

# Note
1. In order to make Kafka producers and consumers work on K8S, need to set advertised.listeners in $KAFKA_HOME/config/server.properties 
Uncomment and set :: #advertised.listeners=PLAINTEXT://your.host.name:9092

2. Need to configure security lists to allow traffic from OKE to Kafka vm
