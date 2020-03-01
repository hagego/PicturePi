package picturepi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

/**
 * Provides the duration (by car) between to locations using TomToms routing API 
 */
public class TomTomTrafficProvider extends Provider {

	public TomTomTrafficProvider() {
		super(Configuration.getConfiguration().getValue(TomTomTrafficPanel.class.getSimpleName(), "refreshInterval", 300));
	}

	@Override
	void fetchData() {
		String url = buildUrl();
		if(url==null) {
			return;
		}
		
		JsonObject jsonRoutingData=getRoutingData(url);
		if(jsonRoutingData==null) {
			return;
		}
		
		RouteData routeData = parseRoutingData(jsonRoutingData);
		if(routeData==null) {
			return;
		}
		
		TomTomTrafficPanel trafficPanel = (TomTomTrafficPanel)panel;
		trafficPanel.setData(routeData);
	}
	
	/**
	 * returns the TomTom API Key
	 * @return The TomTom API Key
	 */
	String getApiKey() {
		// Precedence: Read from config file
		String apiKey = Configuration.getConfiguration().getValue("TomTomTrafficPanel", "apiKey", null);
		
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
	 * builds the API URL
	 * @return URL
	 */
	String buildUrl() {
		String url = null;
		
		// get routing start/end locations
		String locations = Configuration.getConfiguration().getValue("TomTomTrafficPanel", "locations", null);
		if(locations==null) {
			log.severe("no start/end locations found");
			
			return null;
		}
		log.info("Start/End locations: "+locations);
		locations = locations.replace(",","%2C");
		locations = locations.replace(":","%3A");
		
		String key = getApiKey();
		if(key!=null) {
			url = URL+locations+JSON_PARAMS+"&key="+key;
		}
		
		log.fine("final URL="+url);
		
		return url;
	}
	
	/**
	 * retreives the routing information as JSON object from the TomTom server 
	 * @param urlString TomTom server URL
	 * @return          JsonObject with response data if successful, otherwise null
	 */
	JsonObject getRoutingData(String urlString) {
		log.fine("Getting routing data");
		
		try {
			URL url = new URL (urlString);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			
			BufferedReader br = new BufferedReader(
			  new InputStreamReader(con.getInputStream(), "utf-8"));
			    StringBuilder response = new StringBuilder();
			    String responseLine = null;
			    while ((responseLine = br.readLine()) != null) {
			        response.append(responseLine.trim());
			    }
			JsonReader reader = Json.createReaderFactory(null).createReader(new StringReader(response.toString()));
			JsonObject jsonObject    = reader.readObject();
			
			log.fine("Json response: "+jsonObject);
			
			return jsonObject;
		} catch (MalformedURLException e) {
			log.severe("malformed URL Exception");
			
			return null;
		} catch (IOException e) {
			log.severe("IOException: "+e.getMessage());
			
			return null;
		}
	}
	
	/**
	 * parses the "guidance" JSON object
	 * @param   guidance JSON object
	 * @return  list of instructions
	 */
	List<String> parseGuidance(JsonObject guidance) {
		List<String> instructionList = new LinkedList<String>();
		
		try {
			JsonArray instructionGroups = guidance.getJsonArray("instructionGroups");
			if(instructionGroups!=null) {
				for(JsonValue instruction:instructionGroups) {
					String message = instruction.asJsonObject().getString("groupMessage");
					if(message!=null) {
						instructionList.add(message);
						log.finest("found group message: "+message);
					}
					else {
						log.severe("Invalid Json response in parseGuidance, groupMessage. guidance="+guidance);
					}
				}
			}
		}
		catch(ClassCastException e) {
			log.severe("Invalid Json response in parseGuidance. guidance="+guidance.toString());
			log.severe(e.getMessage());
		}
		
		log.fine("parseGuidance: Found "+instructionList.size()+" instructions");
		
		return instructionList;
	}
	
	/**
	 * Parses the JSON data of a single route
	 * @param jsonRouteObject  JSON route object
	 * @return                 route data as RouteData object or null in case of an error
	 */
	RouteData parseRouteData(JsonObject jsonRouteObject) {
		RouteData routeData = new RouteData();
		
		JsonObject jsonSummary = jsonRouteObject.getJsonObject("summary");
		if(jsonSummary==null) {
			log.severe("invalid JSON data: no summary data found in "+jsonRouteObject.toString());
			return null;
		}
		
		JsonNumber jsonLength = jsonSummary.getJsonNumber("lengthInMeters");
		if(jsonLength==null) {
			log.severe("invalid JSON data: no lengthInMeters data found in "+jsonRouteObject.toString());
			return null;
		}
		routeData.length = jsonLength.intValue();
		
		JsonNumber jsonDuration = jsonSummary.getJsonNumber("travelTimeInSeconds");
		if(jsonDuration==null) {
			log.severe("invalid JSON data: no travelTimeInSeconds data found in "+jsonRouteObject.toString());
			return null;
		}
		routeData.duration = jsonDuration.intValue();
		
		JsonNumber jsonDelay = jsonSummary.getJsonNumber("trafficDelayInSeconds");
		if(jsonDelay==null) {
			log.severe("invalid JSON data: no trafficDelayInSeconds data found in "+jsonRouteObject.toString());
			return null;
		}
		routeData.delay = jsonDelay.intValue();
		
		JsonObject jsonGuidance = jsonRouteObject.getJsonObject("guidance");
		if(jsonGuidance==null) {
			log.severe("invalid JSON data: no guidance data found in "+jsonRouteObject.toString());
			return null;
		}
		routeData.instructions = parseGuidance(jsonGuidance);
		
		log.fine("parsed route data: length="+routeData.length+" duration="+routeData.duration+" delay="+routeData.delay+ " instruction count="+routeData.instructions.size());
		
		return routeData;
	}
	
	/**
	 * parses the complete routing data response object
	 * @param   jsonRoutingObject
	 * @return  the 1st route as RouteData object or null in case of an error
	 */
	RouteData parseRoutingData(JsonObject jsonRoutingObject) {
		JsonArray jsonRouteArray = jsonRoutingObject.getJsonArray("routes");
		if(jsonRouteArray==null || jsonRouteArray.size()<1) {
			log.severe("invalid JSON data: no routes array found.");
			return null;
		}
		
		JsonObject jsonRoute = jsonRouteArray.getJsonObject(0);
		if(jsonRoute==null) {
			log.severe("invalid JSON data: no route data found in "+jsonRouteArray.toString());
			return null;
		}
		
		return parseRouteData(jsonRoute);
	}
	
	//
	// private data
	//
	private final Logger    log = Logger.getLogger( this.getClass().getName() );
	
	// nested class to store route data
	class RouteData {
		int          length;        // route length in meter
		int          duration;      // travel duration in seconds
		int          delay;         // traffic delay in seconds
		List<String> instructions;  // list of instructions
	}

	// TomTom routing API base URL
	final String URL         = "https://api.tomtom.com/routing/1/calculateRoute/";
	
	// parameter string
	final String JSON_PARAMS = "/json?maxAlternatives=1&instructionsType=text&language=de&routeRepresentation=summaryOnly&computeTravelTimeFor=all&departAt=now&routeType=fastest&traffic=true&avoid=unpavedRoads&travelMode=car";
}
