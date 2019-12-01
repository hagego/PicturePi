package picturepi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		Configuration.getConfiguration().readConfigurationFile("conf/picturepi.ini");
	}

	@Test
	void testIsRunningOnRaspberry() {
		assertThat(Configuration.getConfiguration().isRunningOnRaspberry(),is(false));
	}
	
	@Test
	void testReadConfigurationFileButDoesNotExist() {
		assertThat(Configuration.getConfiguration().readConfigurationFile("conf/fileDoesNotExist.ini"),is(false));
	}
	
	@Test
	void testReadConfigurationFileDoesExist() {
		assertThat(Configuration.getConfiguration().readConfigurationFile("conf/picturepi.ini"),is(true));
	}
	
	@Test
	void testGetBooleanValueThatExists() {
		assertThat(Configuration.getConfiguration().getValue("unittest", "booleanTest", false),is(true));
	}
	
	@Test
	void testGetBooleanValueThatDoesNotExist() {
		assertThat(Configuration.getConfiguration().getValue("unittest", "booleanTestXX", false), is(false));
	}
	
	@Test
	void testGetIntegerValueThatExists() {
		assertThat(Configuration.getConfiguration().getValue("unittest", "intTest", -1), is(1));
	}

	
	@Test
	void testGetIntegerValueThatDoesNotExist() {
		assertThat(Configuration.getConfiguration().getValue("unittest", "intTestXX", -1), is(-1));
	}
	
	@Test
	void testGetDoubleValueThatExists() {
		assertThat(Configuration.getConfiguration().getValue("unittest", "doubleTest", -1.1), is(1.1));
	}
	
	@Test
	void testGetDoubleValueThatDoesNotExist() {
		assertThat(Configuration.getConfiguration().getValue("unittest", "doubleTestXX", -1.1), is(-1.1));
	}
	
	@Test
	void testGetStringValueThatExists() {
		assertThat(Configuration.getConfiguration().getValue("unittest", "stringTest", "x"), is("exists"));
	}
	
	@Test
	void testGetStringValueThatDoesNotExist() {
		assertThat(Configuration.getConfiguration().getValue("unittest", "stringTestXX", "x"), is("x"));
	}
//	
//	@Test
//	void testParsingViewDataOneSlot() {
//		assertThat(Configuration.getConfiguration().parseViewData("5,06:00-21:40"), hasSize(1));
//	}
//	
//	@Test
//	void testParsingViewDataTwoSlots() {
//		assertThat(Configuration.getConfiguration().parseViewData("5,06:00-09:00,20:00-21:40"), hasSize(2));
//	}
//	
//	@Test
//	void testParsingViewDataErrorNoDuration() {
//		assertThat(Configuration.getConfiguration().parseViewData("06:00-09:00,20:00-21:40"), hasSize(0));
//	}
//
//	@Test
//	void testParsingButtonClickViewDataErrorNoClicks() {
//		assertThat(Configuration.getConfiguration().parseButtonClickViewData("80:e4:da:70:24:d9"), is(nullValue()));
//	}
//	
//	@Test
//	void testParsingButtonClickViewDataErrorWrongClickCount() {
//		assertThat(Configuration.getConfiguration().parseButtonClickViewData("80:e4:da:70:24:d9,3,10"), is(nullValue()));
//	}
//	
//	@Test
//	void testParsingButtonClickViewDataErrorNoDuration() {
//		assertThat(Configuration.getConfiguration().parseButtonClickViewData("80:e4:da:70:24:d9,1"), is(nullValue()));
//	}
//	
//	@Test
//	void testParsingButtonClickViewDataErrorTooMuchData() {
//		assertThat(Configuration.getConfiguration().parseButtonClickViewData("80:e4:da:70:24:d9,1,10,"), is(nullValue()));
//	}
//	
//	@Test
//	void testParsingButtonClickViewDataCorrectData() {
//		assertThat(Configuration.getConfiguration().parseButtonClickViewData("80:e4:da:70:24:d9,1,10"), is(notNullValue()));
//	}
}
