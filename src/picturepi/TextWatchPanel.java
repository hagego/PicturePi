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
		
		labelTimeText = new JLabel ();
		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 60);
		labelTimeText.setFont(font);
		labelTimeText.setForeground(Color.MAGENTA.darker().darker());
		labelTimeText.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTimeText);
		
		add(Box.createVerticalGlue());
		
		labelTime = new JLabel ();
		labelTime.setFont(font);
		labelTime.setForeground(Color.CYAN.darker().darker());
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
	
	/**
	 * sets the actual time as text
	 * @param timeText actual time as text
	 */
	void setTimeText(String timeText) {
		labelTimeText.setText(timeText);
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
	
	private JLabel labelTimeText;   // label for time as text
	private JLabel labelTime;       // label for standard time display
	private JLabel labelAlarm;      // label for display of next alarm time
}
