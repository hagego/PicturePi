package picturepi;

import java.io.StringReader;
import java.time.LocalDate;
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
		
		log.info("creating TextWatchProvider object");
		
		// subscribe to MQTT topic to retrieve alarm list from alarm pi
		String alarmListTopic = Configuration.getConfiguration().getValue("TextWatchPanel", "mqttTopicAlarmlist", null);
		if(alarmListTopic != null) {
			log.info("subscribing for alarmlist");
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
				log.severe("Panel is not of class TextWatchPanel. Disabling updates.");
				
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
		log.fine("MQTT message arrived: topic="+topic+" content="+message);

		String alarmListTopic = Configuration.getConfiguration().getValue("TextWatchPanel", "mqttTopicAlarmlist", null); 
		if(alarmListTopic!=null && topic.equals(alarmListTopic)) {
			log.fine("parsing alarm data");
			
			JsonReader reader = Json.createReaderFactory(null).createReader(new StringReader(message.toString()));
			JsonObject jsonObject = reader.readObject();
			
			JsonArray alarmList = jsonObject.getJsonArray("alarms");
			
			// search if there is an alarm later today
			for(int i=0 ; i<alarmList.size() ; i++) {
				JsonObject alarm = alarmList.getJsonObject(i);
				boolean enabled  = alarm.getBoolean("enabled");
				boolean skipOnce = alarm.getBoolean("skipOnce");
				
				if(enabled && !skipOnce) {
					String alarmWeekDays = alarm.getJsonString("weekDays").toString();
					log.fine("found active alarm with weekDays: "+alarmWeekDays);
					
					// search for alarm scheduled for today or tomorrow
					LocalTime alarmTimeToday    = null;
					LocalTime alarmTimeTomorrow = null;
					String weekDayToday    = LocalDate.now().getDayOfWeek().toString();
					String weekDayTomorrow = LocalDate.now().plusDays(1).getDayOfWeek().toString();
					
					if(alarmWeekDays.contains(weekDayToday)) {
				    	String alarmTimeString = alarm.getString("time");
				    	log.fine("found active alarm for today with time="+alarmTimeString);
				    	
				    	LocalTime alarmTime = LocalTime.parse(alarmTimeString, DateTimeFormatter.ofPattern("HH:mm"));
				    	if(alarmTime.isAfter(LocalTime.now()) && (alarmTimeToday==null || alarmTime.isBefore(alarmTimeToday))) {
				    		alarmTimeToday = alarmTime;
				    	}
					}
					if(alarmWeekDays.contains(weekDayTomorrow)) {
				    	String alarmTimeString = alarm.getString("time");
				    	log.fine("found active alarm for tomorrow with time="+alarmTimeString);
				    	
				    	LocalTime alarmTime = LocalTime.parse(alarmTimeString, DateTimeFormatter.ofPattern("HH:mm"));
				    	if(alarmTimeTomorrow==null || alarmTime.isBefore(alarmTimeTomorrow)) {
				    		alarmTimeTomorrow = alarmTime;
				    	}
					}
					
					if(alarmTimeToday!=null) {
						log.fine("earliest alarm for today is set for "+alarmTimeToday);
						textWatchpanel.setAlarm("Wecker heute "+alarmTimeToday.format(DateTimeFormatter.ofPattern("HH:mm")));
					}
					else {
						log.fine("No alarm found for today");
						
						if(alarmTimeTomorrow!=null) {
							log.fine("earliest alarm for tomorrow is set for "+alarmTimeTomorrow);
							textWatchpanel.setAlarm("Wecker morgen "+alarmTimeTomorrow.format(DateTimeFormatter.ofPattern("HH:mm")));
						}
						else {
							textWatchpanel.setAlarm("Kein Wecker");
						}
					}
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
