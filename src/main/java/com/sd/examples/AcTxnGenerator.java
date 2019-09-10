package com.sd.examples;

public class AcTxnGenerator {

	public static void main(String[] args) throws Exception{
		
		String url = args.length > 0 ? args[0] : "localhost:9092";
		String topic = args.length > 1 ? args[1] : "input";
		String client = "AcTxnGenerator";
		
		System.out.println("Producing messages to : "+url+" : "+topic);
		SampleKafkaProducer producer = new SampleKafkaProducer(url, topic, client);
		String message = null;	
		java.util.Random random = new java.util.Random();
		for(int i =0; i< 10000; i++){
			int randomAccount = random.nextInt(100000);
			message = "ACH#"+randomAccount;
			producer.send(message,message);			
			int randomSleep = random.nextInt(2800);
			Thread.sleep(200+randomSleep);
		}
	}
}
