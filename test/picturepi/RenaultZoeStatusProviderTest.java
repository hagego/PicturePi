package picturepi;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.util.logging.LogManager;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RenaultZoeStatusProviderTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		Configuration.getConfiguration().readConfigurationFile("conf/picturepi.ini");
		System.setProperty( "java.util.logging.config.file", "conf/picturepi.logging" );
		
		try {
			LogManager.getLogManager().readConfiguration();
		}
		catch ( Exception e ) {
			// unable to read logging configuration file
			e.printStackTrace();
		}
	}

	@BeforeEach
	void setUp() throws Exception {
		provider = new RenaultZoeStatusProvider();
	}

	@Test
	void testLoginWithInvalidUser() {
		assertNull(provider.login("dummy", "user")); 
	}
	
	@Test
	void testLoginWithValidUser() {
		String user     = Configuration.getConfiguration().getValue("RenaultZoeStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("RenaultZoeStatusPanel", "password", "");
		
		assertNotNull(provider.login(user, password)); 
	}
	
	@Test
	void testParseLoginResponseWithValidData() {
		String loginResponseString = 
				"{\r\n" + 
				"	\"token\": \"AAAA\",\r\n" + 
				"	\"refresh_token\": \"BBBB\",\r\n" + 
				"	\"user\": {\r\n" + 
				"		\"id\": \"CCCC\",\r\n" + 
				"		\"locale\": \"en_GB\",\r\n" + 
				"		\"country\": \"GB\",\r\n" + 
				"		\"timezone\": \"Europe/London\",\r\n" + 
				"		\"email\": \"you@example.com\",\r\n" + 
				"		\"first_name\": \"Terence\",\r\n" + 
				"		\"last_name\": \"Eden\",\r\n" + 
				"		\"phone_number\": \"+447700900123\",\r\n" + 
				"		\"vehicle_details\": {\r\n" + 
				"			\"timezone\": \"Europe/London\",\r\n" + 
				"			\"VIN\": \"VVVV\",\r\n" + 
				"			\"activation_code\": \"GGGG\",\r\n" + 
				"			\"phone_number\": \"+447700900123\"\r\n" + 
				"		},\r\n" + 
				"		\"scopes\": [\"BATTERY_CHARGE_STATUS\", \r\n" + 
				"				   \"BATTERY_CHARGE_HISTORY\", \r\n" + 
				"				   \"BATTERY_CHARGE_REMOTE_ACTIVATION\", \r\n" + 
				"				   \"BATTERY_CHARGE_SCHEDULING\", \r\n" + 
				"				   \"AC_REMOTE_CONTROL\", \r\n" + 
				"				   \"BATTERY_CHARGE_LOWALERT\"],\r\n" + 
				"		\"active_account\": \"DDDD\",\r\n" + 
				"		\"associated_vehicles\": [{\r\n" + 
				"			\"VIN\": \"VVVV\",\r\n" + 
				"			\"activation_code\": \"GGGG\",\r\n" + 
				"			\"user_id\": \"XXXX\"\r\n" + 
				"		}],\r\n" + 
				"		\"gdc_uid\": \"YYYY\"\r\n" + 
				"	}\r\n" + 
				"}";
		
		JsonObject lsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(loginResponseString)).readObject();
		assertTrue(provider.parseLoginResponse(lsonResponseObject));
	}
	
	@Test
	void testParseLoginResponseWithInvalidData() {
		String loginResponseString = 
				"{\r\n" + 
				"	\"token\": \"AAAA\",\r\n" + 
				"	\"refresh_token\": \"BBBB\",\r\n" + 
				"	\"user\": {\r\n" + 
				"		\"id\": \"CCCC\",\r\n" + 
				"		\"locale\": \"en_GB\",\r\n" + 
				"		\"country\": \"GB\",\r\n" + 
				"		\"timezone\": \"Europe/London\",\r\n" + 
				"		\"email\": \"you@example.com\",\r\n" + 
				"		\"first_name\": \"Terence\",\r\n" + 
				"		\"last_name\": \"Eden\",\r\n" + 
				"		\"phone_number\": \"+447700900123\",\r\n" + 
				"		\"gdc_uid\": \"YYYY\"\r\n" + 
				"	}\r\n" + 
				"}";
		
		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(loginResponseString)).readObject();
		assertFalse(provider.parseLoginResponse(jsonResponseObject));
	}
	
	@Test
	void testGetBatteryStatusWithInvalidData() {
		assertNull(provider.getBatteryStatus("dummy", "dummy")); 
	}
	
	@Test
	void testParseBatteryStatusWithValidData() {
		String batteryStatusResponseString = "{\r\n" + 
				"	\"charging\": false,\r\n" + 
				"	\"plugged\": true,\r\n" + 
				"	\"charge_level\": 100,\r\n" + 
				"	\"remaining_range\": 124.0,\r\n" + 
				"	\"last_update\": 1476472742000,\r\n" + 
				"	\"charging_point\": \"INVALID\",\r\n" + 
				"	\"remaining_time\": 750\r\n" + 
				"}";
		
		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(batteryStatusResponseString)).readObject();
		assertTrue(provider.parseBatteryStatusResponse(jsonResponseObject));
	}
	
	@Test
	void testParseBatteryStatusWithInvalidData() {
		String batteryStatusResponseString = "{\r\n" + 
				"	\"plugged\": true,\r\n" + 
				"	\"charge_level\": 100,\r\n" + 
				"	\"remaining_range\": 124.0,\r\n" + 
				"	\"last_update\": 1476472742000,\r\n" + 
				"	\"charging_point\": \"INVALID\",\r\n" + 
				"	\"remaining_time\": 750\r\n" + 
				"}";
		
		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(batteryStatusResponseString)).readObject();
		assertFalse(provider.parseBatteryStatusResponse(jsonResponseObject));
	}
	
	// private members
	private RenaultZoeStatusProvider provider;
}
