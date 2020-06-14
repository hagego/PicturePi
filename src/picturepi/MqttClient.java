package picturepi;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


/**
 * MQTT Client
 */
public class MqttClient implements MqttCallbackExtended{
	
	/**
	 * private constructor
	 * @param brokerAddress MQTT broker address
	 * @param brokerPort    MQTT broker port
	 */
	private MqttClient(String brokerAddress, int brokerPort, int keepalive, String clientId) {
			try {
				MqttConnectOptions connectOptions = new MqttConnectOptions();
				connectOptions.setAutomaticReconnect(true);
				connectOptions.setKeepAliveInterval(keepalive);
				
				isConnected = new AtomicBoolean(false);

				String broker = "tcp://"+brokerAddress+":"+brokerPort;
				log.info("Creating MQTT client for broker "+broker+", client ID="+clientId);
				mqttClient = new org.eclipse.paho.client.mqttv3.MqttAsyncClient(broker, clientId,new MemoryPersistence());
				log.info("client created");
				mqttClient.setCallback(this);
				
				// maintain list of topics to subscribe for automatic reconnect
				topicList = new LinkedList<Topic>();
				
				log.fine("Connecting to MQTT broker "+brokerAddress);
				mqttClient.connect(connectOptions);
			} catch (MqttException e) {
				log.severe("Excepion during MQTT connect: "+e.getMessage());
				log.severe("Excepion during MQTT connect reason="+e.getReasonCode());
			}
	}
	
	/**
	 * @return the singleton MQTT client object if a MQTT broker is specified in the configuration
	 *         or null otherwise 
	 */
	public synchronized static MqttClient getMqttClient() {
		if(theObject==null) {
			log.fine("Creating new MqttClient");
			
			if(Configuration.getConfiguration().getValue("mqtt", "address",null)!=null) {
				theObject = new MqttClient(Configuration.getConfiguration().getValue("mqtt", "address",null),
						                   Configuration.getConfiguration().getValue("mqtt", "port", 1883),
						                   Configuration.getConfiguration().getValue("mqtt", "keepalive", 900),
						                   Configuration.getConfiguration().getValue("global", "name","PicturePi"));
			}
		}
		
		return theObject;
	}
	
	/**
	 * subscribes for an MQTT topic
	 * @param topicName  topic to subscribe
	 * @param listener   listener objects for the callbacl
	 */
	public synchronized void subscribe(String topicName,IMqttMessageListener listener) {
		if(mqttClient==null) {
			log.severe("Unable to subscribe for topic "+topicName+": MQTT client not created");
			
			return;
		}
		log.fine("subscribing for MQTT topic: "+topicName);
		
		// add to list of topics in case we have to reconnect
		Topic topic = new Topic();
		topic.topic    = topicName;
		topic.listener = listener;

		if(topicList.contains(topic)) {
			log.warning("MQTT subscription of topic "+topicName+": topic already subscribed");
		}
		else {
			topicList.add(topic);
		}
		
		// subscribe if we are currently connected
		if(isConnected.get()) {
			try {
				mqttClient.subscribe(topicName,0,listener);
			} catch (MqttException e) {
				log.severe("MQTT subscribe for topic "+topicName+" failed: "+e.getMessage());
			}
		}
	}
	
	/**
	 * publishes an MQTT topic
	 * @param topic  topic to publish to
	 * @param data   data to publish
	 */
	public void publish(String topic,String data) {
		try {
			mqttClient.publish(topic, data.getBytes(), 0, false);
		} catch (MqttException e) {
			log.severe("Unable to publish MQTT topic "+topic+", data="+data);
			log.severe(e.getMessage());
		}
	}

	@Override
	public void connectionLost(Throwable t) {
		isConnected.set(false);
		
		log.severe("connection to MQTT broker lost: "+t.getMessage());
		if(t.getCause()!=null) {
			log.severe("connection to MQTT broker lost cause: "+t.getCause().getMessage());
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken t) {
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("MQTT message arrived: topic="+topic+" content="+message);
	}

	@Override
	public synchronized void connectComplete(boolean reconnect, String serverURI) {
		isConnected.set(true);
		log.info("connection to MQTT broker completed. reconnect="+reconnect);

		// in case of reconnect loop over all topics and subscribe again
		for(Topic topic:topicList) {
			log.info("(re-)subscribing to MQTT topic "+topic.topic);
			
			// subscribe
			try {
				mqttClient.subscribe(topic.topic,0,topic.listener);
			} catch (MqttException e) {
				log.severe("MQTT subscribe for topic "+topic.topic+" failed: "+e.getMessage());
			}
		}
	}
	
	//
	// private data members
	//
	private static final Logger log = Logger.getLogger( MqttClient.class.getName() );
	
	private static  MqttClient                                     theObject = null;
	private         org.eclipse.paho.client.mqttv3.MqttAsyncClient mqttClient;
	
	// private class to hold topics to subscribe together with their listeners
	private class Topic {
		String               topic;
		IMqttMessageListener listener;
		
		@Override
		public boolean equals(Object obj) {
			if(obj==null) {
				return false;
			}
			Topic t = (Topic)obj;
			return topic.equals(t.topic) && listener.equals(t.listener);
		}
	};
	
	private List<Topic>   topicList;              // list of subsribed topics
	private AtomicBoolean isConnected;            // maintains if client is currently connected ot nor
}

