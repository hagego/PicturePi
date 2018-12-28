package picturepi;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.swing.ImageIcon;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import tk.plogitech.darksky.forecast.APIKey;
import tk.plogitech.darksky.forecast.DarkSkyClient;
import tk.plogitech.darksky.forecast.ForecastException;
import tk.plogitech.darksky.forecast.ForecastRequest;
import tk.plogitech.darksky.forecast.ForecastRequestBuilder;
import tk.plogitech.darksky.forecast.ForecastRequestBuilder.Block;
import tk.plogitech.darksky.forecast.GeoCoordinates;
import tk.plogitech.darksky.forecast.model.Latitude;
import tk.plogitech.darksky.forecast.model.Longitude;

/**
 * Data Provider for Weather Panel
 */
public class WeatherProvider extends Provider implements IMqttMessageListener {

	WeatherProvider() {
		// update data every hour
		super(3600);
		
		// subscribe to MQTT topics to retrieve measured temperature updates
		mqttTopicTemperature = Configuration.getConfiguration().getValue(WeatherPanel.class.getSimpleName(), mqttTopicTemperatureConfigKey, null);
		if(mqttTopicTemperature != null) {
			log.info("subscribing for temperature");
			MqttClient.getMqttClient().subscribe(mqttTopicTemperature, this);
		}
		mqttTopicTemperatureMin = Configuration.getConfiguration().getValue(WeatherPanel.class.getSimpleName(), mqttTopicTemperatureMinConfigKey, null);
		if(mqttTopicTemperatureMin != null) {
			log.info("subscribing for min temperature");
			MqttClient.getMqttClient().subscribe(mqttTopicTemperatureMin, this);
		}
	}

	@Override
	protected void fetchData() {
		log.fine("fetchData() called");
		
		if(weatherPanel==null) {
			if(panel.getClass()==WeatherPanel.class) {
				log.fine("assigning weatherPanel object");
				weatherPanel = (WeatherPanel)panel;
			}
			else {
				log.severe("Panel is not of class WeatherPanel. Disabling updates.");
				return;
			}
		}
		
		// retrieve weather forecast from Dark Sky
		Double longitude = Configuration.getConfiguration().getValue(WeatherPanel.class.getSimpleName(), "longitude", 0.0);
		Double latitude  = Configuration.getConfiguration().getValue(WeatherPanel.class.getSimpleName(), "latitude", 0.0);
		if(longitude==0.0 || latitude==0.0) {
			log.warning("No longitude or latitude found in configuration file. No weather forecast reported.");
			return;
		}
		
		ForecastRequest request = new ForecastRequestBuilder()
		        .key(new APIKey(DARK_SKY_KEY))
		        .location(new GeoCoordinates(new Longitude(longitude), new Latitude(latitude)))
		        .exclude(Block.currently).exclude(Block.hourly).exclude(Block.flags)
		        .build();

		    DarkSkyClient client = new DarkSkyClient();
		    try {
				String forecast = client.forecastJsonString(request);
				log.fine("retrieved forecast: "+forecast);

				// extract data for daily forecast of 1st day (=today)
				JsonReader reader = Json.createReaderFactory(null).createReader(new StringReader(forecast));
				JsonObject jsonObject    = reader.readObject();
				JsonObject jsonDailyObject = jsonObject.getJsonObject("daily");
				if(jsonDailyObject==null) {
					log.severe("Unable to retrieve daily object from Dark Sky forecast");
					return;
				}
				JsonArray  jsonDailyArray  = jsonDailyObject.getJsonArray("data");
				if(jsonDailyArray==null || jsonDailyArray.size()<1) {
					log.severe("Unable to retrieve daily array from Dark Sky forecast");
					return;
				}
				
				JsonObject jsonDayForecast = jsonDailyArray.getJsonObject(0);
				log.fine("daily forecast: "+jsonDayForecast.toString());

				Date forecastDate = new Date((long)jsonDayForecast.getInt("time") * 1000L);
			    String forecastDateString = new SimpleDateFormat("dd-MM-yyyy").format(forecastDate);
			    log.fine("daily foreccast date: "+forecastDateString);
			    
			    String summary = new String(jsonDayForecast.getString("summary").getBytes("ISO-8859-1"), "UTF-8");
			    String iconName = jsonDayForecast.getString("icon");
			    log.fine("daily foreccast summary: "+summary+" icon="+iconName);
			    
			    double temperatureHigh = jsonDayForecast.getJsonNumber("temperatureHigh").doubleValue();
			    log.fine("daily foreccast temperature high: "+temperatureHigh);

			    ImageIcon icon = null;
			    try {
				    java.net.URL imageURL = this.getClass().getResource("weatherIcons/"+iconName+".png");
				    icon = new ImageIcon(imageURL);
			    }
			    catch(Exception e) {
			    	log.severe("Unable to load forecast icon: "+iconName);
			    }
			    
			    weatherPanel.setForecast(forecastDate, summary, temperatureHigh,icon);
			} catch (ForecastException | UnsupportedEncodingException e) {
				log.severe("Exception during forecast retrieval or parsing: "+e.getMessage());
			}
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("MQTT message arrived: topic="+topic+" content="+message);

		try {
			if(weatherPanel!=null && mqttTopicTemperature!=null && topic.equals(mqttTopicTemperature)) {
				log.fine("Updating actual temperature with "+message.toString());
				weatherPanel.setTemperature(Double.parseDouble(message.toString()));
			}
			if(weatherPanel != null && mqttTopicTemperatureMin!=null && topic.equals(mqttTopicTemperatureMin)) {
				log.fine("Updating min temperature with "+message.toString());
				weatherPanel.setTemperatureMin(Double.parseDouble(message.toString()));
			}
		}
		catch(Exception e) {
			log.severe("Exception in MQTT subscrib callback: "+e.getMessage());
		}
	}

	//
	// private members
	//
	private static final Logger   log     = Logger.getLogger( WeatherProvider.class.getName() );
	
	private final String            DARK_SKY_KEY                     = "0e5262d37aba4c40f7daa72705f4ca87";  // Dark Sky API Key
	private final String 			mqttTopicTemperatureConfigKey    = "mqttTopicTemperature";              // key in config file for MQTT topic for temperature
	private final String 			mqttTopicTemperatureMinConfigKey = "mqttTopicTemperatureMin";           // key in config file for MQTT topic of min. temperature
	private       String            mqttTopicTemperature;
	private       String            mqttTopicTemperatureMin;
	
	private       WeatherPanel      weatherPanel                  = null;    // WeatherPanel to update

}
