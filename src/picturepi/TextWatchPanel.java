package picturepi;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Font;

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
		
		setBackground(Color.BLACK);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		//add(Box.createRigidArea(new Dimension(0,100)));		
		add(Box.createVerticalGlue());
		
		labelTimeTextLine1 = new JLabel ();
		labelTimeTextLine2 = new JLabel ();
		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 60);
		
		labelTimeTextLine1.setFont(font);
		labelTimeTextLine1.setForeground(Color.MAGENTA.darker().darker());
		labelTimeTextLine1.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTimeTextLine1);
		
		labelTimeTextLine2.setFont(font);
		labelTimeTextLine2.setForeground(Color.MAGENTA.darker().darker());
		labelTimeTextLine2.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTimeTextLine2);
		
		add(Box.createVerticalGlue());
		
		labelTime = new JLabel ();
		labelTime.setFont(font);
		labelTime.setForeground(Color.CYAN.darker().darker().darker());
		labelTime.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTime);
		
		add(Box.createVerticalGlue());
		
		labelAlarm = new JLabel ();
		labelAlarm.setFont(font);
		labelAlarm.setForeground(Color.ORANGE.darker().darker());
		labelAlarm.setAlignmentX(CENTER_ALIGNMENT);
		add(labelAlarm);
		
		add(Box.createVerticalGlue());
	}
	
	@Override
	void setColorDark() {
		labelTimeTextLine1.setForeground(Color.MAGENTA.darker().darker());
		labelTimeTextLine2.setForeground(Color.MAGENTA.darker().darker());
		labelTime.setForeground(Color.CYAN.darker().darker());
		labelAlarm.setForeground(Color.ORANGE.darker().darker());
	}
	
	@Override
	void setColorBright() {
		labelTimeTextLine1.setForeground(Color.MAGENTA.brighter().brighter());
		labelTimeTextLine2.setForeground(Color.MAGENTA.brighter().brighter());
		labelTime.setForeground(Color.CYAN.brighter().brighter());
		labelAlarm.setForeground(Color.ORANGE.brighter().brighter());
	}
	
	/**
	 * sets the actual time as text
	 * @param timeText actual time as text
	 */
	void setTimeText(String timeTextLine1,String timeTextLine2) {
		labelTimeTextLine1.setText(timeTextLine1);
		labelTimeTextLine2.setText(timeTextLine2);
	}
	
	/**
	 * sets the actual time 
	 * @param time actual time
	 */
	void setTime(String timeText) {
		labelTime.setText(timeText);
	}
	
	/**
	 * sets the next alarm time
	 * @param alarmText next alarm time
	 */
	void setAlarm(String alarmText) {
		labelAlarm.setText(alarmText);
	}

	//
	// private members
	// 
	private static final long serialVersionUID = 8937994138265702017L;
	
	private JLabel labelTimeTextLine1;   // label for time as text, line 1
	private JLabel labelTimeTextLine2;   // label for time as text, line 2
	private JLabel labelTime;            // label for standard time display
	private JLabel labelAlarm;           // label for display of next alarm time
}
