package picturepi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

/**
 * This class implements access to a Google Calendar
 */
public class GoogleCalendar {

	/**
	 * used to specify for which day (today or tomorrow) calendar items are to be
	 * retrieved
	 */
	public enum Mode {
		TODAY, TOMORROW
	};

	// application name
	private static final String APPLICATION_NAME = "AlarmPi";

	// directory to store access tokens. In the development environment this is a local folder
	// when deployed on raspberry this directory must be copied into the /etc/picturepi folder
	private static final String TOKENS_PATH_DEVELOPMENT = "res/google";
	private static final String TOKENS_PATH_DEPLOYED    = "/etc/picturepi/google";

	private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);

	/**
	 * connects to Google Calendar
	 * 
	 * @return true in case connection could be made, false otherwise
	 */
	public boolean connect() {
		NetHttpTransport HTTP_TRANSPORT;
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException e) {
			log.severe("Security exception: " + e.getMessage());

			return false;
		} catch (IOException e) {
			log.severe("IOException: " + e.getMessage());

			return false;
		}

		// Load client secrets
		InputStream in = this.getClass().getResourceAsStream("/GoogleCalendarCredentials.json");
		if (in == null) {
			log.severe("Unable to load Google calendar credentials file from resources folder");
			return false;
		}

		GoogleClientSecrets clientSecrets;
		try {
			clientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), new InputStreamReader(in));
			log.fine("Google Calendar secrets file loaded successfully");
		} catch (IOException e) {
			log.severe("Unable to load Google Calendar secrets file: " + e.getMessage());
			return false;
		}

		// Build flow and trigger user authorization request.
		String tokensDirectory;
		if(Configuration.getConfiguration().isRunningOnRaspberry()) {
			tokensDirectory = TOKENS_PATH_DEPLOYED;
		}
		else {
			tokensDirectory = TOKENS_PATH_DEVELOPMENT;
		}
		GoogleAuthorizationCodeFlow flow;
		try {
			flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), clientSecrets, SCOPES)
					.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectory)))
					.setAccessType("offline").build();
					
		} catch (IOException e) {
			log.severe("Unable to build Google Calendar flow: " + e.getMessage());
			return false;
		}

		Credential credential;
		try {
			credential = flow.loadCredential("alarmpi");
			log.fine("credential store could be accessed successessfully");
		} catch (IOException e) {
			log.severe("Unable to load credentials: " + e.getMessage());
			return false;
		}

		if (credential == null) {
			log.severe("Google Calendar credential could not be loaded. Authorization might still be required:");
			log.severe(flow.newAuthorizationUrl().setRedirectUri("http://localhost:8888/Callback").build());
			log.severe("tokens directory: "+tokensDirectory);

			LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
			try {
				credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("alarmpi");
			} catch (IOException e) {
				log.severe("Authorization failed: " + e.getMessage());
			}
		}

		// Build a new authorized API client service.
		calendar = new Calendar.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), credential).setApplicationName(APPLICATION_NAME)
				.build();

		return true;
	}

	/**
	 * Returns a list with calendar entries for today
	 * 
	 * @return list with calendar entries for today
	 */
	List<String> getCalendarEntriesForToday(String calendarName) {
		Mode mode = Mode.TODAY;
		LinkedList<String> entries = new LinkedList<String>();

		log.fine("Getting calender enries for " + mode.toString());

		if (calendar == null) {
			log.warning("getCalendarEntries called, but Calendar is not connected yet");
			return entries;
		}

		try {
			// loop thru all calendars and search for the one specified in the configuration
			CalendarList feed = calendar.calendarList().list().execute();
			if (feed.getItems() != null) {
				for (CalendarListEntry entry : feed.getItems()) {
					log.finest("Found calendar: Summary=" + entry.getSummary());

					if (entry.getSummary().equalsIgnoreCase(calendarName)) {
						log.fine("Found specified calendar. Summary=" + entry.getSummary() + " ID=" + entry.getId());

						com.google.api.services.calendar.Calendar.Events.List request = calendar.events()
								.list(entry.getId());

						// query all calendar events of today
						java.util.Calendar startOfDay = java.util.Calendar.getInstance();
						startOfDay.set(java.util.Calendar.HOUR_OF_DAY, 0);
						startOfDay.set(java.util.Calendar.MINUTE, 0);
						startOfDay.set(java.util.Calendar.SECOND, 0);
						java.util.Calendar endOfDay = java.util.Calendar.getInstance();
						endOfDay.set(java.util.Calendar.HOUR_OF_DAY, 23);
						endOfDay.set(java.util.Calendar.MINUTE, 59);
						endOfDay.set(java.util.Calendar.SECOND, 59);

						// add 1d in case we have to retrieve entries for tomorrow
						if (mode == Mode.TOMORROW) {
							startOfDay.add(java.util.Calendar.DAY_OF_YEAR, 1);
							endOfDay.add(java.util.Calendar.DAY_OF_YEAR, 1);
						}
						log.fine("start=" + startOfDay + " end=" + endOfDay);

						DateTime start = new DateTime(Date.from(startOfDay.toInstant()));
						DateTime end = new DateTime(Date.from(endOfDay.toInstant()));
						log.fine("start=" + start + " end=" + end);
						request.setTimeMin(start);
						request.setTimeMax(end);

						String pageToken = null;
						Events events = null;

						do {
							request.setPageToken(pageToken);
							events = request.execute();

							List<Event> items = events.getItems();
							if (items.size() == 0) {
								log.fine("No calendar items found");
							} else {
								for (Event event : items) {
									log.fine("Found calendar item: " + event.getSummary());
									entries.add(event.getSummary());
								}
							}
						} while (pageToken != null);

						break;
					}
				}
			} else {
				log.warning("Calendar List is empty");
			}
		} catch (IOException e) {
			log.severe("Error during processing of calendar entries: " + e.getMessage());
		}

		return entries;
	}

	//
	// private members
	//
	private static final Logger log = Logger.getLogger(GoogleCalendar.class.getName());

	private com.google.api.services.calendar.Calendar calendar = null;

}
