package picturepi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 * Provider class for public transportation departure information based on the EFA XML interface 
 */
public class EfaDepartureMonitorProvider extends Provider {
	
	// local class with departure information
	public class DepartureInformation{
		String    destination;    // destination
		LocalTime scheduledTime;  // scheduler departure time
		LocalTime realTime;       // realtime departure time
	}

	/**
	 * constructor
	 */
	EfaDepartureMonitorProvider() {
		super(120);
		
		// get configuration data
		String section = EfaDepartureMonitorPanel.class.getSimpleName();
		baseUrl = Configuration.getConfiguration().getValue(section, "url", null);
		if(baseUrl==null) {
			log.severe("No value for url found in configuration file");
			
			return;
		}
		
		stopPointName = Configuration.getConfiguration().getValue(section, "stopPointName", null);
		if(stopPointName==null) {
			log.severe("No value for stopPointName found in configuration file");
		}
		else {
			log.config("EfaDepartureMonitor provider created for stop point "+stopPointName+", server URL="+baseUrl);
		}
		
		int sleepTime = Configuration.getConfiguration().getValue(section, "refreshInterval", 0);
		if(sleepTime>0) {
			log.config("refresh interval set to "+sleepTime+" seconds");
			setSleepTime(sleepTime);
		}
	}
	
	/**
	 * executes the HTTP request and returns an input stream with the server response
	 * @param baseUrl        server base URL
	 * @param stopPointName  name of stop point ("Haltestelle")
	 * @return InputStream with the server response or null in case of error
	 */
	InputStream getResponseStream(String baseUrl,String stopPointName) {
		if(baseUrl==null || stopPointName==null) {
			log.severe("executeRequest: method parameters are null");
			return null;
		}
		
		URL fullUrl = null;
		try {
			fullUrl = new URL(baseUrl+"/XML_DM_REQUEST?language=de&typeInfo_dm=stopID&nameInfo_dm="+stopPointName.replace(" ", "%20")+"&deleteAssignedStops_dm=1&useRealtime=1&mode=direct");
		} catch (MalformedURLException e) {
			log.severe("Unable to build final URL. Base Url="+baseUrl);
			log.severe(e.getMessage());
			
			return null;
		}
		log.fine("final url="+fullUrl);
		
		try {
			return fullUrl.openStream();
		} catch (IOException e) {
			log.severe("Unable to execute HTTP request for "+fullUrl);
			log.severe(e.getMessage());
			
			return null;
		}
	}
	
	/**
	 * parses the departure list from the XML response
	 * @param inputStream InputStream with the XML server response
	 * @return list of DepartureInformation objects or null in case of error
	 */
	List<DepartureInformation> getDepartureList(InputStream inputStream) {
	    
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

      try {
         // parse the XML data
         DocumentBuilder builder = factory.newDocumentBuilder();
         Document doc = builder.parse(inputStream);
         
         // get root element
         Element root = doc.getDocumentElement();
         
         NodeList nodeList = root.getElementsByTagName("itdDepartureMonitorRequest");
         if(nodeList.getLength()!=1) {
        	 log.severe("itdRequest has not exactly one itdDepartureMonitorRequest element");
        	 return null;
         }
         Node node = nodeList.item(0);
         if (node.getNodeType() != Node.ELEMENT_NODE)
         {
            log.severe("itdDepartureMonitorRequest is not of type Element");
            return null;
         }
         
         Element itdDepartureMonitorRequest = (Element) node;
         nodeList = itdDepartureMonitorRequest.getElementsByTagName("itdDepartureList");
         if(nodeList.getLength()!=1) {
        	 log.severe("itdDepartureMonitorRequest has not exactly one itdDepartureList element");
        	 return null;
         }
         node = nodeList.item(0);
         if (node.getNodeType() != Node.ELEMENT_NODE)
         {
            log.severe("itdDepartureList is not of type Element");
            return null;
         }
         
         Element itdDepartureList = (Element) node;
         nodeList = itdDepartureList.getElementsByTagName("itdDeparture");
         log.fine("Number of departures: "+nodeList.getLength());
         
         List<DepartureInformation> departureInformationList = new LinkedList<DepartureInformation>();
         int departureCount = Integer.min(nodeList.getLength(), 4);
         for(int i=0 ; i<departureCount ; i++) {
        	 node = nodeList.item(i);
             if (node.getNodeType() != Node.ELEMENT_NODE)
             {
                log.severe("itdDeparture is not of type Element");
                return null;
             }
             
             Element itdDeparture = (Element)node;
             DepartureInformation departureInformation = parseDepartureInformation(itdDeparture);
             if(departureInformation==null) {
            	 log.severe("parseDepartureInformation returns null");
             }
             else {
            	 departureInformationList.add(departureInformation);
             }
         }
         
         return departureInformationList;
      } catch (Exception e) {
         log.severe("Exception during XML parsing");
         log.severe(e.getMessage());
      }
		
      return null;
	}
	
	/**
	 * parses information from an XML DepartureList element
	 * @param  itdDeparture XML itdDeparture element
	 * @return Java DepartureInformation object
	 */
	private DepartureInformation parseDepartureInformation(Element itdDeparture) {
		DepartureInformation departureInformation = new DepartureInformation();
		
		// scheduled time
		NodeList nodeList = itdDeparture.getElementsByTagName("itdDateTime");
        if(nodeList.getLength()!=1) {
        	log.severe("itdDeparture has not exactly one itdDateTime element");
        	return null;
        }
        departureInformation.scheduledTime = parseDateTime((Element)nodeList.item(0));
        
        // real time
		nodeList = itdDeparture.getElementsByTagName("itdRTDateTime");
        if(nodeList.getLength()!=1) {
        	// not all departures have real time data
        	departureInformation.realTime = null;
        }
        else {
        	departureInformation.realTime = parseDateTime((Element)nodeList.item(0));
        }
        
        // line
        nodeList = itdDeparture.getElementsByTagName("itdServingLine");
        if(nodeList.getLength()!=1) {
        	log.severe("itdDeparture has not exactly one itdServingLine element");
       	 	return null;
        }
        departureInformation.destination = ((Element)nodeList.item(0)).getAttributeNode("direction").getNodeValue();
        
        log.fine("Found departure to "+departureInformation.destination+" with scheduled time "+departureInformation.scheduledTime+" and real time "+departureInformation.realTime);
        
		return departureInformation;
	}
	
	/**
	 * parses a DateTime XML element into a Java LocalTime object
	 * @param  dateTime XML DateTime element
	 * @return Java LocalTime objects
	 */
	private LocalTime parseDateTime(Element dateTime) {
		NodeList nodeList = dateTime.getElementsByTagName("itdTime");
        if(nodeList.getLength()!=1) {
        	log.severe("dateTime element has not exactly one itdDateTime element");
        	return null;
        }
        
        Element itdTime = (Element)nodeList.item(0);
        String hour =itdTime.getAttributeNode("hour").getNodeValue();
        hour = hour.length()<2 ? "0"+hour : hour;
        
        String minute =itdTime.getAttributeNode("minute").getNodeValue();
        minute = minute.length()<2 ? "0"+minute : minute;
        
        return LocalTime.parse(hour+":"+minute+":00");
	}

	/* (non-Javadoc)
	 * @see picturepi.Provider#fetchData()
	 */
	@Override
	protected void fetchData() {
		// TODO Auto-generated method stub
		
		// http://efastatic.vvs.de/OpenVVSDay/XML_DM_REQUEST?laguage=de&typeInfo_dm=stopID&nameInfo_dm=Cafe%20Stoll&deleteAssignedStops_dm=1&useRealtime=1&mode=direct
		
	}
	
	//
	// private data
	//
	private static final Logger   log     = Logger.getLogger( MethodHandles.lookup().lookupClass().getName() );

	private String  baseUrl       = null;  // EFA server URL for XML_DM_REQUEST query
	private String  stopPointName = null;  // name of the stop point, read from configuration file
	private Integer stopPointId   = null;  // stop point ID, extracted from first successful response

	private EfaDepartureMonitorPanel    panel = null;    // associated panel to update
}
