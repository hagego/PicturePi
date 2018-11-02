package picturepi;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.logging.Logger;

/**
 * Data Provider for TextWatch Panel
 * Provides the current time as a text
 */
public class TextWatchProvider extends Provider {

	TextWatchProvider() {
		// update data every second
		super(1);
	}

	@Override
	protected void fetchData() {
		if(textWatchpanel==null) {
			if(panel.getClass()==TextWatchPanel.class) {
				textWatchpanel = (TextWatchPanel)panel;
			}
			else {
				log.severe("Panel is not of class TextWatchPanel. Disabling updates."); //$NON-NLS-1$
				
				return;
			}
		}
		
		LocalTime time = LocalTime.now();
		textWatchpanel.setTime(time.format(formatter));
		
		// calculate index into table with time strings. Threshold to switch is always
		// at 2m30s+n*5min
		int secondsSinceHour = time.getMinute()*60+time.getSecond();
		int index = 0;
		if( secondsSinceHour>150) {
			index = (secondsSinceHour-150)/300+1;
		}
		
		if(index!=lastIndex) {
			if(index >= timeMessages.length) {
				log.severe("index into time messages out of bounds: "+index); //$NON-NLS-1$
				return;
			}
			
			String msg = timeMessages[index];
			msg = msg.replace("%current", String.valueOf(time.get(ChronoField.CLOCK_HOUR_OF_AMPM))); //$NON-NLS-1$
			msg = msg.replace("%next", String.valueOf(time.get(ChronoField.CLOCK_HOUR_OF_AMPM)+1)); //$NON-NLS-1$
			
			textWatchpanel.setTimeText(msg);
			lastIndex = index;
		}
	}

	//
	// private members
	//
	private static final Logger   log     = Logger.getLogger( TextWatchPanel.class.getName() );
	
	// time text messages
	private final String timeMessages[]   = {
			Messages.getString("TextWatchProvider.TIME_SHORTLY_AFTER_CURRENT"), //$NON-NLS-1$
			Messages.getString("TextWatchProvider.TIME_5_MINUTES_AFTER_CURRENT"), //$NON-NLS-1$
			Messages.getString("TextWatchProvider.TIME_10_MINUTES_AFTER_CURRENT"), //$NON-NLS-1$
			Messages.getString("TextWatchProvider.TIME_QUARTER_PAST_CURRENT"),  //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_10_MINUTES_BEFORE_HALF"), //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_5_MINUTES_BEFORE_HALF"), //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_HALF"), //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_5_MINUTES_PAST_HALF"), //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_10_MINUTES_PAST_HALF"), //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_QUARTER_TO_NEXT"), //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_10_MINUTES_BEFORE_NEXT"), //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_5_MINUTES_BEFORE_NEXT"), //$NON-NLS-1$
            Messages.getString("TextWatchProvider.TIME_SHORTLY_BEFORE_NEXT")}; //$NON-NLS-1$

	private       TextWatchPanel    textWatchpanel = null;    // TextWatchPanel to update
	private       int               lastIndex = -1;           // stores index of last message displayed
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss"); //$NON-NLS-1$
}
