package picturepi;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class PicturePi implements IMqttMessageListener,Runnable,MouseListener {
	
	public static void main(String[] args) {
		
		// read logging configuration file.
		// first check for a local conf directory (as it exists in the development environment)
		String configDir = "conf/";
		File confDir = new File(configDir);
		if( confDir.exists() && confDir.isDirectory()) {
			log.info("Found local conf directory. Will use this for configuration files");
		}
		else {
			configDir = "/etc/picturepi/";
			confDir = new File(configDir);
			if( confDir.exists() && confDir.isDirectory()) {
				log.info("Found conf directory "+configDir+". Will use this for configuration files");
			}
			else {
				log.severe("Unable to find a valid configuration directory. Exiting...");
				
				return;
			}
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
		picturePi.mainWindow = new MainWindow(picturePi);
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
		
		// start the scheduler as different thread
		Thread t = new Thread(picturePi);
		picturePi.setSchedulerThread(t);
		t.start();
	}
	

	/**
	 * private constructor
	 */
	private PicturePi() {
		boolean enableGPIO = Configuration.getConfiguration().getValue("global", "enableGPIO", false);
		gpioController  = (Configuration.getConfiguration().isRunningOnRaspberry() && enableGPIO) ? GpioFactory.getInstance() : null;
		
		// determine screen type from configuration file
		String screenTypeString = Configuration.getConfiguration().getValue("screen", "type", null);
		log.config("screen type: "+screenTypeString);

		if(screenTypeString==null) {
			log.severe("No screen type configured. Exiting...");
			return;
		}
		
		if(screenTypeString.equalsIgnoreCase("display")) {
			// HDMI Display
			screenType = ScreenType.DISPLAY;
		
			// check if display power is controlled thru GPIO
			int powerControlGpio = Configuration.getConfiguration().getValue("screen", "powerControlGpio", -1);
			if(Configuration.getConfiguration().isRunningOnRaspberry() && gpioController!=null && powerControlGpio>=0) {
				gpioOutScreenEnable = gpioController!=null ? gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_00, "screenEnable", PinState.HIGH): null;
				log.config("Display is HDMI display, power controlled thru GPIO.");
			}
			else {
				gpioOutScreenEnable = null;
				log.config("Display is HDMI display, enabling thru wlr-randr");
			}
		}
		else {
			gpioOutScreenEnable = null;
		}
		
		if(screenTypeString.equalsIgnoreCase("projector")) {
			// EVM2000 projector
			log.config("Display is projector. Initializing GPIO pins for projector control");

			if(gpioController!=null) {
				// control pin for projector power enable
				gpioProjectorPower = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_02);
				screenType = ScreenType.PROJECTOR;
				log.fine("Provisioning done");
			}
			else {
				log.severe("Unable to initialize GPIO controller for projector control");
				gpioProjectorPower     = null;
			}
		}

		if(screenType==null) {
			log.severe("No valid screen type configured. Exiting...");
			return;
		}
		
		// enable display
		enableDisplay(false);
		
		// check if brightness can be overridden thru MQTT message
		String brightnessOverrideTopic = Configuration.getConfiguration().getValue("screen", MQTT_TOPIC_BRIGHTNESS_OVERRIDE, null);
		if(brightnessOverrideTopic != null) {
			// subscribe to MQTT topic to listen for brightness override messages
			log.config("brightness can be overridden thru MQTT message. Topic: "+brightnessOverrideTopic);
			MqttClient.getMqttClient().subscribe(brightnessOverrideTopic, this);
		}
		else {
			log.config("no brightness override thru MQTT message.");
		}
		
		// configure motion detection: local PIR sensor or remote thru MQTT
		String motionDetectionTopic = Configuration.getConfiguration().getValue("screen", MQTT_TOPIC_MOTION_DETECTION, null);
		if(motionDetectionTopic != null) {
			// subscribe to MQTT topic to listen for motion detection
			log.config("motion detection is handled thru MQTT message. Topic: "+motionDetectionTopic);
			MqttClient.getMqttClient().subscribe(motionDetectionTopic, this);
		}
		else {
			log.config("no motion detection thru MQTT message.");
		}

		// configure motion detection on period
		motionDetectedOnTime = Configuration.getConfiguration().getValue("screen", "motionDetectionOnTime", 60);
    	log.fine("initializing display timer to "+motionDetectedOnTime+" s");
		displayOnCounter = motionDetectedOnTime*1000;

		// always start with display on
		motionDetected       = false;
		motionDetectedPeriod = true;
		enableDisplay(true);


		
		int motionDetectionGpio = (int)Configuration.getConfiguration().getValue("screen", "motionDetectionGpio", -1);
		if(Configuration.getConfiguration().isRunningOnRaspberry() && gpioController!=null &&  motionDetectionGpio>=0) {
			// enable motion detection thru local GPIO
			log.config("motion detection thru GPIO: "+motionDetectionGpio);
			
			gpioInPIRSensor = gpioController.provisionDigitalInputPin (RaspiPin.getPinByAddress(motionDetectionGpio), PinPullResistance.PULL_DOWN);
			
			gpioInPIRSensor.addListener(new GpioPinListenerDigital() {
	            @Override
	            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
	                // display pin state on console
	            	if(event.getState()==PinState.LOW)
	            	{
	            		// NO motion detected any more
	            		log.fine("PIR motion sensor: motion cleared");
	            		motionDetected =  false;
	            		
	        			// (re-)start timer
						displayOnCounter = motionDetectedOnTime*1000;
	        			log.fine("restarting display timer to "+motionDetectedOnTime+" s");
	            	}
	            	else {
	            		// motion detected
	            		log.fine("PIR motion sensor: motion detected");
	            		motionDetected       = true;
	            		motionDetectedPeriod = true;
	            		
	    				if(screenType==ScreenType.PROJECTOR || scheduledViewActive) {
		            		// enter motion detected period
						    enableDisplay(motionDetectedPeriod);
	    				}
	            	}
	            }
	        });
		}
		else {
			gpioInPIRSensor = null;
		}
		
		createViewName2PanelMap();
	}
		
	/**
	 * sets the thread object that is running the scheduler
	 * @param thread thread object running the scheduler
	 */
	void setSchedulerThread(Thread thread) {
		schedulerThread = thread;
	}
	
	/**
	 * creates the viewName2panelMap from the configuration file
	 */
	private void createViewName2PanelMap() {
		log.config("Creating view name 2 panel map from config file");
		
		// read view data from configuration file and create panels
		viewName2panelMap = new HashMap<String,Panel>();
		for(ViewData viewData:Configuration.getConfiguration().getViewDataList()) {
			if(viewName2panelMap.get(viewData.name) == null) {
				// new panel
				log.fine("No panel created yet for "+viewData.name);
				Panel panel = Panel.createPanelFromName(viewData.name,null);
								
				if(panel!=null) {
					log.fine("successfully created panel "+viewData.name);
					viewData.panel = panel;
					viewName2panelMap.put(viewData.name, panel);
				}
				else {
					log.severe("Unable to create panel "+viewData.panel);
				}
			}
			else {
				log.fine("re-using panel object for "+viewData.name);
				viewData.panel = viewName2panelMap.get(viewData.name);
			}
		}
		
		// call init() on all providers
		log.config("calling init() for all providers");
		Configuration.getConfiguration().getViewDataList().stream().forEach(viewData -> {
			Panel panel = viewData.panel;
			if(panel!=null) {
				Provider provider = panel.provider;
				if(provider!=null) {
					log.fine("calling init for provider of view="+viewData.name);
					provider.init();
				}
			}
		});
	}
		
	
	@Override
	public void run() {
		log.config("scheduler started");
		
		// start motion detected panel
		String motionDetectedPanelName = Configuration.getConfiguration().getValue("screen", "motionDetectedPanel", null);
		if(motionDetectedPanelName!=null) {
			log.config("activating motion detected panel "+motionDetectedPanelName);
			
			// first check if panel already exists
			motionDetectedPanel = null;
			for(ViewData viewData:Configuration.getConfiguration().getViewDataList()) {
				if(viewData.panel!=null) {
					log.fine("view has panel class: "+viewData.panel.getClass().getName());
					if(motionDetectedPanel==null && viewData.panel.getClass().getName().equals("picturepi."+motionDetectedPanelName)) {
						log.fine("motion detected panel already exists in scheduler");
						motionDetectedPanel = viewData.panel;
						
						break;
					}
				}
			}
			
			if(motionDetectedPanel==null) {
				motionDetectedPanel = Panel.createPanelFromName(motionDetectedPanelName,null);
			}

			if(motionDetectedPanel!=null) {
				log.fine("successfully created motion deteced panel "+motionDetectedPanelName);
				
				Configuration.ViewData viewData = new ViewData();
				viewData.name = "motion detection view";
				viewData.panel = motionDetectedPanel;
				motionDetectedPanel.addActiveView(viewData);
			}
		}
		else {
			log.warning("No panel specified to active when motion is detected");
		}

		// check if a panel is specified to be displayed when a touch or mouse click is detected
		initializeInteractivePanel();

		List<ViewData> viewDataList     = Configuration.getConfiguration().getViewDataList();
		Iterator<ViewData> viewIterator = viewDataList.iterator();
		
		ViewData lastView = viewIterator.next();
		ViewData nextView;
		
		scheduledViewActive = false;
		do {
			try {
				long sleepTime = SLEEP_TIME;

				// check if interactive panel shall be activated
				if(activateInteractivePanel.get()) {
					enableDisplay(true);
					log.fine("activating interactive panel");
					mainWindow.setPanel(interactivePanel);
					
					sleepTime = interactivePanelDisplayTime*1000;
					activateInteractivePanel.set(false);
				}
				else {
					// find the next view to display. Loop until either an active view was found
					// or we end up at the same view again (so no active view exists currently)
					do {
						if(!viewIterator.hasNext()) {
							// start at beginning again
							viewIterator = viewDataList.iterator();
						}
						nextView = viewIterator.next();
						
						if(nextView.showNow()) {
							nextView.panel.addActiveView(nextView);
						}
						else {
							nextView.panel.removeActiveView(nextView);
						}
					}
					while ((nextView.showNow()==false || nextView.panel.hasData()==false) && nextView != lastView);
					
					if(nextView.showNow() && nextView.panel.hasData()) {
						// active view found that has data to display or is not active yet
						// (if panel is not active, provider is not started so it cannot have data)
						
						
						if(!scheduledViewActive) {
							// scheduled views become active again (they were inactive before)
							log.info("scheduled view period started");
							scheduledViewActive = true;
							
							if(screenType==ScreenType.PROJECTOR) {
								// in case of projector, enable again (regardless of motion detection)
								enableDisplay(true);
							}
							if(screenType==ScreenType.DISPLAY ) {
								// in case of HDMI, only enable if motion is detected
								enableDisplay(motionDetectedPeriod);
							}
						}
						
						log.finest("activating view "+nextView.name);
						mainWindow.setPanel(nextView.panel);
						
						sleepTime = nextView.duration*1000;
					}
					else {
						// no active view found. Sleep a minute and try again
						log.finest("no active view found");
						if(scheduledViewActive) {
							// disable display
							enableDisplay(false);
							scheduledViewActive = false;
						}
						if(screenType==ScreenType.PROJECTOR) {
							// on projector, activate motion detected panel (but don't enable display yet)
							mainWindow.setPanel(motionDetectedPanel);
						}
					}
					lastView = nextView;
				}
				
				if(scheduledViewActive || motionDetectedPeriod) {
					adjustBrightness();
				}
			
				Thread.sleep(sleepTime);
				
				// handle enable/disable display thru motion detection
				if(motionDetectedPeriod) {
					// we are in a motion detected period. Check if it is time to disable again
					if(motionDetected==false) {
						displayOnCounter -= sleepTime;
						log.fine("motion detected period still active but no active motion detected, remaining time [s]: "+displayOnCounter/1000);
					}
					else {
						log.finest("motion detected period active and active motion detected");
					}

					if(displayOnCounter<=0) {
						// motion detected period end
						log.fine("motion detected period end. Disabling display");
						motionDetectedPeriod = false;
						
						if(screenType==ScreenType.DISPLAY) {
							enableDisplay(false);
						}
						
						if(screenType==ScreenType.PROJECTOR) {
							if(!scheduledViewActive) {
								enableDisplay(false);
							}
						}
					}
				}
			} catch (InterruptedException e) {
				if(activateInteractivePanel.get()) {
					log.fine("thread sleep interrupted due to interactive click");
				}
				else {
					log.severe("thread sleep interrupted");
					log.severe(e.getMessage());
				}
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
			int i2cbus = 0;
			try {
				i2cbus = Configuration.getConfiguration().getValue("global","i2cbus",I2CBus.BUS_3);
				bus = I2CFactory.getInstance(i2cbus);
				log.config("Successfully detected i2c bus "+i2cbus );
			} catch (UnsupportedBusNumberException | IOException e) {
				log.severe("Unable to get I2C Factory for i2cbus  "+i2cbus);
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
				
				
				// start with a simple quadratic relation for bright situations and go linear when it gets darker
				if(brightness>=5) {
					brightness *= brightness;
				}
				
				// if brightness got overridden by MQTT message, use this instead of sensor value
				if(brightnessOverrideValue>0 ) {
					// assume that 100% brightness override value fits to full brightness
					log.fine("overriding brightness with value from MQTT override message: "+brightnessOverrideValue);
					brightness = (int) (brightnessOverrideValue*2.55);
				}
				
				brightness = Integer.min(brightness, 255);
				brightness = Integer.max(brightness, 3);
				
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
	
	/**
	 * enables or disables the display
	 * @param enable true to enable, false to disable
	 */
	private synchronized void enableDisplay(boolean enable) {
		log.finest("enableDisplay called with parameter: "+enable);
		
		if(enable==displayEnabled) {
			// do nothing
			return;
		}
		
		log.fine("changing display enabled state to "+enable);
		
		// code for DSP2000 projector
		if(Configuration.getConfiguration().isRunningOnRaspberry() && screenType==ScreenType.PROJECTOR) {
			if(bus==null) {
				int i2cbus = 0;
				try {
					i2cbus = Configuration.getConfiguration().getValue("global","i2cbus",I2CBus.BUS_3);
					bus = I2CFactory.getInstance(i2cbus);
					log.config("Successfully detected i2c bus "+i2cbus );
				} catch (UnsupportedBusNumberException | IOException e) {
					log.severe("Unable to get I2C Factory for i2cbus  "+i2cbus);
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
			if(gpioOutScreenEnable!=null) {
				// enable/disable display power thru GPIO
				log.fine("enable/display screen power");
				gpioOutScreenEnable.setState(enable ? PinState.HIGH : PinState.LOW);
			}
			else {
				// enable/disable display thru wlr-randr
				String command = "wlr-randr --output DSI-1 --"+(enable ? "on" : "off");
				log.fine("enable/displable display by executing command: "+command);
				try {
					Runtime.getRuntime().exec(command);
				} catch (IOException e) {
					log.severe("Unable to execute command: "+command);
					log.severe(e.getMessage());
				}
			}
			
		}
		
		displayEnabled = enable;
	}

	/**
	 * initializes the handling of the interactive panel
	 */
	private void initializeInteractivePanel() {
		final String interactivePanelName = Configuration.getConfiguration().getValue("screen", INTERACTIVE_PANEL_CONFIG_KEY, null);
		interactivePanelDisplayTime = Configuration.getConfiguration().getValue("screen", INTERACTIVE_PANEL_DISPLAY_TIME_KEY, 30);

		if(interactivePanelName!=null) {
			log.config("configuring interactive panel "+interactivePanelName+", display time [s]=" + interactivePanelDisplayTime);
			
			// first check if panel already exists
			interactivePanel = null;
			for(ViewData viewData:Configuration.getConfiguration().getViewDataList()) {
				if(viewData.panel!=null) {
					log.fine("view has panel class: "+viewData.panel.getClass().getName());
					if(interactivePanel==null && viewData.panel.getClass().getName().equals("picturepi."+interactivePanelName)) {
						log.fine("interactive panel already exists in scheduler");
						interactivePanel = viewData.panel;
						
						break;
					}
				}
			}
			
			if(interactivePanel==null) {
				// panel not found yet. Create it
				interactivePanel = Panel.createPanelFromName(interactivePanelName,null);

				// call init() on the provider
				if(interactivePanel!=null) {
					Provider provider = interactivePanel.getProvider();
					if(provider!=null) {
						log.fine("calling init for provider of interactive panel");
						provider.init();
					}
				}
			}

			if(interactivePanel!=null) {
				log.fine("successfully created interactive panel "+interactivePanelName);
				
				Configuration.ViewData viewData = new ViewData();
				viewData.name = "interactive view";
				viewData.panel = interactivePanel;
				interactivePanel.addActiveView(viewData);
			}
		}
		else {
			log.warning("No panel specified to active when a touch or mouse click is detected");
		}
	}
	

	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("MQTT message arrived: topic="+topic);
		log.finest("MQTT message arrived: topic="+topic+" content="+message);

		String motionDetectionTopic = Configuration.getConfiguration().getValue("screen", MQTT_TOPIC_MOTION_DETECTION, null);
		if(motionDetectionTopic!=null && topic.equals(motionDetectionTopic)) {
			if(new String(message.getPayload()).toLowerCase().equals("on")) {
				// enter motion detected period, start counter
				motionDetectedPeriod = true;
				motionDetected	     = true;
				displayOnCounter	 = motionDetectedOnTime*1000;
				log.fine("motion detected ON received, on-time [s]: "+motionDetectedOnTime);

				if(scheduledViewActive) {
					// enable display
					log.fine("scheduled view active, enabling display");
					enableDisplay(true);
				}
				else {
					// no scheduled view active. Activate special panel for motion detected case
					if(motionDetectedPanel != null) {
						mainWindow.setPanel(motionDetectedPanel);
						enableDisplay(true);
						
						// enable projector
						adjustBrightness();
					}
				}
			}
			else {
				// motion cleared
				log.fine("motion cleared");
				motionDetected       = false;
				//motionDetectedPeriod = false;
			}
		}
		
		if(topic.equals(Configuration.getConfiguration().getValue("screen", MQTT_TOPIC_BRIGHTNESS_OVERRIDE, null))) {
			log.fine("received brightness override message, brigntness="+new String(message.getPayload()));
			
			brightnessOverrideValue = Double.valueOf(new String(message.getPayload()));
		}
	}

	/*
	 * MouseListener interface
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		// mouse click or touch detected. Activate interactive panel
		log.info("Mouse click or touch detected");
		// stop regular scheduling and display interactive panel
		activateInteractivePanel.set(true);

		// enter motion detected period, start counter
		motionDetectedPeriod = true;
		motionDetected	     = true;
		displayOnCounter	 = motionDetectedOnTime*1000;
		log.fine("(re-)started motion detected period, on-time [s]: "+motionDetectedOnTime);

		schedulerThread.interrupt();
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	//
	// private members
	//
	private static final Logger log = Logger.getLogger( PicturePi.class.getName() );
	
	private static final String MQTT_TOPIC_MOTION_DETECTION    = "motionDetectionMqttTopic";
	private static final String MQTT_TOPIC_BRIGHTNESS_OVERRIDE = "brightnessOverrideMqttTopic";
	
	private MainWindow mainWindow;
	
	private enum ScreenType {DISPLAY,PROJECTOR};                // possible screen types
	private ScreenType screenType = null;                       // actual screen type
	
	private I2CBus             bus                     = null;
	private boolean            displayEnabled          = true;   // tracks if display is currently enabled. Must have initial value of true
	private boolean            scheduledViewActive     = false;  // tracks if a view is active based on time schedule
	private double             brightnessOverrideValue = 0.0;    // stores brightness override values received thru MQTT

	// handling of motion detection
	private boolean            motionDetected          = false;  // reflects if motion sensor currently detects motion
	private boolean            motionDetectedPeriod    = false;  // reflects if we are in a motion detected period or not
	private int                motionDetectedOnTime    = 0;     // time in seconds to keep display on after motion detection
	private int                displayOnCounter        = 0;      // ms counter to disable projector again after motion detection
	private Panel              motionDetectedPanel     = null;   // Panel to display in case of motion detection

	// handling of activation of a dedicated panel for user interaction
	private static final String INTERACTIVE_PANEL_CONFIG_KEY        = "interactivePanel";                   // configuration key for interactive panel
	private static final String INTERACTIVE_PANEL_DISPLAY_TIME_KEY  = "interactivePanelDisplayTime";        // configuration key for interactive panel display time
	private AtomicBoolean	    activateInteractivePanel      = new AtomicBoolean(false);  // flag to indicate if an interactive panel is currently active
	private Panel               interactivePanel              = null;                      // panel to display after a touch or mouse click
	private int                 interactivePanelDisplayTime   = 30;                        // time in s how long interactive panel is displayed after touch or mouse click
	
	private Map<String,Panel>  viewName2panelMap       = null;   // maps view names to panel objects                  
	
	private Thread             schedulerThread         = null;   // thread object that is running the scheduler
	

	// pi4j objects for GPIO
	private final GpioController       gpioController ;        // GPIO controller instance
	private GpioPinDigitalOutput gpioOutScreenEnable;    // turn screen on/off, P1-11
	private GpioPinDigitalInput  gpioInPIRSensor;        // PIR motion sensor HC-SR501 input pint
	private GpioPinDigitalOutput gpioProjectorPower;     // control pin for Projector power enable

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
	static final int PROJECTOR_BOOT_TIME = 600;     // projector on time after motion detection in ms
}



