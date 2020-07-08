package picturepi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;



/**
 * Data provider for the status of a Renault electrical car using the MyRenault service
 * Implemented based on https://muscatoxblog.blogspot.com/2019/07/delving-into-renaults-new-api.html
 * 
 * Keys needed:
 *     "servers": {
        "wiredProd": {
            "target": "https://api-wired-prod-1-euw1.wrd-aws.com",
            "apikey": "oF09WnKqvBDcrQzcW1rJNpjIuy7KdGaB"
        },
        "gigyaProd": {
            "target": "https://accounts.eu1.gigya.com",
            "apikey": "3_7PLksOyBRkHv126x5WhHb-5pqC1qFR8pQjxSeLB6nhAnPERTUlwnYoznHSxwX668"
        }
    }
 */
public class MyRenaultStatusProvider extends Provider {

	/**
	 * Constructor
	 * @param task optional additional task to execute in fetchData
	 *             only option: "ac" => Car AC is enabled when calling fetch Data()
	 */
	MyRenaultStatusProvider(@Nullable String task) {
		super(0);
		
		this.task = task;
	}
	
	@Override
	public void run() {
		// calling fetchData() only once as this Provider is for "on request" use
		log.config("run(): Doing a one-time fetchData()");
		
		fetchData();
	}
	
	/**
	 * Executes an HTTP POST query, returning the result as JSON object
	 * @param urlString         URL
	 * @param contentType       HTTP content type
	 * @param inputParameter    A map with key-value parameters that is converted into a URL encoded string
	 *                          and passed as input to the query
	 * @return                  query result as JSON object or null in case of error
	 */
	@Nullable JsonObject executeHttpPostJsonQuery(String urlString, String contentType, @Nullable Map<String,String> inputParameter) {
		return executeHttpPostJsonQuery(urlString, contentType, inputParameter, null);
	}
	
	/**
	 * 
	 * Executes an HTTP POST query, returning the result as JSON object
	 * @param urlString         URL
	 * @param contentType       HTTP content type
	 * @param inputParameter    A map with key-value parameters that is converted into a URL encoded string
	 *                          and passed as input to the query
	 * @param requestProperties optional Map of key-value pairs that will be set as HTTP request properties
	 * @return                  query result as JSON object or null in case of error
	 */
	@Nullable JsonObject executeHttpPostJsonQuery(String urlString, String contentType, @Nullable Map<String,String> inputParameter, @Nullable Map<String,String> requestProperties) {
		log.config("executing HTTP Post query for URL="+urlString);
		
			String inputString = "";
			if(inputParameter!=null) {
				for (Map.Entry<String, String> entry : inputParameter.entrySet()) {
					if(inputString.length()>0) {
						inputString += "&";
					}
					try {
						inputString += entry.getKey()+"="+URLEncoder.encode(entry.getValue(),"UTF-8");
					} catch (UnsupportedEncodingException e) {
						log.severe("execute HTTP Post Query: Unsupported encoding exception:");
						log.severe(e.getMessage());
						
						return null;
					}
				}
			}
			
			log.fine("encoded input:"+inputString);
			
			return executeHttpPostJsonQuery(urlString, contentType, inputString, requestProperties);
	}
	
	/**
	 * Executes an HTTP POST query, returning the result as JSON object
	 * @param urlString         URL
	 * @param contentType       HTTP content type
	 * @param inputString       input string for the query
	 * @param requestProperties optional Map of key-value pairs that will be set as HTTP request properties
	 * @return                  query result as JSON object or null in case of error
	 */
	@Nullable JsonObject executeHttpPostJsonQuery(String urlString, String contentType, String inputString, @Nullable Map<String,String> requestProperties) {
		log.config("executing HTTP Post query for URL="+urlString);
		
		try {
			URL url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", contentType);
			con.setRequestProperty("Accept", "application/json");
			con.setDoInput(true);
			con.setDoOutput(true);
			
			if(requestProperties!=null) {
				for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
					con.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}
			
			try(OutputStream os = con.getOutputStream()) {
				byte[] input = inputString.getBytes("UTF-8");
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
	 * execute an HTTP GET query
	 * @param url
	 * @param requestProperties HTTP request property key/value pairs
	 * @return                  JSON response object or null in case of a problem
	 */
	@Nullable JsonObject executeHttpGetJsonQuery(String urlString, String contentType, @Nullable Map<String,String> requestProperties) {
		log.config("executing HTTP GET query for URL "+urlString);
		
		HttpURLConnection con = null;
		try {
			URL url = new URL (urlString);
			con = (HttpURLConnection)url.openConnection();
			log.fine("connection opened");
			
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", contentType);
			con.setRequestProperty("Accept", "application/json");
			
			if(requestProperties!=null) {
				for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
					con.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}
			
			con.setDoInput(true);
			con.setDoOutput(true);
			
			log.fine("creating reader");
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			log.fine("reading...");
			while ((responseLine = br.readLine()) != null) {
		      log.fine("line read");
			  response.append(responseLine.trim());
			}
			JsonReader reader = Json.createReaderFactory(null).createReader(new StringReader(response.toString()));
			JsonObject jsonObject    = reader.readObject();
			
			log.fine("Json response: "+jsonObject);
			
			return jsonObject;
		} catch (MalformedURLException e) {
			log.severe("malformed URL Exception: "+e.getMessage());
			
			return null;
		} catch (IOException e) {
			log.severe("IOException: "+e.getMessage());
			if(con!=null) {
				try {
					log.severe(con.getResponseMessage());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			return null;
		}
	}

	
	/**
	 * log into the gigya account
	 * @param user
	 * @param password
	 * @return JSON response object or null
	 */
	@Nullable JsonObject login(String user, String password) {
		log.config("logging in to MyRenault Services as user "+user);
		
		Map<String, String> inputParameter = Map.of("loginID",user, "password",password, "apiKey",GIGYA_KEY);
		return executeHttpPostJsonQuery(GIGYA_URL+"/accounts.login", "application/x-www-form-urlencoded;charset=UTF-8", inputParameter);
	}
	
	/**
	 * parse the login response
	 * @param loginResponse  JSON object retrieved from login
	 * @return               cookie value or null in case of an error
	 */
	@Nullable String parseLoginResponse(JsonObject loginResponse) {
		log.fine("parsing login response");
		
		if(loginResponse!=null) {
			JsonObject jsonObjectsessionInfo = loginResponse.getJsonObject("sessionInfo");
			if(jsonObjectsessionInfo!=null) {
				JsonString jsonStringCookieValue = jsonObjectsessionInfo.getJsonString("cookieValue");
				if(jsonStringCookieValue!=null) {
					log.fine("found cookie value: "+jsonStringCookieValue.getString());
						
					return jsonStringCookieValue.getString();
				}
			}
		}
		
		log.warning("Unable to find cookie value");
		
		return null;
	}
	
	/**
	 * gets the Gigy account info
	 * @param cookieValue
	 * @return JSON response object or null in case of errors
	 */
	@Nullable JsonObject getGigyaAccount(String cookieValue) {
		log.config("getting GIGYA account info");
		
		if(cookieValue==null) {
			log.warning("getGigyaAccount call with null parameter");
			
			return null;
		}
		
		Map<String, String> inputParameter = Map.of("oauth_token",cookieValue);
		return executeHttpPostJsonQuery(GIGYA_URL+"/accounts.getAccountInfo", "application/x-www-form-urlencoded;charset=UTF-8", inputParameter);
	}
	
	/**
	 * parses the GIGYA account information
	 * @param response JSON response from Gigya GetAccountInformation query
	 * @return         personId or null in case of errors
	 */
	@Nullable String parseGigyaAccountInformation(JsonObject response) {
		log.fine("parsing Gigya account information");
		
		if(response!=null) {
			JsonObject jsonObjectData = response.getJsonObject("data");
			if(jsonObjectData!=null) {
				JsonString jsonStringPersonId = jsonObjectData.getJsonString("personId");
				if(jsonStringPersonId!=null) {
					log.fine("found Gigya person ID: "+jsonStringPersonId.getString());
						
					return jsonStringPersonId.getString();
				}
			}
		}
		
		log.warning("Unable to find Gigy person ID");
		
		return null;
	}
	
	/**
	 * Gets the Gigya JWT Token
	 * @param  cookieValue
	 * @return JWT token as JSON object or null in case of error
	 */
	@Nullable JsonObject getGigyaJwtToken(String cookieValue) {
		log.config("getting GIGYA JWT Token, input cookieValue="+cookieValue);
		
		if(cookieValue==null) {
			log.warning("getGigyaJwtToken called with null parameter");
			
			return null;
		}
		
		Map<String, String> inputParameter = Map.of("oauth_token",cookieValue,
				"fields","data.personId,data.gigyaDataCenter",
				"expiration","900");
		return executeHttpPostJsonQuery(GIGYA_URL+"/accounts.getJWT", "application/x-www-form-urlencoded;charset=UTF-8", inputParameter);
	}
	
	/**
	 * parses the GIGYA account information
	 * @param response JSON response from Gigya GetJwtToken query
	 * @return         token or null in case of errors
	 */
	@Nullable String parseGigyaJwtToken(JsonObject response) {
		log.fine("parsing Gigya account information");
		
		if(response!=null) {
			JsonString jsonStringToken = response.getJsonString("id_token");
			if(jsonStringToken!=null) {
				log.fine("found Gigya token: "+jsonStringToken.getString());
					
				return jsonStringToken.getString();
			}
		}
		
		log.warning("Unable to find Gigya token");
		
		return null;
	}
	
	/**
	 * gets the Kamereon account id
	 * @param gigyaJwtToken
	 * @param gigyaPersonId
	 * @return JSON response object or null in case of errors
	 */
	@Nullable JsonObject getKamereonAccountId(String gigyaJwtToken,String gigyaPersonId) {
		log.config("getting Kamereon account info, input gigyaJwtToken="+gigyaJwtToken+" gigyaPersonId="+gigyaPersonId);
		
		if(gigyaJwtToken==null || gigyaPersonId==null) {
			log.warning("getKamereonAccountId: input parameter is null. JWT Token="+gigyaJwtToken+" person ID="+gigyaPersonId);
			
			return null;
		}
		
		Map<String, String> requestProperties = Map.of("x-gigya-id_token",gigyaJwtToken, "apikey", KAMEREON_KEY);
		String url = KAMEREON_URL+"/commerce/v1/persons/"+gigyaPersonId+"?country=DE";
		
		return executeHttpGetJsonQuery(url, "application/x-www-form-urlencoded;charset=UTF-8", requestProperties);
	}
	
	/**
	 * parses the Kamereon account ID JSON object
	 * @param response
	 * @return account ID or null in case of errors
	 */
	@Nullable String parseKamereonAccountId(JsonObject response) {
		log.fine("parsing Kamereon account information");
		
		if(response!=null) {
			JsonArray jsonArrayAccounts = response.getJsonArray("accounts");
			if(jsonArrayAccounts!=null) {
				JsonValue jsonValueAccount = jsonArrayAccounts.get(0);
				if(jsonValueAccount!=null) {
					JsonObject jsonObjectAccount = jsonValueAccount.asJsonObject();
					if(jsonObjectAccount!=null) {
						JsonString jsonStringAccountId = jsonObjectAccount.getJsonString("accountId");
						if(jsonStringAccountId!=null) {
							log.fine("found kamreon account ID: "+jsonStringAccountId.getString());
							
							return jsonStringAccountId.getString();
						}
					}
				}
				
			}
		}
		
		log.warning("Unable to find kamereon account ID");
		
		return null;
	}
	
		
	@Nullable JsonObject getVehicleList(String gigyaJwtToken,String kamereonAccountId) {
		log.config("getting vehicle list");
		if(gigyaJwtToken!=null && kamereonAccountId!=null ) {
			Map<String, String> requestProperties = Map.of("x-gigya-id_token",gigyaJwtToken,
					"apikey", KAMEREON_KEY);
					//"x-kamereon-authorization","Bearer: "+kamereonToken);
			String url = KAMEREON_URL+"/commerce/v1/accounts/"+kamereonAccountId+"/vehicles?country=DE";
			
			return executeHttpGetJsonQuery(url, "application/x-www-form-urlencoded;charset=UTF-8", requestProperties);
		}
		else {
			log.warning("getVehcleList: input parameter is null");
			return null;
		}
	}
	
	@Nullable String parseVehicleList(JsonObject response) {
		log.fine("parsing vehicle list");
		
		if(response!=null) {
			JsonArray jsonArrayVehicles = response.getJsonArray("vehicleLinks");
			if(jsonArrayVehicles!=null) {
				JsonValue jsonValueVehicle = jsonArrayVehicles.get(0);
				if(jsonValueVehicle!=null) {
					JsonObject jsonObjectVehicle = jsonValueVehicle.asJsonObject();
					if(jsonObjectVehicle!=null) {
						JsonString jsonStringVin = jsonObjectVehicle.getJsonString("vin");
						if(jsonStringVin!=null) {
							log.fine("found VIN: "+jsonStringVin.getString());
							
							return jsonStringVin.getString();
						}
					}
				}
			}
		}
		
		log.warning("Unable to find kamereon token");
		
		return null;
	}
	
	@Nullable JsonObject getBatteryStatus(String gigyaJwtToken,String kamereonAccountId, String vin) {
		log.config("getting battery status");
		
		if(gigyaJwtToken!=null && kamereonAccountId!=null && vin!=null) {
			Map<String, String> requestProperties = Map.of("x-gigya-id_token",gigyaJwtToken,
					"apikey", KAMEREON_KEY);
			// String url = KAMEREON_URL+"/commerce/v1/accounts/kmr/remote-services/car-adapter/v1/cars/"+vin+"/battery-status";
			// changed, see: https://github.com/jamesremuscat/pyze/issues/34
			String url = KAMEREON_URL+"/commerce/v1/accounts/"+kamereonAccountId+"/kamereon/kca/car-adapter/v1/cars/"+vin+"/battery-status?country=DE";
			
			return executeHttpGetJsonQuery(url, "application/vnd.api+json", requestProperties);
		}
		else {
			log.warning("getBatteryStatus: parameter is null");
			return null;
		}
	}
	
	/**
	 * turns on the AC
	 * @param gigyaJwtToken
	 * @param kamereonAccountId
	 * @param kamereonToken
	 * @param vin
	 * @return true in case of success, otherwise false
	 */
	boolean enableAc(String gigyaJwtToken,String kamereonAccountId,String vin) {
		if(gigyaJwtToken!=null && kamereonAccountId!=null ) {
			Map<String, String> requestProperties = Map.of("x-gigya-id_token",gigyaJwtToken,
					"apikey", KAMEREON_KEY);
					//"x-kamereon-authorization","Bearer "+kamereonToken);
			
			 JsonBuilderFactory factory = Json.createBuilderFactory(null);
			 JsonObject value = factory.createObjectBuilder()
			     .add("data", factory.createObjectBuilder()
			         .add("type", "HvacStart")
			         .add("attributes", factory.createObjectBuilder()
			             .add("action","start")
			        	 .add("targetTemperature", 21)))
			     .build();
			 
			 log.fine("JSON object: "+value.toString());
			 
			 // TODO: URL changed
			 // see : https://github.com/jamesremuscat/pyze/issues/34
			 String url = KAMEREON_URL+"/commerce/v1/accounts/"+kamereonAccountId+"/kamereon/kca/car-adapter/v1/cars/"+vin+"/actions/hvac-start?country=DE";
			 JsonObject rc = executeHttpPostJsonQuery(url, "application/vnd.api+json", value.toString(),requestProperties);
			 
			 
			 
			 if(rc!=null) {
				 log.fine("return value:"+rc.toString());
			 }
			 
			 return rc!=null;
		}
		else {
			log.warning("enableAC: parameter is null");
			return false;
		}
	}
	

	@Override
	protected void fetchData() {
		log.fine("fetchData() called");
		
		if(myRenaultStatusPanel==null) {
			if(panel.getClass()==MyRenaultStatusPanel.class) {
				myRenaultStatusPanel = (MyRenaultStatusPanel)panel;
			}
			else {
				log.severe("Panel is not of class RenaultZoeStatusPanel. Disabling updates.");
				
				return;
			}
		}
		
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		String cookieValue   = parseLoginResponse(login(user, password));
		String jwtToken      = parseGigyaJwtToken(getGigyaJwtToken(cookieValue));
		String personId      = parseGigyaAccountInformation(getGigyaAccount(cookieValue));
		String accountId     = parseKamereonAccountId(getKamereonAccountId(jwtToken,personId));
		String vin           = parseVehicleList(getVehicleList(jwtToken, accountId));
		
		JsonObject jsonObjectBatteryStatus = getBatteryStatus(jwtToken, accountId, vin);
		
		/* 
		  sample response for battery status
		  {
		    "id":"myVIN",
		    "timeRequiredToFullFast":null
		    "vehicleUnplugTimestamp":null
		    "batteryBarLevel":null
		    "rangeHvacOn":null
		    "rangeHvacOff":172,
		    "vehiclePlugTimestamp":null,
		    "timeRequiredToFullNormal":null,
		    "plugStatus":0,
		    "instantaneousPower":null,
		    "batteryCapacity":null,
		    "batteryRemainingAmount":null,
		    "timeRequiredToFullSlow":null,
		    "batteryLevel":79,
		    "plugStatusDetail":null,
		    "chargeStatus":-1,
		    "lastUpdateTime":"2020-02-28T12:08:01+01:00",
		    "chargePower":null,
		    "batteryTemperature":7
		  }
		*/
		
		Boolean isCharging  = null;
		Integer chargeLevel = null;
		Boolean isPlugged   = null;
		Integer range       = null;
		Boolean acEnabled   = null;
		if(jsonObjectBatteryStatus!=null) {
			JsonNumber jsonNumberBatteryLevel = jsonObjectBatteryStatus.getJsonNumber("batteryLevel");
			if(jsonNumberBatteryLevel!=null) {
				chargeLevel = jsonNumberBatteryLevel.intValue();
			}
			
			JsonNumber jsonNumberPlugStatus = jsonObjectBatteryStatus.getJsonNumber("plugStatus");
			if(jsonNumberPlugStatus!=null) {
				isPlugged = jsonNumberPlugStatus.intValue() > 0;
			}
			
			JsonNumber jsonNumberRange = jsonObjectBatteryStatus.getJsonNumber("rangeHvacOff");
			if(jsonNumberRange!=null) {
				range = jsonNumberRange.intValue();
			}
			
			if(task!=null && task.equalsIgnoreCase("AC")) {
				log.fine("triggering AC");
				acEnabled = enableAc(jwtToken, accountId, vin);
			}
			else {
				log.fine("just retrieving status - AC not triggered");
			}
		}
		
		myRenaultStatusPanel.setStatus(isCharging, chargeLevel, isPlugged, range, acEnabled);
	}
	
	//
	// private members
	//
	private final Logger   log     = Logger.getLogger( this.getClass().getName() );
	
    private final String GIGYA_URL = "https://accounts.eu1.gigya.com";
    private final String GIGYA_KEY = "3_7PLksOyBRkHv126x5WhHb-5pqC1qFR8pQjxSeLB6nhAnPERTUlwnYoznHSxwX668";


    private final String KAMEREON_URL = "https://api-wired-prod-1-euw1.wrd-aws.com";
    private final String KAMEREON_KEY = "oF09WnKqvBDcrQzcW1rJNpjIuy7KdGaB";
    		
    
	String  task              = null;
	
	MyRenaultStatusPanel myRenaultStatusPanel = null;
}

