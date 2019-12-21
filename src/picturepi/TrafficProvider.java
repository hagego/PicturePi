package picturepi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.ini4j.Ini;

/**
 * Provider to get data about traffic times from GoogleDirectionApi
 */
public class TrafficProvider extends Provider {
	
	public TrafficProvider(Ini ini) {
		// update data every 10 minutes
		super(600);
		
		// read configuration data
		readConfig(ini);
	}

	
	@Override
	protected void fetchData() {
		log.fine("MapsDirectionProvider fetchData called");
		
		// get travel times from Google destination API
		// https://maps.googleapis.com/maps/api/directions/json?origin=N%C3%BCrtingen&destination=B%C3%B6blingen&alternatives=true&key=AIzaSyBOVIAo060JB81w5jUhrf5_SbvVRvZIw2c
		for(Route route:routeList) {
			route.alternativeList.clear();
			
			try {
				URL url = new URL(BASE_URL+"origin="+route.start.replace(" ", "%20")+"&destination="+route.destination.replace(" ", "%20")+"&alternatives=true&key="+API_KEY);
				log.fine("URL="+url);
			    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				JsonReader reader = Json.createReader(in);
				JsonObject rootObject = reader.readObject();
				String status = rootObject.getString("status");
				JsonArray routes = rootObject.getJsonArray("routes");
				
				log.fine("retrieving data for route "+route.description+" returned status "+status);
				
				for(int i=0 ; i<routes.size(); i++) {
					Route.Alternative alternative = route.new Alternative();
					
					JsonObject routeData = routes.getJsonObject(i);
					alternative.summary = routeData.getString("summary");
					
					JsonArray legs = routeData.getJsonArray("legs");
					JsonObject leg = legs.getJsonObject(0);
					
					alternative.duration = leg.getJsonObject("duration").getString("text");
					
					log.fine("retrieved summary="+alternative.summary+" duration="+alternative.duration);
					route.alternativeList.add(alternative);
				}
			} catch (IOException e) {
				log.severe("Unable to retrieve data for route "+route.description);
				log.severe(e.getMessage());
				
				route.alternativeList.clear();
			}	
		}
		
		// get traffic camera pictures using wget
		 Runtime r = Runtime.getRuntime();
		 try {
			Process p = r.exec(WGET_CMD,null,new File("\\tmp"));
			
			if(p.waitFor() == 0) {
				
			}
			else {
				log.severe("Retrieval of traffic camera picture with wget returns !=0");
			}
		} catch (IOException | InterruptedException e) {
			log.severe("Exception while trying to retrieve traffic camera pictures with wget:");
			log.severe(e.getMessage());
		}
		
		// wget --no-cache -nd -p -N -A jpg --user-agent="Mozilla/5.0 (X11; U; Linux i686; de; rv:1.9b5) Gecko/2008050509 Firefox/3.0b5" http://www.svz-bw.de/kamera.html?webcamid=S202

		// refresh panel display
		if(panel instanceof TrafficPanel) {
			((TrafficPanel) panel).refresh(routeList);
		}
		else {
			log.severe("panel is not of type TrafficPanel");
		}
		
	}
	
	/*
	 * reads configuration data from .ini file
	 * @param ini ini4j ini file object
	 */
	private void readConfig(Ini ini) {
		// read route data
		int index = 0;
		log.config("reading route data");
			
		Ini.Section routes= ini.get("routes");
			
		if(routes!=null) {
			log.fine("found routes section");
			routeList.clear();

			try {
				while(true) {
					Route route = new Route();
					route.description = routes.get("description",index);
					route.start       = routes.get("start",index);
					route.destination = routes.get("destination",index);
					log.config("found route data: description="+route.description+" start="+route.start+" destination="+route.destination);
					routeList.add(route);
					
					index++;
				}
			}
			catch (IndexOutOfBoundsException e) {}
		}
		else {
			log.severe("No routes section found in ini file");
		}
	}
	
	//
	// private data members
	//
	private static final Logger log      = Logger.getLogger( TrafficProvider.class.getName() );
	private static final String API_KEY  = "AIzaSyBOVIAo060JB81w5jUhrf5_SbvVRvZIw2c";  // Key for Google directions API
	private static final String BASE_URL = "https://maps.googleapis.com/maps/api/directions/json?";
	private static final String WGET_CMD = "wget --no-cache -nd -p -N -A jpg --user-agent=\"Mozilla/5.0 (X11; U; Linux i686; de; rv:1.9b5) Gecko/2008050509 Firefox/3.0b5\" http://www.svz-bw.de/kamera.html?webcamid=S202";
	
    // local class to store route data
	class Route {
		String description;
		String start;
		String destination;
		
		class Alternative {
			String summary;
			String duration;
		}
		List<Alternative> alternativeList = new LinkedList<Alternative>();
	}
	private List<Route> routeList = new LinkedList<Route>();
	
	
}
