package picturepi;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

public class TomTomTrafficPanel extends Panel {

	/**
	 * 
	 */

	public TomTomTrafficPanel() {
		super(new TomTomTrafficProvider());
		
		log.config("Creating TomTomTrafficPanel");
		
		int fontSizeHeader = 72;
		int fontSizeText   = 48;
		
		setBackground(Color.BLACK);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		//add(Box.createRigidArea(new Dimension(0,100)));		
		add(Box.createVerticalGlue());
		
		labelHeader        = new JLabel ("Verkehrslage");
		labelDestination   = new JLabel ("Zielort: "+Configuration.getConfiguration().getValue(this.getClass().getSimpleName(), "destination", ""));
		labelDistance      = new JLabel ("Entfernung");
		labelDuration      = new JLabel ("Dauer: -- Minuten");
		labelDelay         = new JLabel ("Verzögerungen: -- Minuten");
		labelSummaryLine1  = new JLabel ("---");
		labelSummaryLine2  = new JLabel ("---");
		labelSummaryLine3  = new JLabel ("---");
		
		Font fontHeader    = new Font(Font.SANS_SERIF, Font.PLAIN, fontSizeHeader);
		Font fontText      = new Font(Font.SANS_SERIF, Font.PLAIN, fontSizeText);
		
		labelHeader.setFont(fontHeader);
		labelHeader.setForeground(Color.MAGENTA);
		labelHeader.setAlignmentX(CENTER_ALIGNMENT);
		add(labelHeader);
		
		add(Box.createVerticalGlue());
		
		labelDestination.setFont(fontText);
		labelDestination.setForeground(Color.WHITE);
		labelDestination.setAlignmentX(CENTER_ALIGNMENT);
		add(labelDestination);
		
		labelDistance.setFont(fontText);
		labelDistance.setForeground(Color.WHITE);
		labelDistance.setAlignmentX(CENTER_ALIGNMENT);
		add(labelDistance);
		
		labelDuration.setFont(fontText);
		labelDuration.setForeground(Color.WHITE);
		labelDuration.setAlignmentX(CENTER_ALIGNMENT);
		add(labelDuration);
		
		labelDelay.setFont(fontText);
		labelDelay.setForeground(Color.WHITE);
		labelDelay.setAlignmentX(CENTER_ALIGNMENT);
		add(labelDelay);
		
		add(Box.createVerticalGlue());
		
		labelSummaryLine1.setFont(fontText);
		labelSummaryLine1.setForeground(Color.CYAN);
		labelSummaryLine1.setAlignmentX(CENTER_ALIGNMENT);
		add(labelSummaryLine1);
		
		labelSummaryLine2.setFont(fontText);
		labelSummaryLine2.setForeground(Color.CYAN);
		labelSummaryLine2.setAlignmentX(CENTER_ALIGNMENT);
		add(labelSummaryLine2);
		
		labelSummaryLine3.setFont(fontText);
		labelSummaryLine3.setForeground(Color.CYAN);
		labelSummaryLine3.setAlignmentX(CENTER_ALIGNMENT);
		add(labelSummaryLine3);
		
		add(Box.createVerticalGlue());
	}

	@Override
	void setColorDark() {

	}

	@Override
	void setColorBright() {

	}

	@Override
	boolean hasData() {
		return true;
	}
	
	void setData(TomTomTrafficProvider.RouteData routeData) {
		log.fine("updating data");
		
		labelDistance.setText(String.format("Entfernung: %d km",routeData.length/1000));
		labelDuration.setText(String.format("Dauer: %d Minuten",routeData.duration/60));
		labelDelay.setText(String.format("Verzögerung: %d Minuten",routeData.delay/60));
		
		if(routeData.instructions.size()>0) {
			labelSummaryLine1.setText(routeData.instructions.get(0));
		}
		if(routeData.instructions.size()>1) {
			labelSummaryLine2.setText(routeData.instructions.get(1));
		}
		if(routeData.instructions.size()>2) {
			labelSummaryLine3.setText(routeData.instructions.get(2));
		}
	}

	//
	// private data
	//
	private static final long serialVersionUID = -8603653110019705649L;
	
	private final Logger      log = Logger.getLogger( this.getClass().getName() );

	private JLabel labelHeader;
	private JLabel labelDestination;
	private JLabel labelDistance;
	private JLabel labelDuration;
	private JLabel labelDelay;
	private JLabel labelSummaryLine1;
	private JLabel labelSummaryLine2;
	private JLabel labelSummaryLine3;
}
