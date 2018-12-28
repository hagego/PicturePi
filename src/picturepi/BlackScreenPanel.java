package picturepi;

import java.awt.Color;

/**
 * Panel displaying nothing but a black screen
 */
class BlackScreenPanel extends Panel {

	private static final long serialVersionUID = -8276167364875951889L;

	/**
	 * Constructor
	 */
	public BlackScreenPanel() {
		super(new BlackScreenProvider());
		
		setBackground(Color.BLACK);
	}
	
	@Override
	void setColorDark() {
	}
	
	@Override
	void setColorBright() {
	}
	
}
