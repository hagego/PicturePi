package picturepi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.LogManager;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import picturepi.SummaryProvider.RouteInformation;

class SummaryProviderTest {

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
		provider = new SummaryProvider();
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "PICTUREPI_TOMTOMAPIKEY", matches = ".*")
	void testGetTomTomApiKey() {
		assertThat(provider.getTomTomApiKey(), is(notNullValue()));
	}
	
	@Test
	void testInvalidRouteId() {
		assertThat(provider.getRouteInformation(10),is(nullValue()));
	}
	
	@Test
	void testEmptyRouteName() {
		assertThat(provider.getRouteInformation(2),is(nullValue()));
	}
	
	@Test
	void testEmptyRouteStart() {
		assertThat(provider.getRouteInformation(3),is(nullValue()));
	}
	
	@Test
	void testInvalidRouteStart() {
		assertThat(provider.getRouteInformation(4),is(nullValue()));
	}
	
	@Test
	void testInvalidRouteStart2() {
		assertThat(provider.getRouteInformation(5),is(nullValue()));
	}
	
	@Test
	void testEmptyRouteEnd() {
		assertThat(provider.getRouteInformation(6),is(nullValue()));
	}
	
	@Test
	void testInvalidRouteEnd() {
		assertThat(provider.getRouteInformation(7),is(nullValue()));
	}
	
	@Test
	void testInvalidRouteEnd2() {
		assertThat(provider.getRouteInformation(8),is(nullValue()));
	}
	
	@Test
	void testInvalidRouteWaypoint() {
		assertThat(provider.getRouteInformation(9),is(nullValue()));
	}
	
	@Test
	void testValidRouteData() {
		RouteInformation routeInformation;
		
		assertThat(routeInformation=provider.getRouteInformation(1),is(notNullValue()));
		assertThat(routeInformation.name,is("Autobahn"));
		assertThat(routeInformation.start.latitude,is(48.634688));
		assertThat(routeInformation.start.longitude,is(9.325942));
		assertThat(routeInformation.end.latitude,is(48.677821));
		assertThat(routeInformation.end.longitude,is(8.981804));
		assertThat(routeInformation.waypoints,hasSize(3));
		assertThat(routeInformation.waypoints.get(0).latitude,is(48.67178));
		assertThat(routeInformation.waypoints.get(0).longitude,is(9.3540));
		assertThat(routeInformation.waypoints.get(1).latitude,is(48.72233));
		assertThat(routeInformation.waypoints.get(1).longitude,is(9.07583));
		assertThat(routeInformation.waypoints.get(2).latitude,is(48.70363));
		assertThat(routeInformation.waypoints.get(2).longitude,is(9.04066));
	}
	
	@Test
	void testBuildUrl() {
		RouteInformation routeInformation;
		assertThat(routeInformation=provider.getRouteInformation(1),is(notNullValue()));
		
		String key = provider.getTomTomApiKey();
		
		if(key==null) {
			key = "";
		}
		
		assertThat(provider.buildUrl(routeInformation, key),is(notNullValue()));
	}
	
	@Test
	void testParseJsonObjectRouteNoAlternative() throws FileNotFoundException {
		FileReader fileReader;
		fileReader = new FileReader("test/picturepi/TomTomQueryResultsNoAlternative.json");
		
		JsonReader reader = Json.createReaderFactory(null).createReader(fileReader);
		JsonObject jsonObject = reader.readObject();
		JsonArray jsonRoutesArray = jsonObject.getJsonArray("routes");
		JsonObject jsonObjectRoute = jsonRoutesArray.getJsonObject(0);

		RouteInformation routeInformation = provider.new RouteInformation();
		
		assertThat(jsonRoutesArray,hasSize(1));
		assertThat(provider.parseJsonRouteData(jsonObjectRoute, routeInformation),is(true));
		assertThat(routeInformation.travelTimeActual,is(1764));
		assertThat(routeInformation.travelTimeNoTraffic,is(1683));
	}
	
	@Test
	void testParseJsonObjectRouteWithAlternative() throws FileNotFoundException {
		FileReader fileReader;
		fileReader = new FileReader("test/picturepi/TomTomQueryResultsWithAlternative.json");
		
		JsonReader reader = Json.createReaderFactory(null).createReader(fileReader);
		JsonObject jsonObject = reader.readObject();
		JsonArray jsonRoutesArray = jsonObject.getJsonArray("routes");
		assertThat(jsonRoutesArray,hasSize(2));
		RouteInformation routeInformation = provider.new RouteInformation();
		
		JsonObject jsonObjectRoute1 = jsonRoutesArray.getJsonObject(0);
		assertThat(provider.parseJsonRouteData(jsonObjectRoute1, routeInformation),is(true));
		assertThat(routeInformation.travelTimeActual,is(2220));
		assertThat(routeInformation.travelTimeNoTraffic,is(2220));
		
		JsonObject jsonObjectRoute2 = jsonRoutesArray.getJsonObject(1);
		assertThat(provider.parseJsonRouteData(jsonObjectRoute2, routeInformation),is(true));
		assertThat(routeInformation.travelTimeActual,is(1691));
		assertThat(routeInformation.travelTimeNoTraffic,is(1683));
	}
	
	@Test
	void testFetchTrafficInformation() {
		RouteInformation routeInformation;
		assertThat(routeInformation=provider.getRouteInformation(1),is(notNullValue()));
		
		assertThat(provider.fetchTrafficInformation(routeInformation),is(true));
	}

	//
	// member data
	//
	SummaryProvider provider = null;
}
