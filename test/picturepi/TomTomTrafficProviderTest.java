package picturepi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.io.StringReader;
import java.util.logging.LogManager;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class TomTomTrafficProviderTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		Configuration.getConfiguration().readConfigurationFile("conf/picturepitest.ini");
		System.setProperty( "java.util.logging.config.file", "conf/picturepitest.logging" );
		
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
		provider = new TomTomTrafficProvider();
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_TOMTOMAPIKEY", matches = ".*")
	void testGetApiKey() {
		assertThat(provider.getApiKey(), is(notNullValue()));
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_TOMTOMAPIKEY", matches = ".*")
	void testBuildUrl() {
		assertThat(provider.buildUrl(), is(notNullValue()));
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_TOMTOMAPIKEY", matches = ".*")
	void testGetRoutingData() {
		assertThat(provider.getRoutingData(provider.buildUrl()), is(notNullValue()));
	}
	
	@Test
	void testParseGuidanceFromFile() {
		JsonObject guidance = Json.createReaderFactory(null).createReader(new StringReader(responseStringGuidance)).readObject();
		assertThat(provider.parseGuidance(guidance), not(emptyIterable()));
	}
	
	@Test
	void testParseRouteDataFromFile() {
		JsonObject jsonRouteData = Json.createReaderFactory(null).createReader(new StringReader(responseStringRouteData)).readObject();
		TomTomTrafficProvider.RouteData routeData = provider.parseRouteData(jsonRouteData);
		assertThat(routeData,is(notNullValue()));
		assertThat(routeData.length,greaterThan(0));
		assertThat(routeData.duration,greaterThan(0));
		assertThat(routeData.instructions,not(emptyIterable()));
	}
	
	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_TOMTOMAPIKEY", matches = ".*")
	void parseRoutingDataFromServer() {
		String url = provider.buildUrl();
		assertThat(url, is(notNullValue()));
		JsonObject jsonRoutingData = provider.getRoutingData(url);
		assertThat(jsonRoutingData,is(notNullValue()));
		TomTomTrafficProvider.RouteData routeData = provider.parseRoutingData(jsonRoutingData);
		assertThat(routeData,is(notNullValue()));
		assertThat(routeData.length,greaterThan(0));
		assertThat(routeData.duration,greaterThan(0));
		assertThat(routeData.instructions,not(emptyIterable()));
	}
	

	//
	// member data
	//
	TomTomTrafficProvider provider = null;
	
	final String responseStringGuidance =
			"{\r\n" + 
			"        \"instructions\": [\r\n" + 
			"          {\r\n" + 
			"            \"routeOffsetInMeters\": 0,\r\n" + 
			"            \"travelTimeInSeconds\": 0,\r\n" + 
			"            \"point\": {\r\n" + 
			"              \"latitude\": 52.50931,\r\n" + 
			"              \"longitude\": 13.42937\r\n" + 
			"            },\r\n" + 
			"            \"instructionType\": \"LOCATION_DEPARTURE\",\r\n" + 
			"            \"street\": \"An der Schillingbrücke\",\r\n" + 
			"            \"countryCode\":\"DEU\",\r\n" + 
			"            \"possibleCombineWithNext\": false,\r\n" + 
			"            \"drivingSide\": \"RIGHT\",\r\n" + 
			"            \"maneuver\": \"DEPART\",\r\n" + 
			"            \"message\": \"Leave from An der Schillingbrücke\"\r\n" + 
			"          }\r\n" +
			"        ],\r\n" + 
			"        \"instructionGroups\": [\r\n" + 
			"          {\r\n" + 
			"            \"firstInstructionIndex\": 0,\r\n" + 
			"            \"lastInstructionIndex\": 5,\r\n" + 
			"            \"groupLengthInMeters\": 4567,\r\n" + 
			"            \"groupMessage\": \"Leave from An der Schillingbrücke and continue to A1/E35\"\r\n" + 
			"          }\r\n" + 
			"        ]\r\n" + 
			"}\r\n";
	
	String responseStringRouteData = 
			"    {\r\n" + 
			"      \"summary\": {\r\n" + 
			"        \"lengthInMeters\": 1147,\r\n" + 
			"        \"travelTimeInSeconds\": 157,\r\n" + 
			"        \"trafficDelayInSeconds\": 0,\r\n" + 
			"        \"departureTime\": \"2015-04-02T15:01:57+02:00\",\r\n" + 
			"        \"arrivalTime\": \"2015-04-02T15:04:34+02:00\",\r\n" + 
			"        \"noTrafficTravelTimeInSeconds\": 120,\r\n" + 
			"        \"historicTrafficTravelTimeInSeconds\": 157,\r\n" + 
			"        \"liveTrafficIncidentsTravelTimeInSeconds\": 161,\r\n" + 
			"        \"fuelConsumptionInLiters\": 0.0155,\r\n" + 
			"        \"deviationDistance\": 1735,\r\n" + 
			"        \"deviationTime\": 127,\r\n" + 
			"        \"deviationPoint\": {\r\n" + 
			"          \"latitude\": 52.50904,\r\n" + 
			"          \"longitude\": 13.42912\r\n" + 
			"        }\r\n" + 
			"      },\r\n" + 
			"      \"legs\": [\r\n" + 
			"        {\r\n" + 
			"          \"summary\": {\r\n" + 
			"            \"lengthInMeters\": 108,\r\n" + 
			"            \"travelTimeInSeconds\": 11,\r\n" + 
			"            \"trafficDelayInSeconds\": 0,\r\n" + 
			"            \"departureTime\": \"2015-04-02T15:01:57+02:00\",\r\n" + 
			"            \"arrivalTime\": \"2015-04-02T15:02:07+02:00\",\r\n" + 
			"            \"noTrafficTravelTimeInSeconds\": 10,\r\n" + 
			"            \"historicTrafficTravelTimeInSeconds\": 11,\r\n" + 
			"            \"liveTrafficIncidentsTravelTimeInSeconds\": 13,\r\n" + 
			"            \"fuelConsumptionInLiters\": 0.01\r\n" + 
			"          },\r\n" + 
			"          \"points\": [\r\n" + 
			"            {\r\n" + 
			"              \"latitude\": 52.50931,\r\n" + 
			"              \"longitude\": 13.42937\r\n" + 
			"            },\r\n" + 
			"            {\r\n" + 
			"              \"latitude\": 52.50904,\r\n" + 
			"              \"longitude\": 13.42912\r\n" + 
			"            }\r\n" + 
			"          ]\r\n" + 
			"        }\r\n" + 
			"      ],\r\n" + 
			"      \"sections\": [\r\n" + 
			"        {\r\n" + 
			"          \"startPointIndex\": 0,\r\n" + 
			"          \"endPointIndex\": 3,\r\n" + 
			"          \"sectionType\": \"TRAVEL_MODE\",\r\n" + 
			"          \"travelMode\": \"other\"\r\n" + 
			"        },\r\n" + 
			"        {\r\n" + 
			"          \"startPointIndex\": 3,\r\n" + 
			"          \"endPointIndex\": 7,\r\n" + 
			"          \"sectionType\": \"TRAVEL_MODE\",\r\n" + 
			"          \"travelMode\": \"car\"\r\n" + 
			"        },\r\n" + 
			"        {\r\n" + 
			"          \"startPointIndex\": 2,\r\n" + 
			"          \"endPointIndex\": 5,\r\n" + 
			"          \"sectionType\": \"TOLL_ROAD\"\r\n" + 
			"        },\r\n" + 
			"        {\r\n" + 
			"          \"startPointIndex\": 3,\r\n" + 
			"          \"endPointIndex\": 4,\r\n" + 
			"          \"sectionType\": \"TUNNEL\"\r\n" + 
			"        },\r\n" + 
			"        {\r\n" + 
			"          \"startPointIndex\": 0,\r\n" + 
			"          \"endPointIndex\": 1,\r\n" + 
			"          \"sectionType\": \"PEDESTRIAN\"\r\n" + 
			"        },\r\n" + 
			"        {\r\n" + 
			"          \"startPointIndex\": 3,\r\n" + 
			"          \"endPointIndex\": 4,\r\n" + 
			"          \"sectionType\": \"TRAFFIC\",\r\n" + 
			"          \"simpleCategory\": \"JAM\",\r\n" + 
			"          \"effectiveSpeedInKmh\": 40,\r\n" + 
			"          \"delayInSeconds\": 158,\r\n" + 
			"          \"magnitudeOfDelay\": 1,\r\n" + 
			"          \"tec\": {\r\n" + 
			"            \"effectCode\": 4,\r\n" + 
			"            \"causes\": [\r\n" + 
			"              {\r\n" + 
			"                \"mainCauseCode\": 1\r\n" + 
			"              },\r\n" + 
			"              {\r\n" + 
			"                \"mainCauseCode\": 26,\r\n" + 
			"                \"subCauseCode\": 2\r\n" + 
			"              }\r\n" + 
			"            ]\r\n" + 
			"          }\r\n" + 
			"        }\r\n" + 
			"      ],\r\n" + 
			"      \"guidance\": {\r\n" + 
			"        \"instructions\": [\r\n" + 
			"          {\r\n" + 
			"            \"routeOffsetInMeters\": 0,\r\n" + 
			"            \"travelTimeInSeconds\": 0,\r\n" + 
			"            \"point\": {\r\n" + 
			"              \"latitude\": 52.50931,\r\n" + 
			"              \"longitude\": 13.42937\r\n" + 
			"            },\r\n" + 
			"            \"instructionType\": \"LOCATION_DEPARTURE\",\r\n" + 
			"            \"street\": \"An der Schillingbrücke\",\r\n" + 
			"            \"countryCode\":\"DEU\",\r\n" + 
			"            \"possibleCombineWithNext\": false,\r\n" + 
			"            \"drivingSide\": \"RIGHT\",\r\n" + 
			"            \"maneuver\": \"DEPART\",\r\n" + 
			"            \"message\": \"Leave from An der Schillingbrücke\"\r\n" + 
			"          }\r\n" + 
			"        ],\r\n" + 
			"        \"instructionGroups\": [\r\n" + 
			"          {\r\n" + 
			"            \"firstInstructionIndex\": 0,\r\n" + 
			"            \"lastInstructionIndex\": 5,\r\n" + 
			"            \"groupLengthInMeters\": 4567,\r\n" + 
			"            \"groupMessage\": \"Leave from An der Schillingbrücke and continue to A1/E35\"\r\n" + 
			"          }\r\n" + 
			"        ]\r\n" + 
			"      }\r\n" + 
			"    }\r\n"; 
}
