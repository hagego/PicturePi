package picturepi;

import java.io.StringReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Data Provider for TextWatch Panel
 * Provides the current time as a text
 */
public class TextWatchProvider extends Provider implements IMqttMessageListener {

	TextWatchProvider() {
		// update data every second
		super(1);
		
		// subscribe to MQTT topic to retrieve alarm list from alarm pi
		String alarmListTopic = Configuration.getConfiguration().getValue("TextWatchPanel", "mqttTopicAlarmlist", null); //$NON-NLS-1$ //$NON-NLS-2$
		if(alarmListTopic != null) {
			log.info("subscribing for alarmlist"); //$NON-NLS-1$
			MqttClient.getMqttClient().subscribe(alarmListTopic, this);
		}
	}

	@Override
	protected void fetchData() {
		if(textWatchpanel==null) {
			if(panel.getClass()==TextWatchPanel.class) {
				textWatchpanel = (TextWatchPanel)panel;
			}
			else {
				log.severe("Panel is not of class TextWatchPanel. Disabling updates."); //$NON-NLS-1$
				
				return;
			}
		}
		
		LocalTime time = LocalTime.now();
		textWatchpanel.setTime(time.format(formatter));
		
		// calculate index into table with time strings. Threshold to switch is always
		// at 2m30s+n*5min
		int secondsSinceHour = time.getMinute()*60+time.getSecond();
		int index = 0;
		if( secondsSinceHour>150) {
			index = (secondsSinceHour-150)/300+1;
		}
		
		if(index!=lastIndex) {
			if(index >= timeMessagesLine1.length || index >= timeMessagesLine2.length) {
				log.severe("index into time messages out of bounds: "+index); //$NON-NLS-1$
				return;
			}
			
			String msg1 = timeMessagesLine1[index];
			String msg2 = timeMessagesLine2[index];
			
			int next = time.get(ChronoField.CLOCK_HOUR_OF_AMPM)+1;
			if(next==13) {
				next = 1;
			}
			msg1 = msg1.replace("%current", String.valueOf(time.get(ChronoField.CLOCK_HOUR_OF_AMPM))); //$NON-NLS-1$
			msg1 = msg1.replace("%next", String.valueOf(next)); //$NON-NLS-1$
			
			msg2 = msg2.replace("%current", String.valueOf(time.get(ChronoField.CLOCK_HOUR_OF_AMPM))); //$NON-NLS-1$
			msg2 = msg2.replace("%next", String.valueOf(next)); //$NON-NLS-1$
			
			textWatchpanel.setTimeText(msg1,msg2);
			lastIndex = index;
		}
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("MQTT message arrived: topic="+topic+" content="+message); //$NON-NLS-1$ //$NON-NLS-2$

		String alarmListTopic = Configuration.getConfiguration().getValue("TextWatchPanel", "mqttTopicAlarmlist", null); //$NON-NLS-1$ //$NON-NLS-2$
		if(alarmListTopic!=null && topic.equals(alarmListTopic)) {
			log.fine("parsing alarm data"); //$NON-NLS-1$
			
			JsonReader reader = Json.createReaderFactory(null).createReader(new StringReader(message.toString()));
			JsonObject jsonObject = reader.readObject();
			
			JsonArray alarmList = jsonObject.getJsonArray("alarms"); //$NON-NLS-1$
			for(int i=0 ; i<alarmList.size() ; i++) {
				JsonObject alarm = alarmList.getJsonObject(i);
				boolean enabled  = alarm.getBoolean("enabled"); //$NON-NLS-1$
				boolean skipOnce = alarm.getBoolean("skipOnce"); //$NON-NLS-1$
				
				if(enabled && !skipOnce) {
					String time = alarm.getString("time"); //$NON-NLS-1$
					
					log.fine("alarm time="+time); //$NON-NLS-1$
				}
			}
		}
	}

	//
	// private members
	//
	private static final Logger   log     = Logger.getLogger( TextWatchPanel.class.getName() );
	
	// time text messages
	private final String timeMessagesLine1[]   = {
			"kurz nach", //$NON-NLS-1$
			"5 Minuten nach", //$NON-NLS-1$
			"10 Minuten nach", //$NON-NLS-1$
			"viertel",  //$NON-NLS-1$
            "10 Minuten bis", //$NON-NLS-1$
            "5 Minuten bis", //$NON-NLS-1$
            "halb", //$NON-NLS-1$
            "5 Minuten nach", //$NON-NLS-1$
            "10 Minuten nach", //$NON-NLS-1$
            "dreiviertel", //$NON-NLS-1$
            "10 Minuten", //$NON-NLS-1$
            "5 Minuten", //$NON-NLS-1$
            "kurz vor" }; //$NON-NLS-1$
	
	private final String timeMessagesLine2[]   = {
			"%current", //$NON-NLS-1$
			"%current", //$NON-NLS-1$
			"%current", //$NON-NLS-1$
			"%next",  //$NON-NLS-1$
            "halb %next", //$NON-NLS-1$
            "halb %next", //$NON-NLS-1$
            "%next", //$NON-NLS-1$
            "halb %next", //$NON-NLS-1$
            "halb %next", //$NON-NLS-1$
            "%next", //$NON-NLS-1$
            "bis %next", //$NON-NLS-1$
            "bis %next", //$NON-NLS-1$
            "%next" }; //$NON-NLS-1$

	private       TextWatchPanel    textWatchpanel = null;    // TextWatchPanel to update
	private       int               lastIndex = -1;           // stores index of last message displayed
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss"); //$NON-NLS-1$

}
