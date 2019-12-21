package picturepi;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

/**
 * Dummy Panel to open/close grage foor
 */
class GarageDoorPanel extends Panel {

	/**
	 * Constructor
	 */
	public GarageDoorPanel() {
		super(new GarageDoorProvider());
		
		setBackground(Color.BLACK);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		Font fontText = new Font(Font.SANS_SERIF, Font.BOLD, 70);
		
		add(Box.createVerticalGlue());
		
		label.setText("Garagentor öffnet/schließt");
		label.setFont(fontText);
		label.setForeground(Color.MAGENTA.darker().darker());
		label.setAlignmentX(CENTER_ALIGNMENT);
		add(label);
		
		add(Box.createVerticalGlue());
		
		log.fine("GarageDoorPanel created");
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
	


	//
	// private members
	// 
	private static final long serialVersionUID = 8937994138265702017L;
	private static final Logger   log     = Logger.getLogger( GarageDoorPanel.class.getName() );

	private JLabel    label = new JLabel();   // label to display dummy text
}
