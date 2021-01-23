package picturepi;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.InputStream;
import java.util.List;
import java.util.logging.LogManager;



class EfaDepartureMonitorProviderTest {

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
		provider = new EfaDepartureMonitorProvider();
	}

	@Test
	void testEfaDepartureMonitorProvider() {
		new EfaDepartureMonitorProvider(); 
		assertTrue(true);
	}
	
	@Test
	void testGetResponseStreamWithInvalidServer() {
		assertThat(provider.getHttpRequestResponseStream("http://my.dummy.server", ""), is(nullValue()));
	}
	
	@Test
	void testGetResponseStreamWithValidData() {
		assertThat(provider.getHttpRequestResponseStream("http://efastatic.vvs.de/OpenVVSDay", "cafe stoll"), is(notNullValue()));
	}
	
	@Test
	void testGetDepartureListWithValidData() {
		InputStream inputStream = provider.getHttpRequestResponseStream("http://efastatic.vvs.de/OpenVVSDay", "cafe stoll");
		List<EfaDepartureMonitorProvider.DepartureInformation> departureList = provider.getDepartureList(inputStream);
		
		assertThat(departureList,is(notNullValue()));
		assertThat(departureList,not(emptyIterable()));
	}
	
	//
	EfaDepartureMonitorProvider provider;
}
