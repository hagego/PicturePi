package picturepi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.Test;

class PanelTest {

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
