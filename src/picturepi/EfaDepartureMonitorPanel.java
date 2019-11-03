package picturepi;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.invoke.MethodHandles;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;

public class EfaDepartureMonitorPanel extends Panel {

	public EfaDepartureMonitorPanel() {
		super(new EfaDepartureMonitorProvider());
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridwidth  = 4;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(10, 50, 50, 10);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 0.1;
		
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, 50);
		
		JLabel header = new JLabel("Abfahrten "+Configuration.getConfiguration().getValue(EfaDepartureMonitorPanel.class.getSimpleName(), "stopPointName", ""));
		header.setFont(font);
		header.setForeground(Color.BLACK);
		add(header,constraints);
		
		constraints.insets = new Insets(10, 10, 10, 10);
		constraints.gridwidth = 1;
		
		for(int departure=0 ; departure<MAX_COUNT ; departure++) {
			labelScheduledTime[departure] = new JLabel("");
			constraints.gridx = 0;
			constraints.gridy = 3+departure;
			constraints.insets = new Insets(10, 50, 10, 10);
			labelScheduledTime[departure].setFont(font);
			labelScheduledTime[departure].setForeground(Color.BLUE);
			add(labelScheduledTime[departure],constraints);
			
			labelRealTime[departure] = new JLabel("");
			labelRealTime[departure].setFont(font);
			labelRealTime[departure].setForeground(Color.BLUE);
			constraints.gridx = 1;
			constraints.insets = new Insets(10, 10, 10, 10);
			add(labelRealTime[departure],constraints);
			
			labelWaitTime[departure] = new JLabel("");
			labelWaitTime[departure].setFont(font);
			labelWaitTime[departure].setForeground(Color.BLUE);
			constraints.gridx = 2;
			constraints.insets = new Insets(10, 10, 10, 10);
			add(labelWaitTime[departure],constraints);
			
			labelDestination[departure] = new JLabel("");
			labelDestination[departure].setFont(font);
			labelDestination[departure].setForeground(Color.BLUE);
			constraints.gridx = 3;
			constraints.weightx = 0.9;
			add(labelDestination[departure],constraints);
			
			constraints.weightx = 0.1;
		}
		
		log.fine("EfaDepartureMonitorPanel created");
	}
	@Override
	void setColorDark() {
		// TODO Auto-generated method stub

	}

	@Override
	void setColorBright() {
		// TODO Auto-generated method stub

	}

	@Override
	boolean hasData() {
		return departureCount>0;
	}

	/**
	 * called by provider to update departure information
	 * @param departureList List with departure information
	 */
	void setDepartureInfo(List<EfaDepartureMonitorProvider.DepartureInformation> departureList) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");;
		departureCount = Integer.min(MAX_COUNT,departureList.size());
		
		for(int departure=0 ; departure<departureCount ; departure++) {
			labelScheduledTime[departure].setText(departureList.get(departure).scheduledTime.format(formatter));
			if(departureList.get(departure).realTime!=null) {
				labelRealTime[departure].setText(departureList.get(departure).realTime.format(formatter));
				if(departureList.get(departure).realTime.isAfter(departureList.get(departure).scheduledTime)) {
					labelRealTime[departure].setForeground(Color.RED);
				}
				else {
					labelRealTime[departure].setForeground(Color.BLUE);
				}
			}
			else {
				labelRealTime[departure].setText("");
			}
			
			labelWaitTime[departure].setText(departureList.get(departure).waitTime+" min");
			
			String destination = departureList.get(departure).destination;
			if(destination.length()>20 ) {
				destination = destination.substring(0, 20);
			}
			labelDestination[departure].setText(destination);
		}
	}

	//
	// private members
	//
	private static final Logger   log              = Logger.getLogger( MethodHandles.lookup().lookupClass().getName() );
	private static final long     serialVersionUID = -2033700528834112613L;
	
	private static final int     MAX_COUNT           = 4;                              // max. number of departures to display
	private              JLabel labelScheduledTime[] = new JLabel[MAX_COUNT];          // JLabel objects for scheduled departure time
	private              JLabel labelRealTime[]      = new JLabel[MAX_COUNT];          // JLabel objects for real-time departure time
	private              JLabel labelDestination[]   = new JLabel[MAX_COUNT];          // JLabel objects for destination
	private              JLabel labelWaitTime[]      = new JLabel[MAX_COUNT];          // JLabel objects for wait time
	
	private              int     departureCount      = 0;                              // currently displayed number of departures
}
