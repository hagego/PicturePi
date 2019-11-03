package picturepi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Data provider for the status of a Renault Zoe electrical car.
 * Implemented based on https://github.com/edent/Renault-Zoe-API
 */
public class RenaultZoeStatusProvider extends Provider {

	RenaultZoeStatusProvider() {
		super(3600);
	}
	
	
	/**
	 * log into the Renault ZE Service webpage to retrieve all data for further steps 
	 * @param user      user
	 * @param password  password
	 * @return          true if log was successful, otherwise false
	 */
	boolean login(String user, String password) {
		log.config("logging in to Renault ZE Services webpage as user "+user);
		
		try {
			URL url = new URL ("https://www.services.renault-ze.com/api/user/login");
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			
			
			String jsonInputString = "{\"username\": \"login0815@posteo.net\", \"password\": \"skzRr6qysImV_\"}";
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
					    System.out.println(response.toString());
			
		} catch (MalformedURLException e) {
			log.severe("malformed URL Exception");
			
			return false;
		} catch (IOException e) {
			log.severe("IOException: "+e.getMessage());
			
			return false;
		}
		
		
		return false;
	}

	@Override
	protected void fetchData() {
		// TODO Auto-generated method stub

	}
	
	//
	// private members
	//
	private final Logger   log     = Logger.getLogger( this.getClass().getName() );
}
