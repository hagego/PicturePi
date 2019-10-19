package picturepi;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

/**
 * Panel to display the current time in 24h notation and as a 1 line text
 *
 */
class TextWatchPanel extends Panel {

	/**
	 * Constructor
	 */
	public TextWatchPanel() {
		super(new TextWatchProvider());
		
		int fontSizeText = Configuration.getConfiguration().getValue(this.getClass().getSimpleName(), "fontSizeText", 70);
		int fontSizeTime = Configuration.getConfiguration().getValue(this.getClass().getSimpleName(), "fontSizeTime", 70);
		
		setBackground(Color.BLACK);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		Font fontText = new Font(Font.SANS_SERIF, Font.BOLD, fontSizeText);
		Font fontTime = new Font(Font.SANS_SERIF, Font.BOLD, fontSizeTime);
		
		add(Box.createVerticalGlue());
		
		labelTimeTextLine1.setFont(fontText);
		labelTimeTextLine1.setForeground(Color.MAGENTA.darker().darker());
		labelTimeTextLine1.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTimeTextLine1);
		
		labelTimeTextLine2.setFont(fontText);
		labelTimeTextLine2.setForeground(Color.MAGENTA.darker().darker());
		labelTimeTextLine2.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTimeTextLine2);
		
		add(Box.createVerticalGlue());
		
		labelTime.setFont(fontTime);
		labelTime.setForeground(Color.CYAN.darker().darker().darker());
		labelTime.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTime);
		
		add(Box.createVerticalGlue());
		
		labelOptionText.setFont(fontTime);
		labelOptionText.setForeground(Color.CYAN.darker().darker().darker());
		labelOptionText.setAlignmentX(CENTER_ALIGNMENT);
		add(labelOptionText);
		
		add(Box.createVerticalGlue());
		log.fine("TextWatchPanel created");
	}
	
	@Override
	boolean hasData() {
		return true;
	}
	
	@Override
	void setColorDark() {
		labelTimeTextLine1.setForeground(Color.MAGENTA.darker().darker().darker());
		labelTimeTextLine2.setForeground(Color.MAGENTA.darker().darker().darker());
		labelTime.setForeground(Color.CYAN.darker().darker().darker());
		labelOptionText.setForeground(Color.ORANGE.darker().darker().darker());
	}
	
	@Override
	void setColorBright() {
		labelTimeTextLine1.setForeground(Color.MAGENTA.brighter().brighter().brighter());
		labelTimeTextLine2.setForeground(Color.MAGENTA.brighter().brighter().brighter());
		labelTime.setForeground(Color.CYAN.brighter().brighter().brighter());
		labelOptionText.setForeground(Color.ORANGE.brighter().brighter().brighter());
	}
	
	/**
	 * sets the actual time as text
	 * @param timeText actual time as text
	 */
	synchronized void setTimeText(String timeTextLine1,String timeTextLine2) {
		labelTimeTextLine1.setText(timeTextLine1);
		labelTimeTextLine2.setText(timeTextLine2);
	}
	
	/**
	 * sets the actual time 
	 * @param time actual time
	 */
	synchronized void setTime(String timeText) {
		labelTime.setText(timeText);
	}

	/**
	 * sets the optional additional text line
	 * @param text text to set
	 * @param icon optional icon to set
	 */
	synchronized void setOptionText(String text,ImageIcon icon) {
		if(text!=null) {
			labelOptionText.setText(text);
		}
		else {
			labelOptionText.setText("");
		}
		
		labelOptionText.setIcon(icon);
	}
	
	

	//
	// private members
	// 
	private static final long serialVersionUID = 8937994138265702017L;
	private static final Logger   log     = Logger.getLogger( TextWatchPanel.class.getName() );

	private JLabel    labelTimeTextLine1 = new JLabel();   // label for time as text, line 1
	private JLabel    labelTimeTextLine2 = new JLabel();   // label for time as text, line 2
	private JLabel    labelTime          = new JLabel();   // label for standard time display
	private JLabel    labelOptionText    = new JLabel();
}
