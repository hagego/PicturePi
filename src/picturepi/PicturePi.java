package picturepi;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
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
		if(Configuration.getConfiguration().isRunningOnRaspberry() == false) {
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
		if( Configuration.getConfiguration().readConfigurationFile(configFile) == false) {
			// configuration file could  not be read - terminate
			log.severe("Unable to load configuration file - terminating application");
			
			return;
		}
		
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
		if(picturePi.screenType == null) {
			log.severe("No valid screen type configured. Exiting...");
			return;
		}
		
		picturePi.runScheduler();
	}

	/**
	 * private constructor
	 */
	private PicturePi() {
		motionDetectedOnTime  = Configuration.getConfiguration().getValue("screen", "motionDetectedOnTime", 60);
		motionDetectedCounter = motionDetectedOnTime*1000;
		
		gpioController  = Configuration.getConfiguration().isRunningOnRaspberry() ? GpioFactory.getInstance() : null;
		
		// configure motion detection: local PIR sensor or remote thru MQTT
		String motionDetectionTopic = Configuration.getConfiguration().getValue("screen", "motionDetectionMqttTopic", null);
		if(motionDetectionTopic != null) {
			// subscribe to MQTT topic to listen for motion detection
			log.config("motion detection is handled thru MQTT message. Topic: "+motionDetectionTopic);
			MqttClient.getMqttClient().subscribe(motionDetectionTopic, this);
		}
		else {
			log.config("no motion detection thru MQTT message.");
		}
		
		int motionDetectionGpio = (int)Configuration.getConfiguration().getValue("screen", "motionDetectionGpio", -1);
		if(Configuration.getConfiguration().isRunningOnRaspberry() && motionDetectionGpio>=0) {
			// enable motion detection thru local GPIO
			log.config("motion detection thru GPIO: "+motionDetectionGpio);
			
			gpioInPIRSensor = gpioController.provisionDigitalInputPin (RaspiPin.getPinByAddress(motionDetectionGpio), PinPullResistance.PULL_DOWN);
			
			// handle PIR motion sensor changes
			if(gpioInPIRSensor.isHigh()) {
				motionDetected = true;
			}
			
			gpioInPIRSensor.addListener(new GpioPinListenerDigital() {
	            @Override
	            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
	                // display pin state on console
	            	if(event.getState()==PinState.LOW)
	            	{
	            		// NO motion detected any more
	            		log.fine("PIR motion sensor: motion cleared");
	            	}
	            	else {
	            		// motion detected
	            		log.fine("PIR motion sensor: motion detected");
	            		
	            		// enter motion detected period, start counter
	            		motionDetected = true;
	            		motionDetectedCounter = motionDetectedOnTime*1000;
	            		
					    enableDisplay(motionDetected);
	            	}
	            }
	        });
		}
		else {
			gpioInPIRSensor = null;
		}
	
		// determine screen type
		String screenTypeString = Configuration.getConfiguration().getValue("screen", "type", null);
		log.config("screen type: "+screenTypeString);
		
		if(screenTypeString.equalsIgnoreCase("display")) {
			// HDMI Display
			log.config("Display is HDMI display. Initializing GPIO pins for display control");
			
			// initialize GPIO pins
			gpioOutScreenEnable = Configuration.getConfiguration().isRunningOnRaspberry() ? gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_00, "screenEnable", PinState.HIGH): null;
			screenType = ScreenType.DISPLAY;
		}
		else {
			gpioOutScreenEnable = null;
		}
		
		if(Configuration.getConfiguration().isRunningOnRaspberry() && screenTypeString.equalsIgnoreCase("projector")) {
			// EVM2000 projector
			log.config("Display is projector. Initializing GPIO pins for projector control");
			gpioProjectorPower = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_02);
			screenType = ScreenType.PROJECTOR;
		}
		else {
			gpioProjectorPower     = null;
		}
	}
	private void runScheduler() {
		// read view data from config file and create panels
		Map<String,Panel> panelMap = new HashMap<String,Panel>();
		for(ViewData viewData:Configuration.getConfiguration().getViewDataList()) {
			if(panelMap.get(viewData.name) == null) {
				// new panel
				log.fine("No panel created yet for "+viewData.name);
				Panel panel = null;
				
				try {
					Class<?> panelClass = Class.forName("picturepi."+viewData.name);
					panel = (Panel) panelClass.newInstance();
				} catch (ClassNotFoundException e) {
					log.severe("view panel class not found: "+viewData.name);
					log.severe(e.getMessage());
				} catch (IllegalAccessException | InstantiationException e) {
					log.severe("unable to instantiate view panel class : "+viewData.name);
					log.severe(e.getMessage());
				}
				
				log.fine("successfully created panel "+viewData.name);
				viewData.panel = panel;
				panelMap.put(viewData.name, panel);
			}
			else {
				log.fine("re-using panel object for "+viewData.name);
				viewData.panel = panelMap.get(viewData.name);
			}
		}
		
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
			log.warning("No panel specified for motion detected");
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
						enableDisplay(true);
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
						enableDisplay(false);
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
							enableDisplay(false);
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
		if(!Configuration.getConfiguration().isRunningOnRaspberry() || screenType!=ScreenType.PROJECTOR) {
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
	
	private synchronized void enableDisplay(boolean enable) {
		if(enable==displayEnabled) {
			// do nothing
			return;
		}
		
		log.fine("changing display enabled state to "+enable);
		
		// code for DSP2000 projector
		if(Configuration.getConfiguration().isRunningOnRaspberry() && screenType==ScreenType.PROJECTOR) {
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
					Thread.sleep(PROJECTOR_BOOT_TIME);
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
		}
		
		// code for HDMI display
		if(Configuration.getConfiguration().isRunningOnRaspberry() && screenType==ScreenType.DISPLAY) {
			gpioOutScreenEnable.setState(enable ? PinState.HIGH : PinState.LOW);
		}
		
		displayEnabled = enable;
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("MQTT message arrived: topic="+topic+" content="+message);
		
		// enter motion detected period, start counter
		motionDetected = true;
		log.fine("(re-)started motion detected period");
		
		if(!scheduledViewActive) {
			// no scheduled view active. Activate special panel for motion detected case
			if(motionDetectedPanel != null) {
				motionDetectedPanel.setActive(true);
				mainWindow.setPanel(motionDetectedPanel);
				
				// enable projector
				enableDisplay(true);
				adjustBrightness();
			}
		}
	}
	
	// private members
	private static final Logger log = Logger.getLogger( PicturePi.class.getName() );
	
	private MainWindow mainWindow;
	
	private enum ScreenType {DISPLAY,PROJECTOR};                // possible screen types
	private ScreenType screenType = null;                       // actual screen type
	
	private I2CBus 				 bus                   = null;
	private boolean              displayEnabled        = false;  // tracks if display is currently enabled
	private int                  motionDetectedCounter = 0;      // ms counter to disable projector again after motion detection
	private boolean              scheduledViewActive   = false;  // tracks if a view is active based on time schedule
	private boolean              motionDetected        = false;  // true if we are in a motion detected period
	private Panel                motionDetectedPanel   = null;   // Panel to display in case of motion detection
	private final int            motionDetectedOnTime;           // time in seconds to keep display on after motion is detected

	// pi4j objects for GPIO
	private final GpioController       gpioController ;        // GPIO controller instance
	private final GpioPinDigitalOutput gpioOutScreenEnable;    // turn screen on/off, P1-11
	private final GpioPinDigitalInput  gpioInPIRSensor;        // PIR motion sensor HC-SR501 input pint
	private final GpioPinDigitalOutput gpioProjectorPower;     // control pin for Projector power enable

	// i2c commands for projector control
	static final byte I2C_OUTPUT_FORMAT[]    = {0x0c, 0x00, 0x00, 0x00, 0x13};
	static final byte I2C_OUTPUT_RASPI[]     = {0x0b, 0x00, 0x00, 0x00, 0x00};
	static final byte I2C_BRIGHTNESS_R[]     = {0x12, 0x00, 0x00, 0x00, 0x01};
	static final byte I2C_BRIGHTNESS_G[]     = {0x13, 0x00, 0x00, 0x00, 0x01};
	static final byte I2C_BRIGHTNESS_B[]     = {0x14, 0x00, 0x00, 0x00, 0x01};
	static final byte I2C_BRIGHTNESS_SET1[]  = {0x3a, 0x00, 0x00, 0x00, 0x01};
	static final byte I2C_BRIGHTNESS_SET2[]  = {0x38, 0x00, 0x00, 0x00, (byte)0xd3};
	
	private byte  projectorBrightnessSetting = 1;  // current value for the brightness setting of the projector

	static final int SLEEP_TIME          = 60000;   // sleep time for view scheduler in ms
	static final int PROJECTOR_BOOT_TIME = 800;     // projector on time after motion detection in ms
}



