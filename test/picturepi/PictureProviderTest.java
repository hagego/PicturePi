package picturepi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.LocalDate;
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
		provider.createPictureDateList();
	}
	
	@Test
	void testCreateDatePictureList() {
		// total number of pictures in test directory is 7, but one has no date
		assertThat(provider.getPictureDateList().size(),is(6));
	}

	@Test
	void testGetPicturesOfDayWithResults() {
		// 1 picture taken on 19890525
		assertThat(provider.getPicturesOfDay(LocalDate.of(1989,5,25)).size(),is(1));
	}

	@Test
	void testGetPicturesOfDayWithoutResults() {
		// no pictures taken on 2000.12.15
		assertThat(provider.getPicturesOfDay(LocalDate.of(2000, 12, 16)).size(),is(0));
	}

	@Test
	void testGetPicturesOfMonthWithResults() {
		// 2 pictures taken in May but not on 25th
		assertThat(provider.getPicturesOfMonth(LocalDate.of(1989,5,25)).size(),is(2));
	}

	@Test
	void testGetPicturesOfMonthWithoutResults() {
		// no picture taken in 198906
		assertThat(provider.getPicturesOfMonth(LocalDate.of(1989,6,25)).size(),is(0));
	}
}


