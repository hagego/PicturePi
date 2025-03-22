package picturepi;

import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * data provider for the interactive panel
 */
public class InteractiveDataProvider extends Provider implements IMqttMessageListener {
    /**
     * constructor
     */
    InteractiveDataProvider() {
        super(0);
    }

    /**
     * gets called after all panels and provides got instantiated
     */
    protected void init() {
        super.init();

        // get the panel this provider is responsible for
        Panel panel = getPanel();
        if(panel instanceof InteractivePanel) {
            interactivePanel = (InteractivePanel)panel;
        }
        else {
            log.severe("Panel is not an instance of InteractivePanel");
            return;
        }

        // subscribe to MQTT topics to retrieve measured temperature updates
		mqttTopicTemperature = Configuration.getConfiguration().getValue(InteractivePanel.class.getSimpleName(), CONFIG_KEY_MQTT_TOPIC_TEMPERATURE, null);
		if(mqttTopicTemperature != null) {
			log.info("subscribing for temperature, MQTT key="+mqttTopicTemperature);
			MqttClient.getMqttClient().subscribe(mqttTopicTemperature, this);
		}
		mqttTopicTemperatureMin = Configuration.getConfiguration().getValue(InteractivePanel.class.getSimpleName(), CONFIG_KEY_MQTT_TOPIC_TEMPERATURE_MIN, null);
		if(mqttTopicTemperatureMin != null) {
			log.info("subscribing for min temperature");
			MqttClient.getMqttClient().subscribe(mqttTopicTemperatureMin, this);
		}
		mqttTopicTemperatureMax = Configuration.getConfiguration().getValue(InteractivePanel.class.getSimpleName(), CONFIG_KEY_MQTT_TOPIC_TEMPERATURE_MAX, null);
		if(mqttTopicTemperatureMax != null) {
			log.info("subscribing for min temperature");
			MqttClient.getMqttClient().subscribe(mqttTopicTemperatureMax, this);
		}
        mqttTopicTemperature2 = Configuration.getConfiguration().getValue(InteractivePanel.class.getSimpleName(), CONFIG_KEY_MQTT_TOPIC_TEMPERATURE2, null);
		if(mqttTopicTemperature2 != null) {
			log.info("subscribing for temperature2, MQTT key="+mqttTopicTemperature2);
			MqttClient.getMqttClient().subscribe(mqttTopicTemperature2, this);
		}
    }


    @Override
    void fetchData() {
        // TODO Auto-generated method stub
    }

    //
    // private methods
    //

    /**
     * MQTT message arrived
     */
    @Override
    public void messageArrived(String topic, MqttMessage message)  {
        log.fine("messageArrived: topic="+topic+", message="+message.toString());

        try {
            if(interactivePanel!=null && mqttTopicTemperature!=null && topic.equals(mqttTopicTemperature)) {
                log.fine("Updating actual temperature with "+message.toString());
                interactivePanel.setTemperature1(Double.parseDouble(message.toString()));
            }
            if(interactivePanel!=null && mqttTopicTemperature2!=null && topic.equals(mqttTopicTemperature2)) {
                log.fine("Updating actual temperature2 with "+message.toString());
                interactivePanel.setTemperature2(Double.parseDouble(message.toString()));
            }
        }
        catch(Exception e) {
            log.severe("Exception in MQTT subscribe callback: "+e.getMessage());
        }
    }

    //
    // private data members
    //
    private static final Logger log = Logger.getLogger( Provider.class.getName() );

    private InteractivePanel interactivePanel = null;    // the panel this provider is responsible for

    // MQTT topic keys in config file
    private static final String CONFIG_KEY_MQTT_TOPIC_TEMPERATURE     = "mqttTopicTemperature";     // configuration key for the MQTT topic to subscribe for temperature updates
    private static final String CONFIG_KEY_MQTT_TOPIC_TEMPERATURE_MIN = "mqttTopicTemperatureMin";  // configuration key for the MQTT topic to subscribe for min temperature updates
    private static final String CONFIG_KEY_MQTT_TOPIC_TEMPERATURE_MAX = "mqttTopicTemperatureMax";  // configuration key for the MQTT topic to subscribe for max temperature updates
    private static final String CONFIG_KEY_MQTT_TOPIC_TEMPERATURE2    = "mqttTopicTemperature2";    // configuration key for the MQTT topic to subscribe for temperature updates

    private String mqttTopicTemperature    = null;  // MQTT topic to subscribe for temperature updates
    private String mqttTopicTemperatureMin = null;  // MQTT topic to subscribe for min temperature updates
    private String mqttTopicTemperatureMax = null;  // MQTT topic to subscribe for max temperature updates
    private String mqttTopicTemperature2   = null;  // MQTT topic to subscribe for temperature updates
}
