package main.java.pw.oliver.jmkb;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class JmkbMqttConsumer implements MqttCallback {
	
	private MqttClient client;
	private JmkbKafkaProducer producer;
	private ConfigurationFileReader conf;
	
	public JmkbMqttConsumer(String frostServerURI, String clientId, JmkbKafkaProducer producer) {
		
		conf = new ConfigurationFileReader();
		
		// Initialize new MQTT Client
		try {
			this.producer = producer;
			
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			this.client = new MqttClient(frostServerURI, clientId);
			client.setCallback(this);
			client.connect(options);
			System.out.println("Successfully connected to MQTT");
			client.subscribe("v1.0/Things");
			client.subscribe("v1.0/Datastreams");
			client.subscribe("v1.0/Locations");
			client.subscribe("v1.0/HistoricalLocations");
			client.subscribe("v1.0/Sensors");
			client.subscribe("v1.0/ObservedProperties");
			client.subscribe("v1.0/FeaturesOfInterest");
			client.subscribe("v1.0/Observations");
			System.out.println("Successfully subscribed to topics");
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void testPublish(String topic, String message) {
		try {
			MqttTopic mqttTopic = client.getTopic(topic);
			MqttDeliveryToken token = mqttTopic.publish(new MqttMessage(message.getBytes()));
			token.waitForCompletion(1000);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		System.err.println("Connection to MQTT lost! Details: " + cause.getCause() + " - " + cause.getMessage() + "\n" + cause.getStackTrace());
		try {
			client.close();
			System.exit(-1);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// remove "v1.0/" from topic
		topic = topic.split("/")[1];
		System.out.println(topic + ": " + message);
		SchemaRegistryConnector connector = new SchemaRegistryConnector(conf.getProperty("schemaRegistryURI"));
		String schema = connector.getSchemaById(Integer.parseInt(conf.getProperty("schemaId")));
		byte[] avroMessage = MqttMessageConverter.mqttMessageToAvro(message, schema);
		String key = MqttMessageConverter.getSensorIdFromMessage(message);
		producer.send(topic, key, avroMessage);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		
	}
	
	public void disconnect() {
		try {
			client.disconnect();
			client.close();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
