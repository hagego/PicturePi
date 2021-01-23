package picturepi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PanelTest {
	
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

	@Test
	void testCreatePanelFromNameWithValidName() {
		assertThat(Panel.createPanelFromName("TextWatchPanel",null), is(not(nullValue())));
	}
	
	@Test
	void testCreatePanelFromNameWithValidNameButInvalidId() {
		assertThat(Panel.createPanelFromName("TextWatchPanel","id"), is(nullValue()));
	}
	
	@Test
	void testCreatePanelFromNameWithValidNameAndValidId() {
		assertThat(Panel.createPanelFromName("PhoneFinderPanel","id"), is(not(nullValue())));
	}
	
	@Test
	void testCreatePanelFromNameWithInvalidName() {
		assertThat(Panel.createPanelFromName("DummyPanel",null), is(nullValue()));
	}
}
