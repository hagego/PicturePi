package picturepi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.Test;

class PanelTest {

	@Test
	void testCreatePanelFromNameWithValidName() {
		assertThat(Panel.createPanelFromName("TextWatchPanel"), is(not(nullValue())));
	}
	
	@Test
	void testCreatePanelFromNameWithInvalidName() {
		assertThat(Panel.createPanelFromName("DummyPanel"), is(nullValue()));
	}
}
