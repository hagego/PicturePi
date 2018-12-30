package picturepi;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

import picturepi.GarbageCollectionPanel.TrashBinColors;

public class GarbageCollectionProvider extends Provider {

	GarbageCollectionProvider() {
		// update data every hour
		super(60);
		
		// create GoogleCalendar object and connect
		googleCalendar = new GoogleCalendar();
		if( googleCalendar.connect() == false) {
			// connect failed
			log.severe("Connection to Google Calendar failed");
			googleCalendar = null;
		}
	}


	@Override
	protected void fetchData() {
		GarbageCollectionPanel p = (GarbageCollectionPanel)panel;
		
		if(googleCalendar!=null) {
			String calendarName = Configuration.getConfiguration().getValue("GarbageCollectionPanel", "googleCalendarName", null);
			if(calendarName==null || calendarName.length()==0) {
				log.severe("No Google Calendar specified. Unable to read data");
				
				return;
			}
			EnumSet<TrashBinColors> newTrashBinColors = EnumSet.noneOf(TrashBinColors.class);
			List<String> entries = googleCalendar.getCalendarEntriesForToday(calendarName);
			
			for(String entry:entries) {
				log.fine("found entry: "+entry);
				if(entry.toLowerCase().contains("rest")) {
					log.fine("adding black trashbin to display");
					newTrashBinColors.add(TrashBinColors.BLACK);
				}
				if(entry.toLowerCase().contains("bio")) {
					log.fine("adding brown trashbin to display");
					newTrashBinColors.add(TrashBinColors.BROWN);
				}
				if(entry.toLowerCase().contains("gelb")) {
					log.fine("adding yellow trashbin to display");
					newTrashBinColors.add(TrashBinColors.YELLOW);
				}
				if(entry.toLowerCase().contains("papier")) {
					log.fine("adding blue trashbin to display");
					newTrashBinColors.add(TrashBinColors.BLUE);
				}
			}
			p.setTrashBinColors(newTrashBinColors);
		}
	}

	//
	// private members
	//
	private static final Logger   log     = Logger.getLogger( GarbageCollectionProvider.class.getName() );

	private GoogleCalendar googleCalendar = null;
}
