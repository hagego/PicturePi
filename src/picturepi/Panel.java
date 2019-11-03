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
	 * sets the Thread object that is running the scheduler
	 * @param thread thread object running the scheduler
	 */
	void setSchedulerThread(Thread thread) {
		provider.setSchedulerThread(thread);
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
	
	/**
	 * sets a dark foreground color
	 */
	abstract void setColorDark();
	
	/**
	 * sets a bright foreground color
	 */
	abstract void setColorBright();
	
	/**
	 * @return if this panel has data to display
	 */
	abstract boolean hasData();
	
	//
	// private members
	//
	private static final long serialVersionUID = -3111174588868454448L;
	
	protected Provider provider = null;         // data provider for this panel
	protected boolean  isActive = false;        // flags if this panel is currently active or not
}
