package picturepi;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.List;
import java.util.logging.LogManager;

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
		assertFalse(provider.login("dummy", "user")); 
	}
	
	// private members
	private RenaultZoeStatusProvider provider;
}
