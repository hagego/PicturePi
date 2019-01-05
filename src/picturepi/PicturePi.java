package picturepi;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import picturepi.Configuration.ViewData;


/**
 * Main application class for PicturePi
 */
public class PicturePi implements IMqttMessageListener {
	
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
		
		// create MQTT client (if specified)
		log.info("Creating MQTT client");
		MqttClient.getMqttClient();
		
		PicturePi picturePi = new PicturePi();
		
		// create main window
		picturePi.mainWindow = new MainWindow();
		try {
			EventQueue.invokeAndWait(picturePi.mainWindow);
		} catch (InvocationTargetException | InterruptedException e) {
			log.severe("Unable to create Main Window");
			log.severe(e.getMessage());
			
			return;
		} 
		
		// start scheduler for panel display
		picturePi.runScheduler();
	}
	
	private void runScheduler() {
		// read view data from config file
		Configuration.getConfiguration().readViewData();
		
		// start motion detected panel
		String motionDetectedPanelName = Configuration.getConfiguration().getValue("screen", "motionDetectedPanel", null);
		if(motionDetectedPanelName!=null) {
			log.config("activating motion detected panel "+motionDetectedPanelName);
			
			// first check if panel already exists
			motionDetectedPanel = null;
			for(ViewData viewData:Configuration.getConfiguration().getViewDataList()) {
				log.fine("view has panel class: "+viewData.panel.getClass().getName());
				if(motionDetectedPanel==null && viewData.panel.getClass().getName().equals("picturepi."+motionDetectedPanelName)) {
					log.fine("motion detected panel already exists in scheduler");
					motionDetectedPanel = viewData.panel;
				}
			}
			
			if(motionDetectedPanel==null) {
				// instantiate dedicated panel object
				try {
					Class<?> panelClass = Class.forName("picturepi."+motionDetectedPanelName);
					motionDetectedPanel = (Panel) panelClass.newInstance();
				} catch (ClassNotFoundException e) {
					log.severe("motion deteced panel class not found: "+motionDetectedPanelName);
					log.severe(e.getMessage());
				} catch (IllegalAccessException | InstantiationException e) {
					log.severe("unable to instantiate motion deteced panel class : "+motionDetectedPanelName);
					log.severe(e.getMessage());
				}
			}

			if(motionDetectedPanel!=null) {
				log.fine("successfully created motion deteced panel "+motionDetectedPanelName);
			}
		}
		else {
			log.warning("No view specified for motion detected");
		}

		// start provider threads
		log.config("starting provider threads");
		for(ViewData viewData:Configuration.getConfiguration().getViewDataList()) {
			viewData.panel.provider.start();
		}
				
		List<ViewData> viewDataList = Configuration.getConfiguration().getViewDataList();
		Iterator<ViewData> viewIterator = viewDataList.iterator();
		
		ViewData lastView = viewIterator.next();
		ViewData nextView;
		
		// subscribe to MQTT topic to listen for motion detection
		String motionDetectionTopic = Configuration.getConfiguration().getValue("screen", "mqttTopicMotionDetection", null);
		if(motionDetectionTopic != null) {
			log.info("subscribing for motion detection");
			MqttClient.getMqttClient().subscribe(motionDetectionTopic, this);
		}
		
		scheduledViewActive = false;
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
					
//					// ensure that providers of inactive views are stopped
//					if(!nextView.isActive() && nextView.panel.isActive()) {
//						log.fine("deactivating view "+nextView.name);
//						nextView.panel.setActive(false);
//					}
				}
				while ((!nextView.isActive() || !nextView.panel.hasData()) && nextView != lastView);
				
				long sleepTime = SLEEP_TIME; 
				if(nextView.isActive() && nextView.panel.hasData()) {
					// active view found that has data to display
					if(!scheduledViewActive) {
						// enable projector again
						log.info("Enabling projector again");
						enableProjector(true);
						scheduledViewActive = true;
					}
					log.fine("activating view "+nextView.name);
					
					nextView.panel.setActive(true);
					mainWindow.setPanel(nextView.panel);
					
					sleepTime = nextView.duration*1000;
				}
				else {
					// no active view found. Sleep a minute and try again
					log.fine("no active view found");
					if(scheduledViewActive) {
						log.info("Disabling projector");
						enableProjector(false);
						scheduledViewActive = false;
					}
				}
				
				if(scheduledViewActive || motionDetected) {
					adjustBrightness();
				}
			
				Thread.sleep(sleepTime);
				
				if(motionDetected) {
					// we are in a motion detected period
					motionDetectedCounter -= sleepTime;
					if(motionDetectedCounter<=0) {
						// motion detected period end
						log.fine("motion detected period end");
						motionDetected = false;
						
						if(!scheduledViewActive) {
							// disable motion detected panel again
							motionDetectedPanel.setActive(false);
							enableProjector(false);
						}
					}
				}
				
				lastView = nextView;
			} catch (InterruptedException e) {
				log.severe("thread sleep interrupted");
				log.severe(e.getMessage());
			}
		}
		while(true);
		
	}
	
	/**
	 * measures luminance and adopts projector brightness accordingly
	 */
	private synchronized void adjustBrightness() {
		if(!Configuration.getConfiguration().isRunningOnRaspberry()) {
			return;
		}
		
		if(bus==null) {
			try {
				bus = I2CFactory.getInstance(I2CBus.BUS_3);
			} catch (UnsupportedBusNumberException | IOException e) {
				log.severe("Unable to get I2C Factory: "+e.getMessage());
			}
		}
		
		if(bus != null) {
			try {
				byte[] bytes = new byte[2];
				
				I2CDevice controller = bus.getDevice(0x23);
				controller.write((byte) 0x20);
				int count = controller.read(bytes,0,2);
				if(count!=2) {
					throw new IOException("unexpected amount of bytes read from light sensor: "+count);
				}
				int brightness = bytes[0]*256+bytes[1];
				log.fine("brightness read from sensor: HB="+bytes[0]+" LB="+bytes[1]+" brighntess="+brightness);
				
				// start with a simple quadratic relation
				brightness *= brightness;
				brightness = Integer.min(brightness, 255);
				brightness = Integer.max(brightness, 1);
				
				if((byte)brightness != projectorBrightnessSetting) {
					log.fine("adjusting projector brightness to "+brightness);
					projectorBrightnessSetting = (byte)brightness;
					
					I2C_BRIGHTNESS_R[I2C_BRIGHTNESS_R.length-1] = projectorBrightnessSetting;
					I2C_BRIGHTNESS_G[I2C_BRIGHTNESS_G.length-1] = projectorBrightnessSetting;
					I2C_BRIGHTNESS_B[I2C_BRIGHTNESS_B.length-1] = projectorBrightnessSetting;
					
					controller = bus.getDevice(0x1b);
					
					controller.write(I2C_BRIGHTNESS_R);
					controller.write(I2C_BRIGHTNESS_G);
					controller.write(I2C_BRIGHTNESS_B);
					controller.write(I2C_BRIGHTNESS_SET1);
					controller.write(I2C_BRIGHTNESS_SET2);
					
					// and adjust foreground colors of panels
					List<ViewData> viewDataList = Configuration.getConfiguration().getViewDataList();
					for(ViewData viewData:viewDataList) {
						if(brightness>10) {
							log.fine("adjusting foreground color to bright for panel "+viewData.name);
							viewData.panel.setColorBright();
						}
						else {
							log.fine("adjusting foreground color to dark for panel "+viewData.name);
							viewData.panel.setColorDark();
						}
					}
				}
			} catch (IOException e) {
				log.severe("Exception during i2c write/read: "+e.getMessage());
			}
		}
	}
	
	private synchronized void enableProjector(boolean enable) {
		if(enable==projectorEnabled) {
			// do nothing
			return;
		}
		
		if(Configuration.getConfiguration().isRunningOnRaspberry()) {
			if(gpioProjectorPower==null) {
				log.info("initializing GPIO to control projector supply");
				GpioController gpioController = GpioFactory.getInstance();
				gpioProjectorPower = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_02);
			}
			if(bus==null) {
				try {
					bus = I2CFactory.getInstance(I2CBus.BUS_3);
				} catch (UnsupportedBusNumberException | IOException e) {
					log.severe("Unable to get I2C Factory: "+e.getMessage());
				}
			}
			
			gpioProjectorPower.setState(enable ? PinState.HIGH : PinState.LOW);
			
			if(enable) {
				try {
					// sleep 1s until controller has booted up
					Thread.sleep(800);
				} catch (InterruptedException e) {
					log.severe(e.getMessage());
				}
				
				boolean success = false;
				int counter     = 0;
				while(!success && counter<10) {
					try {
						counter++;
						I2CDevice controller = bus.getDevice(0x1b);
						
						controller.write(I2C_OUTPUT_FORMAT);
						controller.write(I2C_OUTPUT_RASPI);
						success = true;
						log.fine("I2C setup success at execution "+counter);
					} catch (IOException e) {
						log.severe("I2C Exception: "+e.getMessage());
					}
				}

				// always start with projector brightness as small as possible
				I2CDevice controller;
				try {
					controller = bus.getDevice(0x1b);
					
					projectorBrightnessSetting = 1;
					I2C_BRIGHTNESS_R[I2C_BRIGHTNESS_R.length-1] = projectorBrightnessSetting;
					I2C_BRIGHTNESS_G[I2C_BRIGHTNESS_G.length-1] = projectorBrightnessSetting;
					I2C_BRIGHTNESS_B[I2C_BRIGHTNESS_B.length-1] = projectorBrightnessSetting;
					
					controller.write(I2C_BRIGHTNESS_R);
					controller.write(I2C_BRIGHTNESS_G);
					controller.write(I2C_BRIGHTNESS_B);
					controller.write(I2C_BRIGHTNESS_SET1);
					controller.write(I2C_BRIGHTNESS_SET2);
				} catch (IOException e) {
					log.severe("I2C Exception: "+e.getMessage());
				}
			}
			
			projectorEnabled = enable;
		}
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("MQTT message arrived: topic="+topic+" content="+message);
		
		// enter motion detected period, start counter
		motionDetected = true;
		motionDetectedCounter = MOTION_ON_TIME;
		log.fine("(re-)started motion detected period");
		
		if(!scheduledViewActive) {
			// no scheduled view active. Activate special panel for motion detected case
			if(motionDetectedPanel != null) {
				motionDetectedPanel.setActive(true);
				mainWindow.setPanel(motionDetectedPanel);
				
				// enable projector
				enableProjector(true);
				adjustBrightness();
			}
		}
	}
	
	// private members
	private static final Logger log = Logger.getLogger( PicturePi.class.getName() );
	
	private MainWindow mainWindow;
	
	private GpioPinDigitalOutput gpioProjectorPower    = null;   // control pin for Projector power enable
	private I2CBus 				 bus                   = null;
	private boolean              projectorEnabled      = false;  // tracks if projector is currently enabled
	private int                  motionDetectedCounter = 0;      // ms counter to disable projector again after motion detection
	private boolean              scheduledViewActive   = false;  // tracks if a view is active based on time schedule
	private boolean              motionDetected        = false;  // true if we are in a motion detected period
	private Panel                motionDetectedPanel   = null;   // Panel to display in case of motion detection
	
	static final byte I2C_OUTPUT_FORMAT[]    = {0x0c, 0x00, 0x00, 0x00, 0x13};
	static final byte I2C_OUTPUT_RASPI[]     = {0x0b, 0x00, 0x00, 0x00, 0x00};
	static final byte I2C_BRIGHTNESS_R[]     = {0x12, 0x00, 0x00, 0x00, 0x01};
	static final byte I2C_BRIGHTNESS_G[]     = {0x13, 0x00, 0x00, 0x00, 0x01};
	static final byte I2C_BRIGHTNESS_B[]     = {0x14, 0x00, 0x00, 0x00, 0x01};
	static final byte I2C_BRIGHTNESS_SET1[]  = {0x3a, 0x00, 0x00, 0x00, 0x01};
	static final byte I2C_BRIGHTNESS_SET2[]  = {0x38, 0x00, 0x00, 0x00, (byte)0xd3};
	
	private byte                 projectorBrightnessSetting = 1;  // current value for the brightness setting of the projector

	static final int SLEEP_TIME = 60000;       // sleep time for view scheduler in ms
	static final int MOTION_ON_TIME = 600000;  // projector on time after motion detection
}



