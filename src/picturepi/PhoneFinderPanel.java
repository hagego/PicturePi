package picturepi;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

/**
 * Panel for PhoneFinder App
 * triggers an alarm sound on a mobile phone using MQTT
 */
class PhoneFinderPanel extends Panel {

	/**
	 * Constructor
	 */
	public PhoneFinderPanel(String phoneId) {
		super(new PhoneFinderProvider(phoneId));
		
		log.fine("Creating PhoneFinderPanel phone ID "+phoneId);
		
		setBackground(Color.BLACK);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		Font fontText = new Font(Font.SANS_SERIF, Font.BOLD, 70);
		
		add(Box.createVerticalGlue());
		
		log.fine("Creating PhoneFinderPanel phone ID "+phoneId);

		JLabel labelHeader = new JLabel();
		labelHeader.setText("PhoneFinder für Handy "+phoneId);
		labelHeader.setFont(fontText);
		labelHeader.setForeground(Color.MAGENTA.darker().darker());
		labelHeader.setAlignmentX(CENTER_ALIGNMENT);
		add(labelHeader);
		
		log.fine("Creating PhoneFinderPanel phone ID "+phoneId);
		
		add(Box.createVerticalGlue());
		
		labelStatus.setText("status");
		labelStatus.setFont(fontText);
		labelStatus.setForeground(Color.MAGENTA.darker().darker());
		labelStatus.setAlignmentX(CENTER_ALIGNMENT);
		add(labelStatus);
		
		add(Box.createVerticalGlue());
		
		log.fine("PhoneFinderPanel created for phone ID "+phoneId);
	}
	
	@Override
	boolean hasData() {
		return false;
	}
	
	@Override
	void setColorDark() {
	}
	
	@Override
	void setColorBright() {
	}
	
	void setStatusText(String statusText) {
		labelStatus.setText(statusText);
	}
	


	//
	// private members
	// 
	private static final long serialVersionUID = 8937994138265702017L;
	private static final Logger   log     = Logger.getLogger( PhoneFinderPanel.class.getName() );

	private JLabel    labelStatus = new JLabel();   // label to display phone status
}
