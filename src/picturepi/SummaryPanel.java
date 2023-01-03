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
		
		Font font   = new Font(Font.SANS_SERIF, Font.BOLD, 20);
		Color color = Color.BLACK;
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(10, 10, 10, 10);
		constraints.anchor = GridBagConstraints.WEST;
		
		// Date/Time
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


	//
	// private members
	//
	private static final long serialVersionUID = 2365333174633387718L;
	
	private final Logger    log = Logger.getLogger( this.getClass().getName() );
	
	private JLabel labelDate       = new JLabel();  // label displaying the date
	private JLabel labelTime       = new JLabel();  // label displaying the time
	private JLabel labelRoute1Name = new JLabel();  // label displaying the name for route 1 
	private JLabel labelRoute1Data = new JLabel();  // label displaying dynamic information for route 1
	private JLabel labelRoute2Name = new JLabel();  // label displaying the name for route 2 
	private JLabel labelRoute2Data = new JLabel();  // label displaying dynamic information for route 2
}
