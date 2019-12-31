package picturepi;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Data Provider for PhoneFinder Android app
 * triggers an alarm sound on a mobile phone using MQTT
 */
public class PhoneFinderProvider extends Provider implements IMqttMessageListener {

	PhoneFinderProvider(String phoneId) {
		// no periodic data update
		super(0);
		
		log.fine("creating PhoneFinder provider for phone ID "+phoneId);
		
		mqttTopic = MQTT_BASE+phoneId;
		
		// subscribe to MQTT messages from the specified phone
		log.info("subscribing for MQTT topic "+mqttTopic);
		MqttClient.getMqttClient().subscribe(mqttTopic, this);
		
		log.fine("PhoneFinder provider created for phone ID "+phoneId);
	}

	@Override
	synchronized protected void fetchData() {
		log.fine("sending to MQTT topic "+mqttTopic+":"+MQTT_VALUE_TRIGGER);
		MqttClient.getMqttClient().publish(mqttTopic, MQTT_VALUE_TRIGGER);
		
		if(phoneFinderPanel==null) {
			if(panel instanceof PhoneFinderPanel) {
				phoneFinderPanel = (PhoneFinderPanel)panel;
			}
			else {
				log.severe("Internal Error: Invalid panel type "+panel.getClass().getName());
			}
		}
		
		if(phoneFinderPanel!=null) {
			phoneFinderPanel.setStatusText("wird gesucht");
		}
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("MQTT message received. Topic="+topic+" value="+message.toString());
		
		if(phoneFinderPanel!=null) {
			if(message.toString().equals(MQTT_VALUE_TRIGGER)) {
				log.fine("received trigger message");
			}
			else if(message.toString().equals(MQTT_VALUE_RINGING)) {
				phoneFinderPanel.setStatusText("klingelt");
			}
			else if(message.toString().equals(MQTT_VALUE_FOUND)) {
				phoneFinderPanel.setStatusText("gefunden");
			}
			else {
				log.warning("Unknown MQTT message value for topic "+mqttTopic+":"+message.toString());
			}
		}
	}


	//
	// private members
	//
	private static final Logger   log     = Logger.getLogger( TextWatchPanel.class.getName() );
	
	private static final String   MQTT_BASE = "phonefinder/";
	private static final String   MQTT_VALUE_TRIGGER = "trigger";
	private static final String   MQTT_VALUE_RINGING = "ringing";
	private static final String   MQTT_VALUE_FOUND   = "found";
	
	private final String          mqttTopic;
	private PhoneFinderPanel      phoneFinderPanel = null;
}
