package picturepi;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import io.flic.fliclib.javaclient.Bdaddr;
import io.flic.fliclib.javaclient.ButtonConnectionChannel;
import io.flic.fliclib.javaclient.FlicClient;
import io.flic.fliclib.javaclient.enums.ClickType;
import io.flic.fliclib.javaclient.enums.ConnectionStatus;
import io.flic.fliclib.javaclient.enums.DisconnectReason;
import io.flic.fliclib.javaclient.enums.LatencyMode;
import io.flic.fliclib.javaclient.enums.RemovedReason;
import picturepi.Configuration.ViewData;


/**
 * Main application class for PicturePi
 */
public class PicturePi extends ButtonConnectionChannel.Callbacks implements IMqttMessageListener,Runnable {
	
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
		
		// start the scheduler as different thread
		Thread t = new Thread(picturePi);
		picturePi.setSchedulerThread(t);
		t.start();
		
	}

	/**
	 * private constructor
	 */
	private PicturePi() {
		gpioController  = Configuration.getConfiguration().isRunningOnRaspberry() ? GpioFactory.getInstance() : null;
		
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
			log.fine("Provisioning done");
		}
		else {
			gpioProjectorPower     = null;
		}
		
		enableDisplay(false);
		
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
				log.fine("PIR motion sensor initial state is high");
				motionDetected       = true;
				motionDetectedPeriod = true;
				
				// initialize display timer
				displayOnCounter = Configuration.getConfiguration().getValue("screen", "motionDetectedOnTime", 60)*1000;
    			log.fine("initializing display timer to "+displayOnCounter+" ms");
			}
			else {
				log.fine("PIR motion sensor initial state is low");
				motionDetected       = false;
				motionDetectedPeriod = false;
			}
			
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
	        			displayOnCounter = Configuration.getConfiguration().getValue("screen", "motionDetectedOnTime", 60)*1000;
	        			log.fine("restarting display timer to "+displayOnCounter+" ms");
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
		initializeFlicBluetoothButtons();
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
	}
	
	/**
	 * creates the button2Panel map from the configuration file
	 */
	private void initializeFlicBluetoothButtons() {
		log.config("Creating bluetooth button 2 panel map from config file");
		
		// read flicd bluetooth button 2 panel mapping and create panel objects
		log.config("creating panel objects for flicd buttons");
		buttonPanelList = Configuration.getConfiguration().getButtonViewList();
		
		for(Configuration.ButtonClickViewData buttonViewData : buttonPanelList) {
			buttonViewData.panel = Panel.createPanelFromName(buttonViewData.viewName,buttonViewData.id);
		}
		
		// start connection to flicd
	    try {
	    	log.info("creating connection to flicd");
			FlicClient flicCLient = new FlicClient("127.0.0.1");
			
			// register all buttons found in configuration file
			// buttons might exist multiple times in the list, register each only one
			Set<String> baddrStringSet = new HashSet<String>();
			for(Configuration.ButtonClickViewData buttonViewData:buttonPanelList) {
				if(!baddrStringSet.contains(buttonViewData.buttonAddress)) {
					baddrStringSet.add(buttonViewData.buttonAddress);
					
					log.fine("registering bluetooth button "+buttonViewData.buttonAddress);
					ButtonConnectionChannel button = new ButtonConnectionChannel(new Bdaddr(buttonViewData.buttonAddress), LatencyMode.NormalLatency, (short)5, this);
					flicCLient.addConnectionChannel(button);
				}
			}
			
			
			
			log.info("entering flic event loop");
			
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						flicCLient.handleEvents();
					} catch (IOException e) {
						log.severe("Exception during flic event loop");
						log.severe(e.getMessage());
					}
				};
			});
			t.start();
		} catch (IOException e) {
			log.severe("Unable to connect to flicd");
			log.severe(e.getMessage());
		}
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
			}
		}
		else {
			log.warning("No panel specified for motion detected");
		}

		// set panels active to start provider threads
		log.config("starting provider threads on active views");
		Configuration.getConfiguration().getViewDataList()
			.stream()
			.filter(viewData -> viewData.isActive())
			.filter(viewData -> viewData.panel!=null)
			.forEach(viewData -> viewData.panel.setActive(true));
				
		List<ViewData> viewDataList = Configuration.getConfiguration().getViewDataList();
		Iterator<ViewData> viewIterator = viewDataList.iterator();
		
		ViewData lastView = viewIterator.next();
		ViewData nextView;
		
		scheduledViewActive = false;
		do {
			try {
				long sleepTime = SLEEP_TIME;
				
				if(buttonClicked.get()) {
					enableDisplay(true);
					log.fine("activating panel "+buttonClickedPanel.getClass().toString());
					buttonClickedPanel.forceUpdate();
					mainWindow.setPanel(buttonClickedPanel);
					
					sleepTime = BUTTON_CLICKED_SLEEP_TIME;
					buttonClicked.set(false);
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
						
						// ensure that providers of inactive views are stopped
						if(!nextView.isActive() && nextView.panel!=null && nextView.panel.isActive() && nextView.panel!=motionDetectedPanel) {
							log.fine("de-activating view "+nextView.name);
							nextView.panel.setActive(false);
						}
						
						// ensure that providers get started if view just gets active again
						if(nextView.isActive() && nextView.panel!=null && !nextView.panel.isActive()) {
							// panel just got activated again. re-start provider.
							log.fine("re-activating view "+nextView.name);
							nextView.panel.setActive(true);
						}
					}
					while ((nextView.panel==null || !nextView.isActive() || !nextView.panel.hasData()) && nextView != lastView);
					
					if(nextView.isActive() && nextView.panel!=null && nextView.panel.hasData()) {
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
						
						log.fine("activating view "+nextView.name);
						mainWindow.setPanel(nextView.panel);
						
						sleepTime = nextView.duration*1000;
					}
					else {
						// no active view found. Sleep a minute and try again
						log.fine("no active view found");
						if(scheduledViewActive) {
							// disable display
							enableDisplay(false);
							scheduledViewActive = false;
						}
						if(screenType==ScreenType.PROJECTOR) {
							// on projector, activate motion detected panel (but don't enable display yet)
							motionDetectedPanel.setActive(true);
							mainWindow.setPanel(motionDetectedPanel);
						}
					}
					lastView = nextView;
				}
				
				if(scheduledViewActive || motionDetectedPeriod) {
					adjustBrightness();
				}
			
				Thread.sleep(sleepTime);
				
				if(motionDetectedPeriod) {
					// we are in a motion detected period. Check if it is time to disable again
					if(motionDetected==false) {
						displayOnCounter -= sleepTime;
					}
					if(displayOnCounter<=0) {
						// motion detected period end
						log.fine("motion detected period end");
						motionDetectedPeriod = false;
						
						if(screenType==ScreenType.DISPLAY) {
							enableDisplay(motionDetectedPeriod);
						}
						
						if(screenType==ScreenType.PROJECTOR) {
							if(!scheduledViewActive) {
								// disable motion detected panel again
								motionDetectedPanel.setActive(false);
								enableDisplay(false);
							}
						}
					}
				}
			} catch (InterruptedException e) {
				if(buttonClicked.get()) {
					log.fine("thread sleep interrupted due to bluetooth button click");
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
			gpioOutScreenEnable.setState(enable ? PinState.HIGH : PinState.LOW);
		}
		
		displayEnabled = enable;
	}
	
	//
	// flicd ButtonConnectionChannel.Callbacks methods
	//
	@Override
	public void onButtonSingleOrDoubleClickOrHold(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) {
		log.fine("Button callback received: clicked="+clickType.toString()+ " wasQueued="+wasQueued);
		
		if(wasQueued==false) {
			log.fine("button click received, button="+channel.getBdaddr());
			
			for(Configuration.ButtonClickViewData buttonViewData:buttonPanelList) {
				if(buttonViewData.buttonAddress.equals(channel.getBdaddr().toString()) && buttonViewData.panel!=null) {
					if( (clickType==ClickType.ButtonSingleClick && buttonViewData.clicks==1)
							|| (clickType==ClickType.ButtonDoubleClick && buttonViewData.clicks==2)
							|| (clickType==ClickType.ButtonHold && buttonViewData.clicks==0) ) {
						buttonClickedPanel = buttonViewData.panel;
						buttonClicked.set(true);
						log.fine("activating view "+buttonClickedPanel.getClass().toString()+" for bluetooth button "+channel.getBdaddr().toString());
						
						// stopping regular scheduling
						schedulerThread.interrupt();
					}
				}
			}
		}
	}
	
	@Override
	public void onConnectionStatusChanged(ButtonConnectionChannel channel,ConnectionStatus connectionStatus,DisconnectReason disconnectReason) {
		log.finest("onStatusChanged received: connectionStatus="+connectionStatus+" diconnectReason="+disconnectReason);
	}
	
	@Override
	public void onRemoved(ButtonConnectionChannel channel,RemovedReason removedReason) {
		log.finest("onRemoved callback received. removedReason="+removedReason);
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("MQTT message arrived: topic="+topic+" content="+message);
		
		// enter motion detected period, start counter
		motionDetectedPeriod = true;
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

	//
	// private members
	//
	private static final Logger log = Logger.getLogger( PicturePi.class.getName() );
	
	private MainWindow mainWindow;
	
	private enum ScreenType {DISPLAY,PROJECTOR};                // possible screen types
	private ScreenType screenType = null;                       // actual screen type
	
	private I2CBus 				 bus                   = null;
	private boolean              displayEnabled        = true;   // tracks if display is currently enabled. Must have initial value of true
	private boolean              scheduledViewActive   = false;  // tracks if a view is active based on time schedule

	private boolean              motionDetected        = false;  // reflects if motion sensor currently detects motion
	private boolean              motionDetectedPeriod  = false;  // reflects if we are in a motion detected period or not
	private int                  displayOnCounter      = 0;      // ms counter to disable projector again after motion detection
	private Panel                motionDetectedPanel   = null;   // Panel to display in case of motion detection
	
	private Map<String,Panel>    viewName2panelMap     = null;   // maps view names to panel objects
	
	private List<Configuration.ButtonClickViewData>   buttonPanelList  = null;   // maps bluetooth buttons to panels to be displayed
	AtomicBoolean                buttonClicked         = new AtomicBoolean(false);  // flag to indicate if a bluetooth button was clicked
	Panel                        buttonClickedPanel    = null;   // panel to display after most recent bluetooth button click
	static final int             BUTTON_CLICKED_SLEEP_TIME = 15000;  // sleep time after bluetooth button was clicked                    
	
	private Thread               schedulerThread       = null;   // thread object that is running the scheduler
	

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



