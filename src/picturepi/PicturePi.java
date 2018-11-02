package picturepi;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import picturepi.Configuration.ViewData;


/**
 * Main application class for PicturePi
 */
public class PicturePi {
	
	public static void main(String[] args) {
		
		// read logging configuration file.
		String configDir = "";
		if(System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")) {
			// on windows development, expect configuration data in conf directory of project
			configDir = "conf/";
			log.info("PicturePi started, running on Windows");
		}
		else {
			// on Linux/Raspberry, expect configuration data in /etc/picturepi
			configDir = "/etc/picturepi/";
			log.info("PicturePi started, running on Linux/Rasperry");
		}
		
		// force reading of logger / handler configuration file
		String configFileLogging = configDir+"picturepi.logging";
		System.setProperty( "java.util.logging.config.file", configFileLogging );
		try {
			LogManager.getLogManager().readConfiguration();
		}
		catch ( Exception e ) {
			// unable to read logging configuration file
			e.printStackTrace();
			
			return;
		}
		
		log.info("Logging configuration was read from "+configFileLogging);
		
		// prepare reading of .ini file with configuration data
		String configFile = configDir+"picturepi.ini";
		Configuration.getConfiguration().readConfigurationFile(configFile);
		
		// create main window
		MainWindow mainWindow = new MainWindow();
		try {
			EventQueue.invokeAndWait(mainWindow);
		} catch (InvocationTargetException | InterruptedException e) {
			log.severe("Unable to create Main Window");
			log.severe(e.getMessage());
			
			return;
		} 

		
		List<ViewData> viewDataList = Configuration.getConfiguration().getViewDataList();
		Iterator<ViewData> viewIterator = viewDataList.iterator();
		
		ViewData lastView = viewIterator.next();
		ViewData nextView;
		
		// start scheduler for panel display
		do {
			try {
				// find the next view to display. Loop until either an active view was found
				// or we end up at the same view again (so no active view exists currently)
				do {
					if(!viewIterator.hasNext()) {
						// start at beginning again
						viewIterator = viewDataList.iterator();
					}
					nextView = viewIterator.next();
					
					// ensure that providers of inactive views are stopped
					if(!nextView.isActive() && nextView.panel.isActive()) {
						log.fine("deactivating view "+nextView.name);
						nextView.panel.setActive(false);
					}
				}
				while (!nextView.isActive() && nextView != lastView);
				
				long sleepTime = 60000; // default 1 min
				if(nextView.isActive()) {
					// active view found
					log.fine("activating view "+nextView.name);
					
					nextView.panel.setActive(true);
					mainWindow.setPanel(nextView.panel);
					
					sleepTime = nextView.duration*1000;
				}
				else {
					// no active view found. Sleep a minute and try again
					log.fine("no active view found");
				}
			
				Thread.sleep(sleepTime);
				lastView = nextView;
			} catch (InterruptedException e) {
				log.severe("thread sleep interrupted");
				log.severe(e.getMessage());
			}
		}
		while(true);
		
	}
	
	// private members
	private static final Logger log = Logger.getLogger( PicturePi.class.getName() );
}


