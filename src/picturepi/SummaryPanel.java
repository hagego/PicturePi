package picturepi;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.JLabel;

public class SummaryPanel extends Panel {

	public SummaryPanel() {
		super(new SummaryProvider());
		
		setLayout(new GridBagLayout());
		
		Font font   = new Font(Font.SANS_SERIF, Font.BOLD, 40);
		Color color = Color.BLACK;
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(10, 10, 10, 10);
		constraints.anchor = GridBagConstraints.WEST;
		
		// Date
		JLabel labelDateHeader = new JLabel("Datum");
		constraints.gridx = 0;
		constraints.gridy = 0;
		labelDateHeader.setFont(font);
		labelDateHeader.setForeground(color);
		add(labelDateHeader,constraints);
		
		constraints.gridx = 1;
		labelDate.setFont(font);
		labelDate.setForeground(color);
		add(labelDate,constraints);
		
		// Time
		JLabel labelTimeHeader = new JLabel("Zeit");
		constraints.gridx = 0;
		constraints.gridy++;
		labelTimeHeader.setFont(font);
		labelTimeHeader.setForeground(color);
		add(labelTimeHeader,constraints);
		
		constraints.gridx = 1;
		labelTime.setFont(font);
		labelTime.setForeground(color);
		add(labelTime,constraints);

		// temperature
		JLabel labelTemperatureHeader = new JLabel("Temperatur");
		constraints.gridx = 0;
		constraints.gridy++;
		labelTemperatureHeader.setFont(font);
		labelTemperatureHeader.setForeground(color);
		add(labelTemperatureHeader,constraints);

		constraints.gridx = 1;
		labelTemperature.setFont(font);
		labelTemperature.setForeground(color);
		labelTemperature.setText("--");
		add(labelTemperature,constraints);

		
		// route 1 data
		constraints.gridx = 0;
		constraints.gridy++;
		labelRoute1Name.setFont(font);
		labelRoute1Name.setForeground(color);
		add(labelRoute1Name,constraints);
		
		constraints.gridx = 1;
		labelRoute1Data.setFont(font);
		labelRoute1Data.setForeground(color);
		add(labelRoute1Data,constraints);
		
		// route 2 data
		constraints.gridx = 0;
		constraints.gridy++;
		labelRoute2Name.setFont(font);
		labelRoute2Name.setForeground(color);
		add(labelRoute2Name,constraints);
		
		constraints.gridx = 1;
		labelRoute2Data.setFont(font);
		labelRoute2Data.setForeground(color);
		add(labelRoute2Data,constraints);

		// calendar entries
		constraints.gridx = 0;
		constraints.gridy++;
		JLabel labelCalendarEntriesHeader = new JLabel("Kalender");
		labelCalendarEntriesHeader.setFont(font);
		labelCalendarEntriesHeader.setForeground(color);
		add(labelCalendarEntriesHeader,constraints);

		constraints.gridx = 1;
		labelCalendarEntries.setFont(font);
		labelCalendarEntries.setForeground(color);
		add(labelCalendarEntries,constraints);
		
		log.fine("SummaryPanel created");
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
		return true;
	}

	void setDateTime(String date, String time) {
		labelDate.setText(date);
		labelTime.setText(time);
	}

	void setTemperature(double temperature) {
		labelTemperature.setText(String.format("%.1f C",temperature));
	}
	
	void setRoute1DataStatic(String route1Name) {
		labelRoute1Name.setText(route1Name);
	}
	
	void setRoute1DataDynamic(String route1Data) {
		labelRoute1Data.setText(route1Data);
	}
	
	void setRoute2DataStatic(String route2Name) {
		labelRoute2Name.setText(route2Name);
	}
	
	void setRoute2DataDynamic(String route2Data) {
		labelRoute2Data.setText(route2Data);
	}

	void setCalendarEntries(String calendarEntries) {
		labelCalendarEntries.setText(calendarEntries);
	}


	//
	// private members
	//
	private static final long serialVersionUID = 2365333174633387718L;
	
	private final Logger    log = Logger.getLogger( this.getClass().getName() );
	
	private JLabel labelDate            = new JLabel();  // label displaying the date
	private JLabel labelTime            = new JLabel();  // label displaying the time
	private JLabel labelTemperature     = new JLabel();  // label displaying the temperature
	private JLabel labelRoute1Name      = new JLabel();  // label displaying the name for route 1 
	private JLabel labelRoute1Data      = new JLabel();  // label displaying dynamic information for route 1
	private JLabel labelRoute2Name      = new JLabel();  // label displaying the name for route 2 
	private JLabel labelRoute2Data      = new JLabel();  // label displaying dynamic information for route 2
	private JLabel labelCalendarEntries = new JLabel();  // label displaying calendar entries
}
