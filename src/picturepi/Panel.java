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
	 * @param panelName  panel name
	 * @param id         optional String ID. If specified, panel constructor must have a String parameter
	 * @return           panel object
	 */
	static Panel createPanelFromName(final String panelName,final String id) {
		Panel panel = null;
		
		try {
			Class<?> panelClass = Class.forName("picturepi."+panelName);
			if(id!=null) {
				panel = (Panel) panelClass.getDeclaredConstructor(String.class).newInstance(id);
			}
			else {
				panel = (Panel) panelClass.getDeclaredConstructor().newInstance();
			}
		} catch (ClassNotFoundException e) {
			log.severe("view panel class not found: "+panelName);
			log.severe(e.getMessage());
		} catch (IllegalAccessException e) {
			log.severe("IllegalAccessException: Unable to instantiate panel class : "+panelName);
			log.severe(e.getMessage());
		} catch (InstantiationException e) {
			log.severe("InstantiationException: Unable to instantiate panel class : "+panelName);
			log.severe(e.getMessage());
		} catch (IllegalArgumentException e) {
			log.severe("IllegalArgumentException: Unable to instantiate panel class : "+panelName);
			log.severe(e.getMessage());
		} catch (InvocationTargetException e) {
			log.severe("InvocationTargetException: Unable to instantiate panel class : "+panelName);
			log.severe(e.getMessage());
		} catch (NoSuchMethodException e) {
			log.severe("NoSuchMethodException: Unable to instantiate panel class : "+panelName);
			log.severe(e.getMessage());
		} catch (SecurityException e) {
			log.severe("SecurityException: Unable to instantiate panel class : "+panelName);
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
	
	protected String   id       = null;         // optional, custom string ID that can be set in config file
}
