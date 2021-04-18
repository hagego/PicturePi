package picturepi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.util.logging.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PictureProviderTest {
	
	private PictureProvider provider;
	
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
		provider = new PictureProvider();
	}

	@Test
	void testGetNationalGeographicPictureOfTheDayUrl() {
		assertThat(provider.getNationalGeographicPictureOfTheDayUrl(), is(not(nullValue())));
	}
	
	@Test
	void testGetNationalGeographicPictureOfTheDay() {
		assertThat(provider.downloadNationalGeographicPictureOfTheDay(), is(not(nullValue())));
	}
	
	@Test
	void testCreatePictureList() {
		assertThat(provider.createPictureList().size(),is(not(0)));
	}
}
