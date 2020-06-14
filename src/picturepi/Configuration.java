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
		return configuration;
	}

	/**
	 * reads the .ini configuration file
	 * @param  filename filename
	 * @return true in case file could be read successfully, otherwise false 
	 */
	boolean readConfigurationFile(final String filename) {
		try {
			log.config("reading configuration file "+filename);
			iniFile.load(new FileReader(filename));
		} catch (IOException e) {
			log.severe("Unable to load configuration file "+filename);
			log.severe(e.getMessage());
			
			return false;
		}
		
		log.config("configuration file was read successfully");
		return true;
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
	 * parses view display data from the configuration .ini file 
	 * @param  data view settings from ini file in form <display duration [s]>,<display start in hh:mm>-<display end in hh:mm> 
	 * @return linked list of ViewData objects containing the parsed data or empty list if data could not be parsed
	 */
	List<ViewData> parseViewData(final String data) {
		int pos = data.indexOf(',');
		int duration;
		LocalTime start,end;
		DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
		List<ViewData> viewList  = new LinkedList<ViewData>();
		
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
						
						log.fine("parsed view data: duration="+duration+" start="+start+" end="+end);
						
						ViewData viewData = new ViewData();
						viewData.name         = null;
						viewData.duration     = duration;
						viewData.displayStart = start;
						viewData.displayEnd   = end;
						viewData.panel        = null;
						
						viewList.add(viewData);
						pos = data.indexOf(pos+1,',');
					}
					pos = endPos;
				}
				return viewList;
			}
			catch(NumberFormatException | DateTimeParseException  e) {
			}
		}
		
		// unable to parse data
		log.severe("Unable to parse view configuration for "+data);
		
		return viewList;
	}
	
	/**
	 * parses data for views activated on bluetooth button click
	 * @param data  View data in the form <bluetooth button address>,<clicks>,<display duration in s>
	 * @return ButtonClickViewData object with the parsed data or null in case of error
	 */
	ButtonClickViewData parseButtonClickViewData(final String data) {
		ButtonClickViewData buttonClickViewData = null;
		String              address;
		int                 clicks;
		int                 duration;
		int                 pos = data.indexOf(',');
		
		try {
			if(pos!=-1) {
				address = data.substring(0, pos);
				if(pos+1<data.length()) {
					int pos2 = data.indexOf(',',pos+1);
					if(pos2!=-1) {
						clicks = Integer.parseInt(data.substring(pos+1,pos2));
						if(clicks>=0 && clicks<3 && pos2+1<data.length()) {
							duration = Integer.parseInt(data.substring(pos2+1));
							
							buttonClickViewData = new ButtonClickViewData();
							buttonClickViewData.buttonAddress = address;
							buttonClickViewData.clicks        = clicks;
							buttonClickViewData.duration      = duration;
							
							return buttonClickViewData;
						}
					}
				}
			}
		}
		catch(NumberFormatException e) {
			// nothing to do
		}
		
		log.severe("Unable to parse button click view data from "+data);
		
		return null;
	}

	/**
	 * reads view data and creates them
	 */
	private void readViewData() {
		// read view data
		log.config("reading view data");
		
		viewDataList = new LinkedList<ViewData>();
		
		// syntax: <Viewname> = <display duration [s]>,<display start in hh:mm>-<display end in hh:mm>
		Ini.Section views= iniFile.get("views");
		for(Map.Entry<String,String> entry: views.entrySet() ) {
			log.config("found view: "+entry.getKey()+"="+entry.getValue());
			
			// parse view settings
			List<ViewData> viewDataTmpList = parseViewData(entry.getValue());
			if(viewDataTmpList != null) {
				for(ViewData viewData:viewDataTmpList) {
					viewData.name = entry.getKey();
				}
				viewDataList.addAll(viewDataTmpList);
			}
		}
	}
	
	/**
	 * reads the mapping of flicd bluetooth buttons to panels
	 */
	private void readButtonViewData() {
		log.config("reading flicd bluetooth button2view mapping");
		
		buttonViewList = new LinkedList<ButtonClickViewData>();
		
		Ini.Section buttons= iniFile.get("buttons");
		for(Map.Entry<String,String> entry: buttons.entrySet() ) {
			String key = entry.getKey();
			log.config("found view: "+key+" maps to button "+entry.getValue());
			// check if view name contains optional ID string for view creation
			String id       = null;
			String viewName = key;
			int startPos = key.indexOf('[');
			if(startPos>0) {
				int endPos = key.indexOf(']');
				if(endPos>0 && endPos>startPos) {
					log.fine("view name contains optional ID");
					id = key.substring(startPos+1, endPos);
					viewName = key.substring(0, startPos);
					log.fine("final view name="+viewName+" id="+id);
				}
			}
			ButtonClickViewData buttonClickViewData = Configuration.getConfiguration().parseButtonClickViewData(entry.getValue());
			if(buttonClickViewData!=null) {
				buttonClickViewData.viewName = viewName;
				buttonClickViewData.id = id;
				buttonViewList.add(buttonClickViewData);
			}
		}
	}
	
	//
	// getters/setters
	//
	final List<ViewData> getViewDataList() {
		if(viewDataList==null) {
			// first time this method is called. Parse data from file
			readViewData();
		}
		return viewDataList;
	}
	
	final List<ButtonClickViewData> getButtonViewList() {
		if(buttonViewList==null) {
			// first time this method is called. Parse data from config file
			readButtonViewData();
		}
		return buttonViewList;
	}

	//
	// nested class for view data
	//
	class ViewData {
		public String    name;                // view name
		public int       duration;            // display duration in seconds
		public LocalTime displayStart;        // start to display at
		public LocalTime displayEnd;          // end to display at
		public Panel     panel;               // Panel object
		
		public boolean isActive() {
			return LocalTime.now().isAfter(displayStart) && LocalTime.now().isBefore(displayEnd);
		}
	}
	
	//
	// nested class for views activated thru button clicks
	//
	class ButtonClickViewData {
		public String   viewName;       // view name
		public String   buttonAddress;  // bluetooth address
		public int      duration;       // display duration in seconds after button click
		public int      clicks;         // click count: 1=single click, 2=double click
		public String   id;             // optional ID string that will be passed thru to the panel
		public Panel    panel;          // Panel object
	}
	
	//
	// member variables
	//
	private static       Configuration configuration;          //singleton object
	static {
		configuration = new Configuration();
	}
	
	private static final Logger        log           = Logger.getLogger( Configuration.class.getName() );

	private final    Ini            iniFile            = new Ini();               // ini4j object
	private          List<ViewData> viewDataList       = null;                    // stores view scheduling data
	private          List<ButtonClickViewData> buttonViewList = null;             // stores data to map button clicks to views
}
