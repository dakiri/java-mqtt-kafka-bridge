package main.java.pw.oliver.jmkb;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.log4j.BasicConfigurator;

/**
 * This class is a bridge between a FROST-Server and Apache Kafka.
 * It serves as a MQTT consumer and an Apache Kafka Producer.
 * 
 * @author Oliver
 * 
 * @version 1.0
 */

public class Main {
	
	public static void main(String[] args) {
		
		// for reading from jmkb.properties
		ConfigurationFileReader conf = new ConfigurationFileReader();
		
		// for logging
		BasicConfigurator.configure();
		
		String frostServerURI = conf.getProperty("frostServerURI");
		String kafkaBrokerURI = conf.getProperty("kafkaBrokerURI");
		String schemaRegistryURI = conf.getProperty("schemaRegistryURI");
			
		// prepend tcp:// to frostServerURI if no protocol is defined (required for MQTT)
		if (!frostServerURI.contains("://")) {
			frostServerURI = "tcp://" + frostServerURI;
		}

		// prepend http:// to kafkaServerURI if no protocol is defined
		if (!kafkaBrokerURI.contains("://")) {
			kafkaBrokerURI = "http://" + kafkaBrokerURI;
		}

		// prepend http:// to schemaRegistryURI if no protocol is defined
		if (!schemaRegistryURI.contains("://")) {
			schemaRegistryURI = "http://" + schemaRegistryURI;
		}

		// check validity of URIs
		try {
			URI uriFrost  = new URI(frostServerURI);
			URI uriKafka  = new URI(kafkaBrokerURI);
			URI uriSchema = new URI(schemaRegistryURI);

			// check if port for FROST was specified
			if (uriFrost.getPort() == -1) {
				System.err.println("Bad URI format: No port defined for FROST-Server. Defaulting to port 1883");
				uriFrost = new URI(uriFrost.toString() + ":1883");
			}

			// check if port for Kafka was specified
			if (uriKafka.getPort() == -1) {
				System.err.println("Bad URI format: No port defined for Kafka Broker. Defaulting to port 9092");
				uriKafka = new URI(uriKafka.toString() + ":9092");
			}

			// check if port for Schema Registry was specified
			if (uriSchema.getPort() == -1) {
				System.err.println("Bad URI format: No port defined for the Schema Registry. Defaulting to port 8081");
				uriSchema = new URI(uriSchema.toString() + ":8081");
			}

			frostServerURI = uriFrost.toString();
			kafkaBrokerURI = uriKafka.toString();
			schemaRegistryURI = uriSchema.toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		JmkbKafkaProducer producer = new JmkbKafkaProducer(kafkaBrokerURI, schemaRegistryURI);
		JmkbMqttConsumer consumer = new JmkbMqttConsumer(frostServerURI, "mqttconsumer1", producer);
		
		// set shutdown hook so that program can terminate gracefully when user presses Ctrl+C
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Performing shutdown.");
				consumer.disconnect();
				System.out.println("MQTT consumer shutdown.");
				producer.disconnect();
				System.out.println("Kafka producer shutdown.");
				System.out.println("Shutdown complete.");
			}
		});
		
		System.out.println("The bridge is now running, terminate with Ctrl+C.");
		
		for (int i = 0; i <= 10; i++) {
			consumer.testPublish("v1.0/HistoricalLocations", "TESTESTEST");
		}
		
		while(true);
	}
	
}
