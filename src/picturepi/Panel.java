package picturepi;

import javax.swing.JPanel;

/**
 * abstract base class for all display panels (screens)
 */
public abstract class Panel extends JPanel {

	/**
	 * constructor
	 */
	Panel(Provider provider) {
		super();
		
		this.provider = provider;
		provider.setPanel(this);
	}
	
	/**
	 * sets the panel as active or inactive
	 * active means the panel can be displayed now, but not necessarily that it
	 * is currently being displayed
	 * @param isActive
	 */
	void setActive(boolean isActive) {
		if(provider!=null) {
			if(isActive) {
				provider.start();
				this.isActive = true;
			}
			else {
				provider.stop();
				this.isActive = false;
			}
		}
	}
	
	/**
	 * returns if this panel is currently active or not
	 * active means the panel can be displayed now, but not necessarily that it
	 * is currently being displayed
	 * @return returns if this panel is currently active or not
	 */
	boolean isActive() {
		return isActive;
	}
	
	//
	// private members
	//
	private static final long serialVersionUID = -3111174588868454448L;
	
	protected Provider provider = null;    // data provider for this panel
	protected boolean  isActive = false;   // flags if this panel is currently active or not
}