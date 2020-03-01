package picturepi;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.util.logging.LogManager;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class MyRenaultStatusProviderTest {

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
		provider = new MyRenaultStatusProvider("status");
	}

	@Test
	void testLoginWithInvalidUser() {
		assertNotNull(provider.login("dummy", "user")); 
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_MYRENAULT_CREDENTIALS", matches = ".*")
	void testLoginWithValidUser() {
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		assertNotNull(provider.login(user, password)); 
	}
	
	@Test
	void testParseLoginResponseWithValidData() {
		final String loginResponseString = "{"
				+ "  \"callId\":\"c35e69bd06a6457d8b4929b2e56aeae3\","
				+ "  \"errorCode\":0,"
				+ "  \"apiVersion\":2,"
				+ "  \"statusCode\":200,"
				+ "  \"statusReason\":\"OK\","
				+ "  \"time\":\"2020-02-15T15:31:17.698Z\","
				+ "  \"registeredTimestamp\":1580248193,"
				+ "  \"UID\":\"7e08df11912e4dff97c0734e28b31e95\","
				+ "  \"UIDSignature\":\"VGJizpgNotRmnK5iTxJBqaOUe/U=\","
				+ "  \"signatureTimestamp\":\"1581780677\","
				+ "  \"created\":\"2020-01-28T21:49:53.709Z\","
				+ "  \"createdTimestamp\":1580248193,"
				+ "  \"isActive\":true,\"isRegistered\":true,"
				+ "  \"isVerified\":true,"
				+ "  \"lastLogin\":\"2020-02-15T15:31:17.670Z\","
				+ "  \"lastLoginTimestamp\":1581780677,"
				+ "  \"lastUpdated\":\"2020-01-28T21:50:15.958Z\","
				+ "  \"lastUpdatedTimestamp\":1580248215958,"
				+ "  \"loginProvider\":\"site\","
				+ "  \"oldestDataUpdated\":\"2020-01-28T21:49:53.709Z\","
				+ "  \"oldestDataUpdatedTimestamp\":1580248193709,"
				+ "  \"profile\":{"
				+ "    \"email\":\"a@b.net\""
				+ "  },"
				+ "  \"registered\":\"2020-01-28T21:49:53.917Z\","
				+ "  \"socialProviders\":\"site\","
				+ "  \"verified\":\"2020-01-28T21:50:15.958Z\","
				+ "  \"verifiedTimestamp\":1580248215958,"
				+ "  \"newUser\":false,"
				+ "  \"sessionInfo\":{"
				+ "    \"cookieName\":\"myCookieName\","
				+ "    \"cookieValue\":\"myCookieValue\""
				+ "  }"
				+ "}";

		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(loginResponseString)).readObject();
		assertEquals(provider.parseLoginResponse(jsonResponseObject),"myCookieValue");
	}
	
	@Test
	void testParseLoginResponseWithInvalidData() {
		final String loginResponseString=
				"{"
				+ "  \"callId\":\"a79cd6d5e96d48e6ac316593166e2edd\","
				+ "  \"errorCode\":403042,"
				+ "  \"errorDetails\":\"invalid loginID or password\","
				+ "  \"errorMessage\":\"Invalid LoginID\","
				+ "  \"apiVersion\":2,"
				+ "  \"statusCode\":403,"
				+ "  \"statusReason\":\"Forbidden\","
				+ "  \"time\":\"2020-02-15T15:31:17.486Z\""
				+ "}";

		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(loginResponseString)).readObject();
		assertNull(provider.parseLoginResponse(jsonResponseObject));
	}
	
	@Test
	void testGetGigyaAccountWithInvalidData() {
		assertNotNull(provider.getGigyaAccount("myCookieValue")); 
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_MYRENAULT_CREDENTIALS", matches = ".*")
	void testGetGigyaAccountWithValidData() {
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		String cookieValue = provider.parseLoginResponse(provider.login(user, password));
		assertNotNull(provider.getGigyaAccount(cookieValue)); 
	}
	
	@Test
	void testParseGigyAccountInformationWithValidData() {
		final String responseString =
				  "{"
				+ "  \"callId\":\"49e9db3500784c54a9513465d4c94b66\","
				+ "  \"errorCode\":0,"
				+ "  \"apiVersion\":2,"
				+ "  \"statusCode\":200,"
				+ "  \"statusReason\":\"OK\","
				+ "  \"time\":\"2020-02-15T20:36:46.266Z\","
				+ "  \"registeredTimestamp\":1580248193000,"
				+ "  \"UID\":\"7e08df11912e4dff97c0734e28b31e95\","
				+ "  \"UIDSignature\":\"qvNbwfr3M2NgJi7q/9mDzugykRI=\","
				+ "  \"signatureTimestamp\":\"1581799006\","
				+ "  \"created\":\"2020-01-28T21:49:53.709Z\","
				+ "  \"createdTimestamp\":1580248193000,"
				+ "  \"data\":"
				+ "  {"
				+ "    \"personId\":\"123\","
				+ "    \"gigyaDataCenter\":\"eu1.gigya.com\""
				+ "  },"
				+ "  \"preferences\":{},"
				+ "  \"emails\":"
				+ "  {"
				+ "    \"verified\":[\"a@b.net\"],"
				+ "    \"unverified\":[]"
				+ "  },"
				+ "  \"isActive\":true,"
				+ "  \"isRegistered\":true,"
				+ "  \"isVerified\":true,"
				+ "  \"lastLogin\":\"2020-02-15T20:36:46.150Z\","
				+ "  \"lastLoginTimestamp\":1581799006000,"
				+ "  \"lastUpdated\":\"2020-01-28T21:50:15.958Z\","
				+ "  \"lastUpdatedTimestamp\":1580248215958,"
				+ "  \"loginProvider\":\"site\","
				+ "  \"oldestDataUpdated\":\"2020-01-28T21:49:53.709Z\","
				+ "  \"oldestDataUpdatedTimestamp\":1580248193709,"
				+ "  \"profile\":"
				+ "  {"
				+ "    \"email\":\"a@b.net\""
				+ "  },"
				+ "  \"registered\":\"2020-01-28T21:49:53.917Z\","
				+ "  \"socialProviders\":\"site\","
				+ "  \"verified\":\"2020-01-28T21:50:15.958Z\","
				+ "  \"verifiedTimestamp\":1580248215958"
				+ "}";

		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(responseString)).readObject();
		assertEquals(provider.parseGigyaAccountInformation(jsonResponseObject),"123");
	}
	
	@Test
	void testParseGigyAccountInformationWithInvalidData() {
		final String responseString = 
				  "{"
				+ "  \"errorMessage\":\"Invalid ApiKey parameter\","
				+ "  \"errorDetails\":\"GSKeyBase is invalid, no version: myCookieValue\","
				+ "  \"statusCode\":400,"
				+ "  \"errorCode\":400093,"
				+ "  \"statusReason\":\"Bad Request\","
				+ "  \"callId\":\"62070435b70747a18d87ea9c9601d78f\","
				+ "  \"time\":\"2020-02-15T20:36:45.996Z\""
				+ "}";

		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(responseString)).readObject();
		assertNull(provider.parseGigyaAccountInformation(jsonResponseObject));
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_MYRENAULT_CREDENTIALS", matches = ".*")
	void testGetGigyaJwtTokenWithValidData() {
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		String cookieValue = provider.parseLoginResponse(provider.login(user, password));
		assertNotNull(provider.getGigyaJwtToken(cookieValue)); 
	}
	
	@Test
	void testParseGigyJwtTokenWithInvalidData() {
		final String responseString = 
				  "{"
				+ "  \"callId\":\"a9c1d0f602524a25ae602b8a5db7413a\","
				+ "	 \"errorCode\":403005,"
				+ "  \"errorDetails\":\"Session not found\","
				+ "  \"errorMessage\":\"Unauthorized user\","
				+ "  \"apiVersion\":2,"
				+ "  \"statusCode\":403,"
				+ "  \"statusReason\":\"Forbidden\","
				+ "  \"time\":\"2020-02-16T16:07:08.091Z\""
				+ "}";

		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(responseString)).readObject();
		assertNull(provider.parseGigyaJwtToken(jsonResponseObject));
	}

	@Test
	void testParseGigyJwtTokenWithValidData() {
		final String responseString = 
				  "{"
				+ "  \"callId\":\"f06a70dcd9df487494f406af9ed18978\","
				+ "  \"errorCode\":0,"
				+ "  \"apiVersion\":2,"
				+ "  \"statusCode\":200,"
				+ "  \"statusReason\":\"OK\","
				+ "  \"time\":\"2020-02-16T16:11:08.570Z\","
				+ "  \"ignoredFields\":\"\","
				+ "  \"id_token\":\"myToken\""
				+ "}"; 

		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(responseString)).readObject();
		assertEquals(provider.parseGigyaJwtToken(jsonResponseObject),"myToken");
	}
	
	@Test
	void testGetKamereonAccountIdWithInValidData() {
		assertNull(provider.getKamereonAccountId("myToken","myPersonId")); 
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_MYRENAULT_CREDENTIALS", matches = ".*")
	void testGetKamereonAccountIdWithValidData() {
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		String cookieValue = provider.parseLoginResponse(provider.login(user, password));
		
		String jwtToken = provider.parseGigyaJwtToken(provider.getGigyaJwtToken(cookieValue));
		String personId = provider.parseGigyaAccountInformation(provider.getGigyaAccount(cookieValue));
		
		assertNotNull(provider.getKamereonAccountId(jwtToken,personId)); 
	}
	
	@Test
	void testParseKamereonAccountIdWithValidData() {
		final String responseString =
				  "{"
				+ "  \"personId\":\"1234\","
				+ "  \"type\":\"I\","
				+ "  \"country\":\"DE\","
				+ "  \"civility\":\"1\","
				+ "  \"firstName\":\"first\","
				+ "  \"lastName\":\"last\","
				+ "  \"idp\":"
				+ "  {"
				+ "    \"idpId\":\"123\","
				+ "    \"idpType\":\"GIGYA\","
				+ "    \"idpStatus\":\"ACTIVE\","
				+ "    \"login\":\"a@b.net\","
				+ "    \"loginType\":\"EMAIL\","
				+ "    \"termsConditionAcceptance\":true,"
				+ "    \"termsConditionLastAcceptanceDate\":\"2020-01-28T21:49:53.966367Z\""
				+ "  },"
				+ "  \"emails\":"
				+ "  ["
				+ "    {\"emailType\":\"MAIN\","
				+ "      \"emailValue\":\"a@b.net\","
				+ "      \"validityFlag\":true,"
				+ "      \"createdDate\":\"2020-01-28T21:49:53.976046Z\","
				+ "      \"lastModifiedDate\":\"2020-01-28T21:49:53.976048Z\","
				+ "      \"functionalCreationDate\":\"2020-01-28T21:49:53.976046Z\","
				+ "      \"functionalModificationDate\":\"2020-01-28T21:49:53.976048Z\""
				+ "    }"
				+ "  ],"
				+ "  \"phones\":"
				+ "  ["
				+ "    {"
				+ "      \"phoneType\":\"MOBILE\","
				+ "      \"phoneValue\":\"0123456\","
				+ "      \"areaCode\":\"49\","
				+ "      \"createdDate\":\"2020-01-28T21:56:00.862923Z\","
				+ "      \"lastModifiedDate\":\"2020-01-28T21:56:00.863090Z\","
				+ "      \"functionalCreationDate\":\"2020-01-28T21:56:00.862923Z\","
				+ "      \"functionalModificationDate\":\"2020-01-28T21:56:00.863090Z\""
				+ "    }"
				+ "  ],"
				+ "  \"identities\":[],"
				+ "  \"myrRequest\":false,"
				+ "  \"accounts\":"
				+ "  ["
				+ "    {"
				+ "      \"accountId\":\"myAccountId\","
				+ "      \"accountType\":\"MYRENAULT\","
				+ "      \"accountStatus\":\"ACTIVE\","
				+ "      \"country\":\"DE\","
				+ "      \"personId\":\"1234\","
				+ "      \"relationType\":\"OWNER\""
				+ "    }"
				+ "  ],"
				+ "  \"partyId\":\"eac123\","
				+ "  \"createdDate\":\"2020-01-28T21:49:53.977382Z\","
				+ "  \"lastModifiedDate\":\"2020-01-28T21:56:00.902519Z\","
				+ "  \"functionalCreationDate\":\"2020-01-28T21:49:53.975927Z\","
				+ "  \"functionalModificationDate\":\"2020-01-28T21:56:00.863091Z\","
				+ "  \"locale\":\"de-DE\","
				+ "  \"lastModificationOrigin\":\"MYRENAULT\","
				+ "  \"originApplicationName\":\"One_Nginx\","
				+ "  \"originUserId\":\"awwir01\""
				+ "}"; 

		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(responseString)).readObject();
		assertEquals(provider.parseKamereonAccountId(jsonResponseObject),"myAccountId");
	}
	
	@Test
	void testGetKamereonTokenWithInValidData() {
		assertNull(provider.getKamereonToken("myToken","myAccountId")); 
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_MYRENAULT_CREDENTIALS", matches = ".*")
	void testGetKamereonTokenWithValidData() {
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		String cookieValue = provider.parseLoginResponse(provider.login(user, password));
		
		String jwtToken = provider.parseGigyaJwtToken(provider.getGigyaJwtToken(cookieValue));
		String personId = provider.parseGigyaAccountInformation(provider.getGigyaAccount(cookieValue));
		
		String accountId = provider.parseKamereonAccountId(provider.getKamereonAccountId(jwtToken,personId));
		
		assertNotNull(provider.getKamereonToken(jwtToken, accountId));
	}

	@Test
	void testParseKamereonTokenWithValidData() {
		final String responseString = 
				  "{"
				+ "  \"nonce\":\"123\","
				+ "  \"scope\":\"openid profile vehicles\","
				+ "  \"accessToken\":\"myToken\","
				+ "  \"tokenType\":\"Bearer\","
				+ "  \"expiresIn\":3599"
				+ "}";
		
		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(responseString)).readObject();
		assertEquals(provider.parseKamereonToken(jsonResponseObject),"myToken");
	}
	
	@Test
	void testParseKamereonTokenWithInvalidData() {
		final String responseString = 
				  "{"
				+ "  \"nonce\":\"123\","
				+ "  \"scope\":\"openid profile vehicles\","
				+ "  \"tokenType\":\"Bearer\","
				+ "  \"expiresIn\":3599"
				+ "}";
		
		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(responseString)).readObject();
		assertNull(provider.parseKamereonToken(jsonResponseObject));
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_MYRENAULT_CREDENTIALS", matches = ".*")
	void testGetVehicleListWithValidData() {
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		String cookieValue = provider.parseLoginResponse(provider.login(user, password));
		
		String jwtToken = provider.parseGigyaJwtToken(provider.getGigyaJwtToken(cookieValue));
		String personId = provider.parseGigyaAccountInformation(provider.getGigyaAccount(cookieValue));
		
		String accountId = provider.parseKamereonAccountId(provider.getKamereonAccountId(jwtToken,personId));
		String kamereonToken = provider.parseKamereonToken(provider.getKamereonToken(jwtToken, accountId));
		
		assertNotNull(provider.getVehicleList(jwtToken, accountId, kamereonToken));
	}
	
	@Test
	void testParseVehicleListWithValidData() {
		final String responseString = 
				    "{"
				  + "  \"accountId\":\"myAccountId\","
				  + "  \"country\":\"DE\","
				  + "  \"vehicleLinks\":"
				  + "  ["
				  + "    {"
				  + "      \"brand\":\"RENAULT\","
				  + "      \"vin\":\"myVIN\","
				  + "      \"status\":\"ACTIVE\","
				  + "      \"linkType\":\"USER\","
				  + "      \"garageBrand\":\"renault\","
				  + "      \"startDate\":\"2020-01-28\","
				  + "      \"createdDate\":\"2020-01-28T21:51:33.918626Z\","
				  + "      \"lastModifiedDate\":\"2020-01-28T21:57:01.176321Z\","
				  + "      \"cancellationReason\":{},"
				  + "      \"connectedDriver\":"
				  + "      {"
				  + "        \"role\":\"MAIN_DRIVER\","
				  + "        \"createdDate\":\"2020-01-28T21:57:01.175217Z\","
				  + "        \"lastModifiedDate\":\"2020-01-28T21:57:01.175217Z\""
				  + "      },"
				  + "      \"vehicleDetails\":"
				  + "      {"
				  + "        \"vin\":\"myVIN\","
				  + "        \"engineType\":\"5AQ\","
				  + "        \"engineRatio\":\"607\","
				  + "        \"deliveryCountry\":"
				  + "        {"
				  + "          \"code\":\"DE\","
				  + "          \"label\":\"ALLEMAGNE\""
				  + "        },"
				  + "        \"family\":"
				  + "        {"
				  + "          \"code\":\"X10\","
				  + "          \"label\":\"FAMILLE X10\","
				  + "          \"group\":\"007\""
				  + "        },"
				  + "        \"tcu\":"
				  + "        {"
				  + "          \"code\":\"TCU0G2\","
				  + "          \"label\":\"TCU VER 0 GEN 2\","
				  + "          \"group\":\"E70\"},"
				  + "          \"navigationAssistanceLevel\":"
				  + "          {"
				  + "            \"code\":\"NAV3G5\","
				  + "            \"label\":\"LEVEL 3 TYPE 5 NAVIGATION\","
				  + "            \"group\":\"408\""
				  + "          },"
				  + "          \"battery\":"
				  + "          {"
				  + "            \"code\":\"BT4AR1\","
				  + "            \"label\":\"BATTERIE BT4AR1\","
				  + "            \"group\":\"968\""
				  + "          },"
				  + "          \"radioType\":"
				  + "          {"
				  + "            \"code\":\"RAD06D\","
				  + "            \"label\":\"RADIO 06D\","
				  + "            \"group\":\"425\""
				  + "          },"
				  + "          \"registrationCountry\":"
				  + "          {"
				  + "            \"code\":\"DE\""
				  + "          },"
				  + "          \"brand\":"
				  + "          {"
				  + "            \"label\":\"RENAULT\""
				  + "          },"
				  + "          \"model\":"
				  + "          {"
				  + "            \"code\":\"X101VE\","
				  + "            \"label\":\"ZOE\","
				  + "            \"group\":\"971\""
				  + "          },"
				  + "          \"gearbox\":"
				  + "          {"
				  + "            \"code\":\"BVEL\","
				  + "            \"label\":\"BOITE A VARIATEUR ELECTRIQUE\","
				  + "            \"group\":\"427\""
				  + "          },"
				  + "          \"version\":"
				  + "          {"
				  + "            \"code\":\"LTD MF 18R\""
				  + "          },"
				  + "          \"energy\":"
				  + "          {"
				  + "            \"code\":\"ELEC\","
				  + "            \"label\":\"ELECTRIQUE\","
				  + "            \"group\":\"019\""
				  + "          },"
				  + "          \"registrationNumber\":\"\","
				  + "          \"vcd\":\"SYTINC/SKTPOU/SAND41/FDIU1/SSESM/MAPSTD/SSCALL/SAND88/SAND90/SQKDRO/SDIFPA/FACBA2/PRLEX1/AVRCAR/TCU0G2/WALB01/EVTEC1/SPMIR/EVCER/STANDA/X10/B10/EA2/MF/ELEC/DG/TEMP/TR4X2/RV/ABS/CAREG/LAC/VT003/CPE/RET03/SPROJA/RALU17/CEAVRH/AIRBA1/SERIE/DRA/DRAP08/HARM02/3ATRPH/FBANAR/TEKQA/SFBANA/KM/DPRPN/AVREPL/SSDECA/ASRESP/RDAR02/ALEVA/CACBL2/SOP02C/CTHAB2/TRNOR/LVAVIP/LVAREL/SASURV/KTGREP/SGACHA/APL03/ALOUCC/CMAR3P/NAV3G5/RAD06D/BVEL/AUTAUG/RNORM/ISOFIX/EQPEUR/HRGM01/SDPCLV/TLALLE/SPRODI/SAN613/SSAPEX/GENEV2/ELC1/SANCML/PE2012/PHAS1/LTD/045KWH/BT4AR1/VEC231/X101VE/NBT017/5AQ\","
				  + "          \"assets\":"
				  + "          ["
				  + "            {"
				  + "              \"assetType\":\"PICTURE\","
				  + "              \"renditions\":"
				  + "              ["
				  + "                {"
				  + "                  \"resolutionType\":\"ONE_MYRENAULT_LARGE\","
				  + "                  \"url\":\"https://3dv2.renault.com/ImageFromBookmark?configuration=SKTPOU%2FPRLEX1%2FSTANDA%2FB10%2FEA2%2FDG%2FVT003%2FRET03%2FRALU17%2FDRAP08%2FHARM02%2F3ATRPH%2FFBANAR%2FTEKQA%2FRDAR02%2FALEVA%2FSOP02C%2FTRNOR%2FLVAVIP%2FLVAREL%2FSGACHA%2FNAV3G5%2FRAD06D%2FSDPCLV%2FGENEV2%2FLTD%2FBT4AR1%2FNBT017&databaseId=1bc39d1c-ca60-4d83-9198-9bfc61d2da12&bookmarkSet=RSITE&bookmark=EXT_34_DESSUS&profile=HELIOS_OWNERSERVICES_LARGE\""
				  + "                },"
				  + "                {"
				  + "                  \"resolutionType\":\"ONE_MYRENAULT_SMALL\","
				  + "                  \"url\":\"https://3dv2.renault.com/ImageFromBookmark?configuration=SKTPOU%2FPRLEX1%2FSTANDA%2FB10%2FEA2%2FDG%2FVT003%2FRET03%2FRALU17%2FDRAP08%2FHARM02%2F3ATRPH%2FFBANAR%2FTEKQA%2FRDAR02%2FALEVA%2FSOP02C%2FTRNOR%2FLVAVIP%2FLVAREL%2FSGACHA%2FNAV3G5%2FRAD06D%2FSDPCLV%2FGENEV2%2FLTD%2FBT4AR1%2FNBT017&databaseId=1bc39d1c-ca60-4d83-9198-9bfc61d2da12&bookmarkSet=RSITE&bookmark=EXT_34_DESSUS&profile=HELIOS_OWNERSERVICES_SMALL_V2\""
				  + "                }"
				  + "              ]"
				  + "            }"
				  + "          ],"
				  + "          \"yearsOfMaintenance\":12,"
				  + "          \"deliveryDate\":\"2019-06-18\","
				  + "          \"retrievedFromDhs\":false"
				  + "        }"
				  + "      }"
				  + "    ]"
				  + "  }";
		
		JsonObject jsonResponseObject = Json.createReaderFactory(null).createReader(new StringReader(responseString)).readObject();
		
		assertEquals(provider.parseVehicleList(jsonResponseObject),"myVIN");
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_MYRENAULT_CREDENTIALS", matches = ".*")
	void testGetBatteryStatusWithValidData() {
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		String cookieValue = provider.parseLoginResponse(provider.login(user, password));
		
		String jwtToken = provider.parseGigyaJwtToken(provider.getGigyaJwtToken(cookieValue));
		String personId = provider.parseGigyaAccountInformation(provider.getGigyaAccount(cookieValue));
		
		String accountId = provider.parseKamereonAccountId(provider.getKamereonAccountId(jwtToken,personId));
		String kamereonToken = provider.parseKamereonToken(provider.getKamereonToken(jwtToken, accountId));
		
		String vin = provider.parseVehicleList(provider.getVehicleList(jwtToken, accountId, kamereonToken));
		
		assertNotNull(provider.getBatteryStatus(jwtToken, accountId, kamereonToken,vin));
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_MYRENAULT_CREDENTIALS", matches = ".*")
	void testEnableAc() {
		String user     = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "user", "");
		String password = Configuration.getConfiguration().getValue("MyRenaultStatusPanel", "password", "");
		
		String cookieValue = provider.parseLoginResponse(provider.login(user, password));
		
		String jwtToken = provider.parseGigyaJwtToken(provider.getGigyaJwtToken(cookieValue));
		String personId = provider.parseGigyaAccountInformation(provider.getGigyaAccount(cookieValue));
		
		String accountId = provider.parseKamereonAccountId(provider.getKamereonAccountId(jwtToken,personId));
		String kamereonToken = provider.parseKamereonToken(provider.getKamereonToken(jwtToken, accountId));
		
		String vin = provider.parseVehicleList(provider.getVehicleList(jwtToken, accountId, kamereonToken));
		
		assertTrue(provider.enableAc(jwtToken, accountId, kamereonToken,vin));
	}
	
	@Test
	void testEnableAcWithInvalidData() {
		assertFalse(provider.enableAc("","","","myVIN"));
	}

	
	
	// private members
	private MyRenaultStatusProvider provider;
	

	
	}
