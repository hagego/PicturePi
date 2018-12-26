package picturepi;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.ini4j.Ini;

/**
 * Singleton class storing configuration info.
 * This includes settings defined in the .ini file but also other configuration items
 */
class Configuration {

	/*
	 * singleton class - private constructor
	 */
	private Configuration() {
	}
	
	/**
	 * returns the singleton object
	 * @return the Configuration singleton object
	 */
	static Configuration getConfiguration() {
		if(configuration==null) {
			configuration = new Configuration();
		}
		
		return configuration;
	}

	/**
	 * reads the .ini configuration file
	 * @param filename filename 
	 */
	void readConfigurationFile(final String filename) {
		try {
			log.config("reading configuration file "+filename);
			iniFile.load(new FileReader(filename));
		} catch (IOException e) {
			log.severe("Unable to load configuration file "+filename);
			log.severe(e.getMessage());
		}
		
		log.config("configuration file was read successfully");
	}

	/**
	 * reads view data and creates them
	 */
	void readViewData() {
		// read view data
		log.config("reading view data");
		
		// syntax: <Viewname> = <display duration [s]>,<display start in hh:mm>-<display end in hh:mm>
		Ini.Section views= iniFile.get("views");
		for(Map.Entry<String,String> entry: views.entrySet() ) {
			log.config("found view: "+entry.getKey()+"="+entry.getValue());
			
			// parse view settings
			List<ViewData> viewData = parseViewData(entry.getKey(),entry.getValue());
			if(viewData != null) {
				viewDataList.addAll(viewData);
			}
		}
	}
	
	/**
	 * @return if we are running on Raspberry or not
	 */
	boolean isRunningOnRaspberry() {
		boolean runningOnRaspberry = true;
		
		if(System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")) {
			runningOnRaspberry = false;
		}
		
		return runningOnRaspberry;
	}
	
	/**
	 * returns a boolean value from the ini file
	 * @param section          section name
	 * @param key              key name
	 * @param defaultValue     default value that will be returned if the value cannot be found
	 * @return                 value read from ini file or default if not found
	 */
	boolean getValue(String section,String key,boolean defaultValue) {
		Ini.Section iniSection= iniFile.get(section);
		
		if(iniSection!=null) {
			return iniSection.get(key,Boolean.class,defaultValue);
		}
		else {
			log.warning("Section "+section+" not found");
			return defaultValue;
		}
	}
	
	/**
	 * returns an integer value from the ini file
	 * @param section          section name
	 * @param key              key name
	 * @param defaultValue     default value that will be returned if the value cannot be found
	 * @return                 value read from ini file or default if not found
	 */
	int getValue(String section,String key,int defaultValue) {
		Ini.Section iniSection= iniFile.get(section);
		
		if(iniSection!=null) {
			return iniSection.get(key,Integer.class,defaultValue);
		}
		else {
			log.warning("Section "+section+" not found");
			return defaultValue;
		}
	}
	
	/**
	 * returns a double value from the ini file
	 * @param section          section name
	 * @param key              key name
	 * @param defaultValue     default value that will be returned if the value cannot be found
	 * @return                 value read from ini file or default if not found
	 */
	double getValue(String section,String key,double defaultValue) {
		Ini.Section iniSection= iniFile.get(section);
		
		if(iniSection!=null) {
			return iniSection.get(key,Double.class,defaultValue);
		}
		else {
			log.warning("Section "+section+" not found");
			return defaultValue;
		}
	}
	
	/**
	 * returns a string value from the ini file
	 * @param section          section name
	 * @param key              key name
	 * @param defaultValue     default value that will be returned if the value cannot be found
	 * @return                 value read from ini file or default if not found
	 */
	String getValue(String section,String key,String defaultValue) {
		Ini.Section iniSection= iniFile.get(section);
		
		if(iniSection!=null) {
			return iniSection.get(key,String.class,defaultValue);
		}
		else {
			log.warning("Section "+section+" not found");
			return defaultValue;
		}
	}
	
	
	/**
	 * parses view display data from the configuration .ini file and creates panel objects
	 * @param name view name
	 * @param data view settings from ini file in form <display duration [s]>,<display start in hh:mm>-<display end in hh:mm> 
	 * @return ViewData object containing the parsed data or null of data could not be parsed
	 */
	private List<ViewData> parseViewData(final String name,final String data) {
		int pos = data.indexOf(',');
		int duration;
		LocalTime start,end;
		DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
		List<ViewData> viewList  = new LinkedList<ViewData>();
		Panel panel = null;
		
		if(pos!=-1) {
			try {
				duration = Integer.parseInt(data.substring(0, pos));
				while(pos!=-1) {
					int endPos = data.indexOf(',',pos+1);
					String displayInterval = endPos>=0 ? data.substring(pos+1,endPos) : data.substring(pos+1);
					pos = displayInterval.indexOf('-');
					if(pos!=-1) {
						start = LocalTime.parse(displayInterval.substring(0, pos),format);
						end   = LocalTime.parse(displayInterval.substring(pos+1),format);
						
						log.fine("parsed view data: name="+name+" duration="+duration+" start="+start+" end="+end);
						
						ViewData viewData = new ViewData();
						viewData.name         = name;
						viewData.duration     = duration;
						viewData.displayStart = start;
						viewData.displayEnd   = end;
						
						viewList.add(viewData);
						pos = data.indexOf(pos+1,',');
						
						// instantiate panel object
						if(panel==null) {
							try {
								Class<?> panelClass = Class.forName("picturepi."+name);
								panel = (Panel) panelClass.newInstance();
							} catch (ClassNotFoundException e) {
								log.severe("view panel class not found: "+name);
								log.severe(e.getMessage());
								return null;
							} catch (IllegalAccessException | InstantiationException e) {
								log.severe("unable to instantiate view panel class : "+name);
								log.severe(e.getMessage());
								return null;
							}
							
							log.fine("successfully created panel "+name);
						}
						viewData.panel = panel;
					}
					pos = endPos;
				}
				return viewList;
			}
			catch(NumberFormatException | DateTimeParseException  e) {
			}
		}
		
		// unable to parse data
		log.severe("Unable to parse view configuration for "+name+":"+data);
		
		return null;
	}
	
	//
	// getters/setters
	//
	final List<ViewData> getViewDataList() {
		return viewDataList;
	}

	//
	// nested class for view data
	//
	class ViewData {
		public String    name;                // view name
		public int       duration;            // display duration in seconds
		public LocalTime displayStart;        // start to display at
		public LocalTime displayEnd;          // end to display at
		Panel            panel;               // Panel object
		
		public boolean isActive() {
			return LocalTime.now().isAfter(displayStart) && LocalTime.now().isBefore(displayEnd);
		}
		
	}
	//
	// member variables
	//
	static  Configuration configuration = null;                 //singleton object
	private static final Logger   log     = Logger.getLogger( Configuration.class.getName() );

	private final    Ini            iniFile      = new Ini();                  // ini4j object
	private          List<ViewData> viewDataList = new LinkedList<ViewData>(); // List with all ViewData
}
