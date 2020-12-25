package picturepi;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Panel to display the current time in 24h notation and as a 1 line text
 *
 */
class WeatherPanel extends Panel {

	/**
	 * Constructor
	 */
	public WeatherPanel() {
		super(new WeatherProvider());
		
		log.fine("creating WeatherPanel");
		
		int fontSizeText        = Configuration.getConfiguration().getValue(this.getClass().getSimpleName(), "fontSizeText", 32);
		int fontSizeTemperature = Configuration.getConfiguration().getValue(this.getClass().getSimpleName(), "fontSizeTemperature", 60);
		
		setBackground(Color.BLACK);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		//add(Box.createRigidArea(new Dimension(0,100)));		
		add(Box.createVerticalGlue());
		
		labelTemperature             = new JLabel ("Aktuell: --");
		labelTemperatureMin          = new JLabel ("Tiefst: --");
		labelForecastDate            = new JLabel ("Vorhersage für: --");
		labelForecastSummary         = new JLabel("Vorhersage");
		labelForecastIcon            = new JLabel();
		labelForecastHighTemperature = new JLabel("Höchst: --");
		
		Font fontText        = new Font(Font.SANS_SERIF, Font.PLAIN, fontSizeText);
		Font fontTemperature = new Font(Font.SANS_SERIF, Font.PLAIN, fontSizeTemperature);
		
		labelTemperature.setFont(fontTemperature);
		labelTemperature.setForeground(Color.MAGENTA.darker().darker());
		labelTemperature.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTemperature);
		
		labelTemperatureMin.setFont(fontTemperature);
		labelTemperatureMin.setForeground(Color.MAGENTA.darker().darker());
		labelTemperatureMin.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTemperatureMin);
		
		add(Box.createVerticalGlue());
		
		labelForecastDate.setFont(fontText);
		labelForecastDate.setForeground(Color.MAGENTA.darker().darker());
		labelForecastDate.setAlignmentX(CENTER_ALIGNMENT);
		//add(labelForecastDate);
		
		labelForecastSummary.setFont(fontText);
		labelForecastSummary.setForeground(Color.CYAN.darker().darker());
		labelForecastSummary.setAlignmentX(CENTER_ALIGNMENT);
		//add(labelForecastSummary);
		
		labelForecastIcon.setAlignmentX(CENTER_ALIGNMENT);
		add(labelForecastIcon);
		
		add(Box.createVerticalGlue());
		
		labelForecastHighTemperature.setFont(fontTemperature);
		labelForecastHighTemperature.setForeground(Color.CYAN.darker().darker());
		labelForecastHighTemperature.setAlignmentX(CENTER_ALIGNMENT);
		add(labelForecastHighTemperature);
		
		add(Box.createVerticalGlue());
		
		log.fine("WeatherPanel created");
	}
	
	@Override
	void setColorDark() {
		labelTemperature.setForeground(Color.MAGENTA.darker().darker());
		labelTemperatureMin.setForeground(Color.MAGENTA.darker().darker());
		labelForecastDate.setForeground(Color.MAGENTA.darker().darker());
		labelForecastSummary.setForeground(Color.CYAN.darker().darker());
		labelForecastHighTemperature.setForeground(Color.CYAN.darker().darker());
	}
	
	@Override
	void setColorBright() {
		labelTemperature.setForeground(Color.MAGENTA.brighter().brighter());
		labelTemperatureMin.setForeground(Color.MAGENTA.brighter().brighter());
		labelForecastDate.setForeground(Color.MAGENTA.brighter().brighter());
		labelForecastSummary.setForeground(Color.CYAN.brighter().brighter());
		labelForecastHighTemperature.setForeground(Color.CYAN.brighter().brighter());
	}
	
	@Override
	boolean hasData() {
		return true;
	}
	
	/**
	 * sets the actual temperature
	 * @param temperature actual temperature
	 */
	void setTemperature(double temperature) {
		EventQueue.invokeLater( new Runnable() {
	        public void run() {
	        	labelTemperature.setText(String.format("Aktuell: %.0f°C", temperature));
	        }
	    } );
	}
	
	/**
	 * sets the minimum temperature of the day
	 * @param temperatureMin minimum temperature
	 */
	void setTemperatureMin(double temperature) {
		EventQueue.invokeLater( new Runnable() {
	        public void run() {
	        	labelTemperatureMin.setText(String.format("Tiefst: %.0f°C", temperature));
	        }
	    } );
	}
	
	/**
	 * sets the forecast for the day
	 * @param date             date of the forecast
	 * @param summary          summary text
	 * @param temperatureHigh  day high temperature
	 * @param icon             forecast icon
	 */
	void setForecast(Date date,String summary,double temperatureHigh,ImageIcon icon) {
		EventQueue.invokeLater( new Runnable() {
	        public void run() {
	        	labelForecastDate.setText("Vorhersage für den "+new SimpleDateFormat("dd.MM.yyyy").format(date)+":");
	        	labelForecastSummary.setText(summary);
	        	labelForecastHighTemperature.setText(String.format("Höchst: %.0f°C",temperatureHigh));
	        	if(icon!=null) {
	        		labelForecastIcon.setIcon(icon);
	        	}
	        }
	    } );
	}
	

	//
	// private members
	// 
	private static final long serialVersionUID = 8937994138265702017L;
	private static final Logger   log     = Logger.getLogger( WeatherPanel.class.getName() );
	
	private JLabel labelTemperature;             // label to display actual measured temperature
	private JLabel labelTemperatureMin;          // label to display minimum measured temperature
	private JLabel labelForecastDate;            // label to display date of forecast
	private JLabel labelForecastSummary;         // label to display forecast summary
	private JLabel labelForecastIcon;            // label to display forecast icon
	private JLabel labelForecastHighTemperature; // label to display high temperature
}
