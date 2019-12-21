package picturepi;

import java.util.logging.Logger;

/**
 * Data Provider for GarageDoor Panel
 * triggers opening/closing of garage door via MQTT
 */
public class GarageDoorProvider extends Provider {

	GarageDoorProvider() {
		// update data every second
		super(0);
		
		log.fine("GarageDoor provider created");
		
		// MQTT topic to toggle the door
		mqttTopic = Configuration.getConfiguration().getValue("GarageDoorPanel", "mqttTopic", null);
		if(mqttTopic != null) {
			log.info("MQTT topic: "+mqttTopic);
		}
		else {
			log.warning("No MQTT topic found for GarageDoor provider - disabling functionality");
		}
	}

	@Override
	synchronized protected void fetchData() {
		if(mqttTopic!=null) {
			log.fine("toggling garage door");
			MqttClient.getMqttClient().publish(mqttTopic, new String());
		}
		else {
			log.warning("No MQTT topic defined - functionality disabled");
		}
	}


	//
	// private members
	//
	private static final Logger   log     = Logger.getLogger( TextWatchPanel.class.getName() );
	
	private String mqttTopic;
}
