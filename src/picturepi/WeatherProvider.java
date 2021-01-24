package picturepi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.swing.ImageIcon;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import picturepi.Configuration.ViewData;

/**
 * Data Provider for Weather Panel
 */
public class WeatherProvider extends Provider implements IMqttMessageListener {

	WeatherProvider() {
		// update data every hour
		super(3600);
		log.fine("creating WeatherProvider");
		
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
		
		log.fine("WeatherProvider created");
	}
	
	/**
	 * builds the URL for OpenWeatherMap
	 * @return URL for OpenWeatherMap
	 */
	URL buildUrl() {
		final String BASE_URL  = "https://api.openweathermap.org/data/2.5/onecall";
		final String API_KEY   = "f593e17912b28437a5f95565670f8e2b";
		
		URL    url = null;
		Double longitude = Configuration.getConfiguration().getValue(WeatherPanel.class.getSimpleName(), "longitude", 0.0);
		Double latitude  = Configuration.getConfiguration().getValue(WeatherPanel.class.getSimpleName(), "latitude", 0.0);
		if(longitude==0.0 || latitude==0.0) {
			log.severe("No longitude or latitude found in configuration file. No weather forecast reported.");
			return null;
		}
		
		
		try {
			url = new URL(String.format("%s?lat=%f&lon=%f&units=metric&lang=de&exclude=current,minutely,hourly,alerts&appid=%s",BASE_URL,latitude,longitude,API_KEY));
		} catch (MalformedURLException e) {
			log.severe("Unable to create URL: MalformedURLException: "+e.getMessage());
		}
		
		return url;
	}
	
	/**
	 * returns the result of the HTTP GET query
	 * @param url complete URL
	 * @return HTTP GET query result as JSON object
	 */
	JsonObject getForecastAsJsonObject(URL url) {
		HttpURLConnection con = null;
		InputStream       is  = null;
		
		try {
        	// short-term (hourly) forecast
			String server = "api.openweathermap.org";
        	log.fine("URL hourly: "+url);
            con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            con.setRequestProperty("Accept","*/*");
            con.connect();
            log.fine( "connected" );

            // check for errors
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            	log.severe("http connection to openweathermap server "+server+" returned "+con.getResponseCode());
            	is = con.getErrorStream();
            	
            	byte b[] = new byte[200];
            	is.read(b);
            	log.severe("http error text: "+new String(b));
            	
            	is.close();
            }
            else {
	            // Let's read the response
	            is = con.getInputStream();
	            
	            JsonReader reader = Json.createReaderFactory(null).createReader(new InputStreamReader(is,StandardCharsets.UTF_8));
				JsonObject jsonObject = reader.readObject();
				log.finest("Forecast JSON object: "+jsonObject.toString());
				
				return jsonObject;
            }
		} catch (IOException e) {
			log.severe("IO Exception during HTTP retrieval: "+e.getMessage());
		}
        finally {
            try { is.close(); } catch(Throwable t) {}
        }

		return null;
	}
	
	boolean parseForecastFromJsonObject(JsonObject jsonObject) {
		log.fine("parsing forecast from JSON object");
		
		JsonArray  jsonDailyArray  = jsonObject.getJsonArray("daily");
		if(jsonDailyArray==null || jsonDailyArray.size()<1) {
			log.severe("Unable to retrieve daily array from forecast");
			return false;
		}
		
		JsonObject jsonDayForecast = jsonDailyArray.getJsonObject(0);
		log.finest("daily forecast: "+jsonDayForecast.toString());

		Integer dt;
		try {
			dt = jsonDayForecast.getInt("dt");
		}
		catch(NullPointerException e) {
			log.severe("parseForecastFromJsonObject: No date found");
	    	return false;
		}
		
		Date forecastDate = new Date((long)dt * 1000L);
	    String forecastDateString = new SimpleDateFormat("dd-MM-yyyy").format(forecastDate);
	    
	    JsonObject jsonObjectTemp = jsonDayForecast.getJsonObject("temp");
	    if(jsonObjectTemp==null) {
	    	log.severe("parseForecastFromJsonObject: No temperature found");
	    	return false;
	    }
	    
	    JsonNumber temperatureMin = jsonObjectTemp.getJsonNumber("min");
	    if(temperatureMin==null) {
	    	log.severe("parseForecastFromJsonObject: No min temperature found");
	    	return false;
	    }
	    
	    JsonNumber temperatureMax = jsonObjectTemp.getJsonNumber("max");
	    if(temperatureMax==null) {
	    	log.severe("parseForecastFromJsonObject: No max temperature found");
	    	return false;
	    }
	    
	    JsonArray weather = jsonDayForecast.getJsonArray("weather");
	    log.finest("size of weather array="+weather.size());
	    if(weather==null || weather.size()<1) {
	    	log.severe("parseForecastFromJsonObject: No weather array found");
	    	return false;
	    }
	    String iconString;
	    String description;
	    try {
	    	iconString  = weather.get(0).asJsonObject().getString("icon");
	    	description = weather.get(0).asJsonObject().getString("description");
	    } catch(NullPointerException | ClassCastException e) {
	    	log.severe("parseForecastFromJsonObject: No icon or description found");
	    	return false;
	    }
	    
	    log.fine(String.format("forecast date: %s temperature min: %f max: %f icon=%s", forecastDateString, temperatureMin.doubleValue(), temperatureMax.doubleValue(),iconString));
	    
	    // map icon string to filename
	    String iconFilename;
	    switch (iconString) {
	    	case "01d": iconFilename = "clear-day.png";break;
	    	case "02d": iconFilename = "partly-cloudy-day_dark.png";break;
	    	case "03d": iconFilename = "cloudy_dark.png";break;
	    	case "04d": iconFilename = "cloudy_dark.png";break;
	    	case "09d": iconFilename = "rain_dark.png";break;
	    	case "10d": iconFilename = "rain_dark.png";break;
	    	case "11d": iconFilename = "rain_dark.png";break;
	    	case "13d": iconFilename = "snow_dark.png";break;
	    	case "50d": iconFilename = "fog_dark.png";break;
	    	default:
	    		log.severe("parseForecastFromJsonObject: Unable to map icon string to filename: "+iconString);
	    		return false;
	    }
	    
	    ImageIcon icon = null;
		java.net.URL imageURL = this.getClass().getResource("weatherIcons/"+iconFilename);
		if(imageURL!=null) {
		    icon = new ImageIcon(imageURL);
		}
		
		if(weatherPanel!=null) {
			weatherPanel.setForecast(forecastDate, description, temperatureMax.doubleValue(),icon);
		}
		
	    return true;
	}
	
	@Override
	void init() {
		String followDynamicViewName = Configuration.getConfiguration().getValue(WeatherPanel.class.getSimpleName(), "followDynamicView", null);
		if(followDynamicViewName!=null) {
			log.fine("looking for view to follow with showDynamic: "+followDynamicViewName);
			List<ViewData> followViewDataList = Configuration.getConfiguration().getViewDataList().stream().filter(viewData -> viewData.name.equals(followDynamicViewName)).collect(Collectors.toList());
			if(followViewDataList.size()>0) {
				log.fine("found provider to follow view with showDynamic: "+followDynamicViewName);
				outsideScheduleProvider = followViewDataList.get(0).panel.provider;
			}
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
		
		// retrieve weather forecast from OpenWeatherMap
		URL url = buildUrl();
		if(url!=null) {
			JsonObject jsonObject = getForecastAsJsonObject(url);
			if(jsonObject!=null) {
				parseForecastFromJsonObject(jsonObject);
			}
			else {
				log.severe("fetchData: Forecast JSON object is null");
			}
		}
		else {
			log.severe("fetchData: url is null");
		}
	}
	
	@Override
	boolean hasOutsideScheduleData() {
		if(outsideScheduleProvider!=null) {
			return outsideScheduleProvider.hasOutsideScheduleData();
		}
		
		return super.hasOutsideScheduleData();
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
				// due to issues with openhab, the message contains also the unit "°C"
				int pos = message.toString().indexOf(' ');
				String s = pos>0 ? message.toString().substring(0, pos) : message.toString(); 
				weatherPanel.setTemperatureMin(Double.parseDouble(s));
			}
		}
		catch(Exception e) {
			log.severe("Exception in MQTT subscribe callback: "+e.getMessage());
		}
	}

	//
	// private members
	//
	private static final Logger   log     = Logger.getLogger( WeatherProvider.class.getName() );
	
	private final String 			mqttTopicTemperatureConfigKey    = "mqttTopicTemperature";              // key in config file for MQTT topic for temperature
	private final String 			mqttTopicTemperatureMinConfigKey = "mqttTopicTemperatureMin";           // key in config file for MQTT topic of min. temperature
	private       String            mqttTopicTemperature;
	private       String            mqttTopicTemperatureMin;
	
	private       WeatherPanel      weatherPanel                  = null;    // WeatherPanel to update
	private       Provider          outsideScheduleProvider       = null;    // optional, other provider used to decide to show outOfSchedule data
}
