package picturepi;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JPanel;

/**
 * abstract base class for all display panels (screens)
 */
public abstract class Panel extends JPanel {

	/**
	 * constructor
	 */
	protected Panel(Provider provider) {
		super();
		
		this.provider = provider;
		activeViews   = new HashSet<>();
		
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
		
		log.fine("creating panel "+panelName+", id="+id);
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
	 * adds an active view for this panel. If this is the first active view, the data provider gets started
	 * @param viewData view which was activated
	 */
	void addActiveView(Configuration.ViewData viewData) {
		log.finest("Adding active view: "+viewData.name+(viewData.index!=null ? "-"+viewData.index : ""));
		
		if(activeViews.isEmpty()) {
			// first active view - start data provider
			log.fine("first active view added for panel: "+viewData.name);
			if(provider!=null) {
				provider.start();
			}
		}
		activeViews.add(viewData);
	}
	
	/**
	 * removes an active view from this panel. If this was the lsat active view, the data provider gets stopped
	 * @param viewData view which was deactivated
	 */
	void removeActiveView(Configuration.ViewData viewData) {
		log.finest("Removing active view: "+viewData.name+(viewData.index!=null ? "-"+viewData.index : ""));
		
		if( activeViews.remove(viewData)==false ) {
			log.finest("removeActiveView called, but view was not active: ");
		}
		else {
			if(activeViews.isEmpty()) {
				log.fine("Last active view removed - stopping provider: "+viewData.name);
				if(provider!=null) {
					provider.stop();
				}
			}
		}
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
	 * @return the associated provider or null if no provider has been set (yet)
	 */
	Provider getProvider() {
		return provider;
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
	
	protected Provider                    provider = null;   // data provider for this panel
	private   Set<Configuration.ViewData> activeViews;       // set with currently active views for this panel
	protected boolean  isActive = false;        // flags if this panel is currently active or not
	
	protected String   id       = null;         // optional, custom string ID that can be set in config file
}
