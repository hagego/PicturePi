package picturepi;

/*
 * Data Provider for the BlackScreen Panel
 * Essentially doing nothing
 */
public class BlackScreenProvider extends Provider {

	BlackScreenProvider() {
		// no updates needed
		super(0);
	}

	@Override
	protected void fetchData() {
		// nothing to be done
	}
}
