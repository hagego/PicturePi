package picturepi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Data provider for the status of a Renault Zoe electrical car.
 * Implemented based on https://github.com/edent/Renault-Zoe-API
 */
public class RenaultZoeStatusProvider extends Provider {

	RenaultZoeStatusProvider() {
		super(3600);
	}
	
	@Override
	public void run() {
		// override run to do nothing
		// as this provide is only for interactive (on explicit request) usage
	}
	
	
	/**
	 * log into the Renault ZE Service webpage to retrieve data for further steps 
	 * @param user      user
	 * @param password  password
	 * @return          JsonObject with response data if successful, otherwise null
	 */
	JsonObject login(String user, String password) {
		log.config("logging in to Renault ZE Services webpage as user "+user);
		
		try {
			URL url = new URL ("https://www.services.renault-ze.com/api/user/login");
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			
			String jsonInputString = "{\"username\": \""+user+"\", \"password\": \""+password+"\"}";
			try(OutputStream os = con.getOutputStream()) {
			    byte[] input = jsonInputString.getBytes("utf-8");
			    os.write(input, 0, input.length);           
			}
			
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
	 * parse the login response
	 * @param loginResponse  JSON object retrieved from login
	 * @return               true on success, false on error
	 */
	boolean parseLoginResponse(JsonObject loginResponse) {
		log.fine("parsing login response");
		
		token = null;
		vin   = null;
		
		if(loginResponse!=null) {
			JsonString jsonStringToken = loginResponse.getJsonString("token");
			if(jsonStringToken!=null) {
				token = jsonStringToken.getString();
				log.fine("found token: "+token);
			}
			JsonObject jsonObjectUser = loginResponse.getJsonObject("user");
			if(jsonObjectUser!=null) {
				JsonObject jsonObjectVehicleDetails = jsonObjectUser.getJsonObject("vehicle_details");
				if(jsonObjectVehicleDetails!=null) {
					JsonString jsonStringVin = jsonObjectVehicleDetails.getJsonString("VIN");
					if(jsonStringVin!=null) {
						vin = jsonStringVin.getString();
						log.fine("found VIN: "+vin);
					}
				}
			}
		}
		
		if(token!=null && vin!=null) {
			log.fine("login response successfully parsed");
			
			return true;
		}
		else {
			log.warning("login response could not be parsed");
			
			return false;
		}
	}

	/**
	 * retrieves the battery status
	 * @param  token
	 * @param  vin
	 * @return response as JSON object of null in case of error
	 */
	JsonObject getBatteryStatus(String token, String vin) {
		log.fine("getting battery status");
		
		try {
			URL url = new URL ("https://www.services.renault-ze.com/api/vehicle/"+vin+"/battery");
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			//con.setRequestMethod("POST");
			con.setRequestProperty("Authorization", "Bearer "+token);
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
	 * parses the response of a getBatteryStatus request
	 * @param batteryStatusResponse Json response object
	 * @return                      true if successful, false on error
	 */
	boolean parseBatteryStatusResponse(JsonObject batteryStatusResponse) {
		log.fine("parsing battery status response");

		isPlugged   = null;
		isCharging  = null;
		chargeLevel = null;
		
		if(batteryStatusResponse!=null) {
			JsonValue jsonValuePlugged = batteryStatusResponse.get("plugged");
			if(jsonValuePlugged!=null) {
				isPlugged = jsonValuePlugged==JsonValue.TRUE ? true : false;
				log.fine("plugger="+isPlugged);
			}
			
			JsonValue jsonValueCharging = batteryStatusResponse.get("charging");
			if(jsonValueCharging!=null) {
				isCharging = jsonValueCharging==JsonValue.TRUE ? true : false;
				log.fine("charging="+isCharging);
			}
			JsonNumber jsonValueChargeLevel = batteryStatusResponse.getJsonNumber("charge_level");
			if(jsonValueChargeLevel!=null) {
				chargeLevel = jsonValueChargeLevel.intValue();
				log.fine("charge level="+chargeLevel);
			}
		}
		
		if(isPlugged!=null && isCharging!=null && chargeLevel!=null) {
			log.fine("battery status response successfully parsed");
			
			return true;
		}
		else {
			log.fine("battery status response could not be parsed");
			
			return false;
		}
	}
	
	/**
	 * triggers the air conditioning
	 * @param  token
	 * @param  vin
	 * @return true on success, false on error
	 */
	boolean triggerAirConditioning(String token, String vin) {
		log.config("triggering air conditioning");
		
		try {
			URL url = new URL ("https://www.services.renault-ze.com/api/vehicle/"+vin+"/air-conditioning");
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Authorization", "Bearer "+token);
			//con.setRequestProperty("Accept", "application/json");
			//con.setDoOutput(true);
			
			return true;
		}
		catch (MalformedURLException e) {
			log.severe("malformed URL Exception");
		
			return false;
		} catch (IOException e) {
			log.severe("IOException: "+e.getMessage());
		
			return false;
		}
	}

	@Override
	protected void fetchData() {
		log.fine("fetchData() called");
		
		if(renaultZoeStatusPanel==null) {
			if(panel.getClass()==RenaultZoeStatusPanel.class) {
				renaultZoeStatusPanel = (RenaultZoeStatusPanel)panel;
			}
			else {
				log.severe("Panel is not of class RenaultZoeStatusPanel. Disabling updates.");
				
				return;
			}
		}
		
		String user       = Configuration.getConfiguration().getValue(renaultZoeStatusPanel.getClass().getSimpleName(), "user", "");
		String password   = Configuration.getConfiguration().getValue(renaultZoeStatusPanel.getClass().getSimpleName(), "password", "");
		boolean triggerAC = Configuration.getConfiguration().getValue(renaultZoeStatusPanel.getClass().getSimpleName(), "triggerAirConditioning", false);
		
		JsonObject loginResponse = login(user, password);
		if( loginResponse!=null && parseLoginResponse(loginResponse) ) {
			JsonObject batteryStatusResponse = getBatteryStatus(token, vin);
			if(batteryStatusResponse!=null && parseBatteryStatusResponse(batteryStatusResponse)) {
				renaultZoeStatusPanel.setStatus(isCharging, chargeLevel, isPlugged, false);
				
//				if(triggerAC) {
//					log.fine("triggering air conditioning");
//					boolean success = triggerAirConditioning(token, vin);
//					renaultZoeStatusPanel.setStatus(isCharging, chargeLevel, isPlugged, success);
//				}
			}
		}
	}
	
	//
	// private members
	//
	private final Logger   log     = Logger.getLogger( this.getClass().getName() );
	
	String  token       = null;
	String  vin         = null;
	Boolean isCharging  = null;
	Integer chargeLevel = null;
	Boolean isPlugged   = null;
	
	RenaultZoeStatusPanel renaultZoeStatusPanel = null;
}