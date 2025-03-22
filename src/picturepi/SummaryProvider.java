package picturepi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;

/**
 * Data provider for SummaryPanel.
 * SummaryPanel provides various information on a single panel:
 * - current time
 * - traffic information to a specified destination
 * - Garbage Collection of the day
 */
public class SummaryProvider extends Provider implements IMqttMessageListener {
	
	// nested class used as a structure to store route information
	class RouteInformation {
		class Location {
			double latitude;
			double longitude;
		};
		
		// input data into TomTom
		String name;              // route name
		Location start;           // start location
		Location end;             // end location
		List<Location> waypoints; // optional waypoints
		
		// result data after TomTom query
		int travelTimeNoTraffic;    // ideal duration in s
		int travelTimeActual;       // actual duration
		int travelTimeAlternative;  // optional duration of an alternative route
		
		public RouteInformation() {
			start     = new Location();
			end       = new Location();
			waypoints = new LinkedList<Location>();
		}
	}
	
	
	// use a hard-coded refresh interval 
	public SummaryProvider() {
		super(refreshInterval);
		
		// get information about route 1 from configuration file
		routeInformation1 = getRouteInformation(1);
		routeInformation2 = getRouteInformation(2);

		// subscribe to MQTT topics to retrieve measured temperature updates
		mqttTopicTemperature = Configuration.getConfiguration().getValue(SummaryPanel.class.getSimpleName(), mqttTopicTemperatureConfigKey, null);
		if(mqttTopicTemperature != null) {
			log.info("subscribing for temperature, MQTT key="+mqttTopicTemperature);
			MqttClient.getMqttClient().subscribe(mqttTopicTemperature, this);
		}
	}

	@Override
	void fetchData() {
		// execute once only
		if(myPanel==null) {
			if(panel.getClass()==SummaryPanel.class) {
				myPanel = (SummaryPanel)panel;
				
				if(routeInformation1!=null) {
					myPanel.setRoute1DataStatic(routeInformation1.name);
				}
				if(routeInformation2!=null) {
					myPanel.setRoute2DataStatic(routeInformation2.name);
				}
			}
			else {
				log.severe("Panel is not of class SummaryPanel. Disabling updates.");
				
				return;
			}
		}

		// tasks done only oncer per day
		if(lastFetchDate==null || lastFetchDate.compareTo(LocalDate.now())!=0) {
			lastFetchDate = LocalDate.now();
			
			log.fine("fetching data for new day");
			// caldendar entries
			String calendarName = Configuration.getConfiguration().getValue("SummaryPanel", "googleCalendarName", null);
			if(calendarName!=null) {
				GoogleCalendar googleCalendar = new GoogleCalendar();
				if( googleCalendar.connect() == false) {
					// connect failed
					log.severe("Connection to Google Calendar failed");
					googleCalendar = null;
				}
				else {
					List<String> calendarEntries = googleCalendar.getCalendarEntriesForToday(calendarName);
					if(calendarEntries!=null && calendarEntries.size()>0) {
						myPanel.setCalendarEntries(String.join(",", calendarEntries));
					}
					else {
						myPanel.setCalendarEntries("--");
					}
				}
			}
		}
		
		// refresh date/time
		myPanel.setDateTime(
				LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy").withLocale(Locale.GERMANY)),
				LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) );
		
		// refresh route traffic information
		if(routeRefreshCounter==0) {
			if(routeInformation1!=null) {
				fetchTrafficInformation(routeInformation1);
				
				if(routeInformation1.travelTimeAlternative>0) {
					myPanel.setRoute1DataDynamic(String.format("aktuell %d min (ideal %d, alt %d min)",
							routeInformation1.travelTimeActual/60,
							routeInformation1.travelTimeNoTraffic/60,
							routeInformation1.travelTimeAlternative/60));
				}
				else {
					myPanel.setRoute1DataDynamic(String.format("aktuell %d min (ideal %d min)",
							routeInformation1.travelTimeActual/60,
							routeInformation1.travelTimeNoTraffic/60));
				}
			}
			
			if(routeInformation2!=null) {
				fetchTrafficInformation(routeInformation2);

				if(routeInformation2.travelTimeAlternative>0) {
					myPanel.setRoute2DataDynamic(String.format("aktuell %d min (ideal %d, alt %d min)",
							routeInformation2.travelTimeActual/60,
							routeInformation2.travelTimeNoTraffic/60,
							routeInformation2.travelTimeAlternative/60));
				}
				else {
					myPanel.setRoute2DataDynamic(String.format("aktuell %d min (ideal %d min)",
							routeInformation2.travelTimeActual/60,
							routeInformation2.travelTimeNoTraffic/60));
				}
			}
		}
		routeRefreshCounter++;
		if(routeRefreshCounter>routeRefreshDivider) {
			routeRefreshCounter=0;
		}
	}
	
	/**
	 * fetches information about a specified route from TomTom
	 * @param routeInformation route to fetch data for
	 * @return true in case of success, false in case of error
	 */
	boolean fetchTrafficInformation(RouteInformation routeInformation) {
		log.fine("fetching traffic information for route "+routeInformation.name);
		
		String key = getTomTomApiKey();
		if(key==null) {
			log.severe("No TomTom API Key found");
			return false;
		}
	
		// building JSON input string
		JsonArrayBuilder supportingPointsArrayBuilder = Json.createArrayBuilder();
		
		supportingPointsArrayBuilder.add(Json.createObjectBuilder()
				.add("latitude", routeInformation.start.latitude)
				.add("longitude",routeInformation.start.longitude));
		
		for(RouteInformation.Location location:routeInformation.waypoints) {
			supportingPointsArrayBuilder.add(Json.createObjectBuilder()
					.add("latitude", location.latitude)
					.add("longitude", location.longitude));
		}
		
		supportingPointsArrayBuilder.add(Json.createObjectBuilder()
				.add("latitude", routeInformation.end.latitude)
				.add("longitude",routeInformation.end.longitude));
		
		JsonObject supportingPoints = Json.createObjectBuilder().add("supportingPoints", supportingPointsArrayBuilder).build();
		
		log.finest("Json input data for POST query: "+supportingPoints.toString());
		JsonObject queryResult = executeHttpPostJsonQuery(buildUrl(routeInformation, key), supportingPoints.toString());
		if(queryResult==null) {
			log.severe("TomTom query failed");
			return false;
		}
		
		JsonArray jsonRoutesArray = queryResult.getJsonArray("routes");
		if(jsonRoutesArray==null || jsonRoutesArray.size()<1) {
			log.severe("No valid routes array found in Json query results");
			return false;
		}
		
		if(jsonRoutesArray.size()>1) {
			RouteInformation routeInformationAlternative = new RouteInformation();
			if(parseJsonRouteData(jsonRoutesArray.getJsonObject(1), routeInformationAlternative)==true) {
				routeInformation.travelTimeAlternative = routeInformationAlternative.travelTimeActual;
			}
		}
		else {
			routeInformation.travelTimeAlternative = 0;
		}

		return parseJsonRouteData(jsonRoutesArray.getJsonObject(0), routeInformation);
	}
	
	/**
	 * parses a Json route object and populates the output values in the RouteInformation object
	 * @param jsonObjectRoute   Json route object to parse
	 * @param routeInformation  RouteINformation object to store the result values
	 * @return true in case of success, false in case of error
	 */
	boolean parseJsonRouteData(JsonObject jsonObjectRoute,RouteInformation routeInformation) {
		JsonObject jsonObjectSummary = jsonObjectRoute.getJsonObject("summary");
		if(jsonObjectSummary==null) {
			log.severe("no summary object found in route object");
			return false;
		}
		
		JsonNumber jsonTravelTimeInSeconds = jsonObjectSummary.getJsonNumber("travelTimeInSeconds");
		if(jsonTravelTimeInSeconds==null) {
			log.severe("no travelTimeInSeconds found in route summary object");
			return false;
		}
		routeInformation.travelTimeActual = jsonTravelTimeInSeconds.intValue();
		
		JsonNumber jsonNoTrafficTravelTimeInSeconds = jsonObjectSummary.getJsonNumber("noTrafficTravelTimeInSeconds");
		if(jsonNoTrafficTravelTimeInSeconds==null) {
			log.severe("no noTrafficTravelTimeInSeconds found in route summary object");
			return false;
		}
		routeInformation.travelTimeNoTraffic = jsonNoTrafficTravelTimeInSeconds.intValue();
		
		return true;
	}
	
	/**
	 * returns the TomTom API Key
	 * @return The TomTom API Key
	 */
	String getTomTomApiKey() {
		// Precedence: Read from config file
		String apiKey = Configuration.getConfiguration().getValue("SummaryPanel", "tomTomApiKey", null);
		
		if(apiKey==null) {
			log.warning("No API Key found in configuration file. Trying environment variable as fallback");
			apiKey = System.getenv("PICTUREPI_TOMTOMAPIKEY");
			
			if(apiKey==null) {
				log.severe("No TomTom API Key found");
			}
		}
		
		return apiKey;
	}
	
	
	/**
	 * splits a location string in the format latitude:longitude
	 * @param location   input, location as string in the format latitude:longitude 
	 * @param latitude   output, latitude
	 * @param longitude  output, longitude
	 * @return true in case of success, false in case of an error
	 */
	boolean splitLocation(String locationString,RouteInformation.Location location) {
		String elements[] = locationString.split(",");
		if(elements.length!=2) {
			log.severe(String.format("Invalid number %d of elements in location string %s",elements.length,location));
			return false;
		}
		
		try {
			location.latitude  = Double.parseDouble(elements[0]);
			location.longitude = Double.parseDouble(elements[1]);
		}
		catch(NumberFormatException e) {
			log.severe("Exception during location string parsing: "+e.getMessage());
			return false;
		}
		
		return true;
	}
	
	/**
	 * Reads information about a route from the configuration file
	 * @param route route ID (in the range of 1...9)
	 * @return a populated RouteInformation object of null in case of any error
	 */
	RouteInformation getRouteInformation(int route) {
		log.fine(String.format("getting information about route ID %d", route));
		
		Configuration configuration = Configuration.getConfiguration();
		RouteInformation routeInformation = new RouteInformation();
		
		if(route<1 || route>9) {
			log.severe(String.format("route ID %d outside of valid range (1...9",route));
			return null;
		}
		
		// config file key name prefix
		String keyPrefix = String.format("route%d", route);
		
		// read route name
		routeInformation.name = configuration.getValue(CONFIG_SECTION, keyPrefix+"Name", null);
		if(routeInformation.name==null) {
			log.severe(String.format("invalid route name for route ID %d: %s",route,routeInformation.name));
			return null;
		}
		
		// read route start location
		String start = configuration.getValue(CONFIG_SECTION, keyPrefix+"Start", null);
		if(start==null) {
			log.severe(String.format("invalid route start for route ID %d: %s",route,start));
			return null;
		}
		
		if(splitLocation(start, routeInformation.start)==false) {
			return null;
		}
		
		// read route end location
		String end = configuration.getValue(CONFIG_SECTION, keyPrefix+"End", null);
		if(end ==null) {
			log.severe(String.format("invalid route end for route ID %d: %s",route,end));
			return null;
		}
		
		if(splitLocation(end , routeInformation.end)==false) {
			return null;
		}
		
		// read (optional) waypoints
		boolean continueWithWaypoints = true;
		int waypointIndex = 1;
		while(continueWithWaypoints) {
			String waypointKey   = String.format("%sWaypoint%d", keyPrefix,waypointIndex);
			String waypointValue = configuration.getValue(CONFIG_SECTION, waypointKey, null);
			if(waypointValue==null) {
				continueWithWaypoints = false;
			}
			else {
				RouteInformation.Location waypoint = routeInformation.new Location();
				if(splitLocation(waypointValue,waypoint)==false) {
					continueWithWaypoints = false;
					return null;
				}
				else {
					routeInformation.waypoints.add(waypoint);
					waypointIndex++;
				}
			}
		}
		
		log.fine(String.format("successfully retrieved information about route ID %d (name:%s,waypoints:%d)", route,routeInformation.name,routeInformation.waypoints.size()));
		return routeInformation;
	}
	
	/**
	 * builds the API URL
	 * @param  routeInformation  RouteInformation object with route parameters
	 * @param  key               TomTOmn API key
	 * @return URL or null in case of an error
	 */
	String buildUrl(RouteInformation routeInformation,String key) {
		String url = null;
		
		// get routing start/end locations
		url = String.format(Locale.US,"%s/%f%%2C%f%%3A%f%%2C%f/json?", URL_BASE,
				routeInformation.start.latitude,routeInformation.start.longitude,
				routeInformation.end.latitude,routeInformation.end.longitude);

		for(Entry<String,String> entry : apiOptionsMap.entrySet()) {
			url += entry.getKey()+"="+entry.getValue()+"&";
		}

		url += "key="+key;
		
		log.fine("final URL="+url);
		
		return url;
	}
	
	/**
	 * Executes an HTTP POST query, returning the result as JSON object
	 * @param urlString         URL
	 * @param inputString       input string for the query
	 * @return                  query result as JSON object or null in case of error
	 */
	JsonObject executeHttpPostJsonQuery(String urlString, String inputString) {
		log.fine("executing HTTP Post query for URL="+urlString);
		
		try {
			URL url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setDoInput(true);
			con.setDoOutput(true);
			
			try(OutputStream os = con.getOutputStream()) {
				byte[] input = inputString.getBytes("UTF-8");
			    os.write(input, 0, input.length);           
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
		    StringBuilder response = new StringBuilder();
		    String responseLine = null;
		    while ((responseLine = br.readLine()) != null) {
		        response.append(responseLine.trim());
		    }
			JsonReader reader = Json.createReaderFactory(null).createReader(new StringReader(response.toString()));
			JsonObject jsonObject    = reader.readObject();
			
			log.finest("Json response: "+jsonObject);
			
			return jsonObject;
		} catch (MalformedURLException e) {
			log.severe("malformed URL Exception");
			
			return null;
		} catch (IOException e) {
			log.severe("IOException: "+e.getMessage());
			
			return null;
		}
	}

	@Override
	public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) throws Exception {
		log.fine("message arrived on topic "+topic);
		
		if(mqttTopicTemperature!=null && topic.equals(mqttTopicTemperature)) {
			log.fine("message is temperature update, content="+message.toString());
			myPanel.setTemperature(Double.parseDouble(message.toString()));
			log.fine("temperature set to "+message.toString());
		}
	}

	
	//
	// private data
	//
	private SummaryPanel    myPanel = null;    // associated panel to update
	
	private final Logger    log = Logger.getLogger( this.getClass().getName() );
	
	// section name in configuration file
	private final String    CONFIG_SECTION = "SummaryPanel";

	private final String	mqttTopicTemperatureConfigKey    = "mqttTopicTemperature";      // key in config file for MQTT topic for temperature

	private       String            mqttTopicTemperature;  									// MQTT topic for temperature updates
	
	// use a hard-coded refresh interval
	private final static int refreshInterval = 60; // in seconds
	
	// date of last fetch operation
	private LocalDate lastFetchDate = null;

	// refresh divider for route data refresh from TomTom
	private final static int routeRefreshDivider = 5;
	private int routeRefreshCounter = 0;
	
	// base URL for Tom Tom API
	private final static String URL_BASE = "https://api.tomtom.com/routing/1/calculateRoute";
	
	// Tom Tom API options
	private final static Map<String, String> apiOptionsMap = Map.ofEntries(
			  new AbstractMap.SimpleEntry<String, String>("maxAlternatives", "1"),
			  new AbstractMap.SimpleEntry<String, String>("alternativeType", "betterRoute"),
			  new AbstractMap.SimpleEntry<String, String>("instructionsType", "coded"),
			  new AbstractMap.SimpleEntry<String, String>("routeRepresentation", "summaryOnly"),
			  new AbstractMap.SimpleEntry<String, String>("computeTravelTimeFor", "all"),
			  new AbstractMap.SimpleEntry<String, String>("travelMode", "car")
			);
	
	// information about routes
	private RouteInformation routeInformation1;
	private RouteInformation routeInformation2;
}

