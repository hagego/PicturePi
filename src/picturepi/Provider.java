package picturepi;

import java.util.logging.Logger;

/**
 * abstract base class for all data providers
 */
public abstract class Provider implements Runnable {

	/**
	 * @param sleepTimeSeconds time in seconds that the provider thread will sleep
	 *                         between checking for updated data by calling fetchData
	 *                         after it was started. If set to 0, fetchData is only
	 *                         called when started
	 */
	Provider(int sleepTimeSeconds) {
		this.sleepTimeSeconds = sleepTimeSeconds;
	}
	
	/*
	 * sets the panel belonging to this provider
	 */
	protected void setPanel(Panel panel) {
		this.panel = panel;
	}
	
	/**
	 * starts the provider thread
	 */
	void start() {
		if(sleepTimeSeconds==0) {
			// no periodic updates needed
			// call fetchData once
			fetchData();
		}
		else {
			if(thread==null) {
				log.fine("starting provider thread");
				thread = new Thread(this);
				thread.start();
			}
			else {
				log.fine("start called but thread object is not null. Doing nothing");
			}
		}
	}
	
	/**
	 * stops the provider thread
	 */
	void stop() {
		if(sleepTimeSeconds!=0) {
			if(thread!=null) {
				log.fine("stopping provider thread");
				thread.interrupt();
			}
			else {
				log.fine("stop called but thread object is null. Doing nothing");
			}
		}
	}
	
	@Override
	public void run() {
		// start endless loop
		log.fine("Provider thread started for "+panel.getClass());
		while (thread!=null && !thread.isInterrupted()) {
			//log.finest("fetching data for "+panel.getClass());
			fetchData();
			
			try {
				Thread.sleep(sleepTimeSeconds*1000);
			} catch (InterruptedException e) {
				log.fine("Provider thread interrupted for "+panel.getClass());
				thread = null;
			} 
		}
		
	}
	
	/**
	 * sets the provider sleep time
	 * @param sleepTimeSeconds sleep time in seconds
	 */
	protected void setSleepTime(int sleepTimeSeconds) {
		this.sleepTimeSeconds = sleepTimeSeconds;
	}
	
	/*
	 * gets called periodically to update the data
	 */
	protected abstract void fetchData();
	
	// private members
	private static final Logger log = Logger.getLogger( Provider.class.getName() );
	
	private int     sleepTimeSeconds;           // sleep time between refreshing data
	private Thread  thread           = null;    // thread object
	protected Panel panel            = null;    // panel object belonging to this provider
}
