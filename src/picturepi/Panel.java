package picturepi;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

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
	 * creates a Panel object from the class name
	 * @param panelName
	 * @return
	 */
	static Panel createPanelFromName(final String panelName) {
		Panel panel = null;
		
		try {
			Class<?> panelClass = Class.forName("picturepi."+panelName);
			panel = (Panel) panelClass.getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			log.severe("view panel class not found: "+panelName);
			log.severe(e.getMessage());
		} catch (IllegalAccessException | InstantiationException e) {
			log.severe("unable to instantiate panel class : "+panelName);
			log.severe(e.getMessage());
		} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			log.severe("unable to instantiate panel class : "+panelName);
			log.severe(e.getMessage());
		}
		
		return panel;
	}
	
	/**
	 * sets the panel as active or inactive
	 * active means the panel can be displayed now and the data provider is started, but not necessarily that it
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
	
	/**
	 * forces the provider to do a data update immediately (in the background)
	 */
	void forceUpdate() {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				provider.fetchData();
			}});
		t.start();
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
	private static final long    serialVersionUID = -3111174588868454448L;
	private static final Logger  log = Logger.getLogger( Panel.class.getName() );
	
	protected Provider provider = null;         // data provider for this panel
	protected boolean  isActive = false;        // flags if this panel is currently active or not
}
