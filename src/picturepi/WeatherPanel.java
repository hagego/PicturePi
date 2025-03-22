package picturepi;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
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
		
		labelTemperature              = new JLabel("Aktuell: --");
		labelForecastSummary          = new JLabel("Vorhersage");
		labelForecastIcon             = new JLabel();
		labelForecastTemperatureRange = new JLabel("Temperatur --");
		
		Font fontText        = new Font(Font.SANS_SERIF, Font.PLAIN, fontSizeText);
		Font fontTemperature = new Font(Font.SANS_SERIF, Font.PLAIN, fontSizeTemperature);
		
		// measured temperature
		labelTemperature.setFont(fontTemperature);
		labelTemperature.setForeground(Color.MAGENTA.darker().darker());
		labelTemperature.setAlignmentX(CENTER_ALIGNMENT);
		add(labelTemperature);

		// chart with temperature over day
		temperatureChart = new XYChartBuilder().width(getWidth()).height(200).title("Tagesverlauf").xAxisTitle("Zeit").yAxisTitle("Temp").build();

		// Customize Chart
		temperatureChart.getStyler().setLegendPosition(LegendPosition.InsideNE);
		temperatureChart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
		temperatureChart.getStyler().setPlotBackgroundColor(Color.BLACK);
		temperatureChart.getStyler().setChartBackgroundColor(Color.BLACK);
		temperatureChart.getStyler().setChartFontColor(Color.RED);
		temperatureChart.getStyler().setChartTitleBoxVisible(false);
		temperatureChart.getStyler().setPlotGridLinesVisible(false);
		temperatureChart.getStyler().setAxisTickLabelsColor(Color.WHITE);

		// Dummy data
		XYSeries series = temperatureChart.addSeries(chartName,new double[] { 0, 3, 5, 7, 9},new double[]{0,10,20,10,0});
		series.setLineColor(Color.RED);
		series.setMarkerColor(Color.YELLOW);

       	// add to panel
        JPanel chartPanel = new XChartPanel<XYChart>(temperatureChart);
        add(chartPanel);
		
		add(Box.createVerticalGlue());
		
		labelForecastSummary.setFont(fontText);
		labelForecastSummary.setForeground(Color.CYAN.darker().darker());
		labelForecastSummary.setAlignmentX(CENTER_ALIGNMENT);
		add(labelForecastSummary);
		
		labelForecastIcon.setAlignmentX(CENTER_ALIGNMENT);
		add(labelForecastIcon);
		
		add(Box.createVerticalGlue());
		
		labelForecastTemperatureRange.setFont(fontText);
		labelForecastTemperatureRange.setForeground(Color.CYAN.darker().darker());
		labelForecastTemperatureRange.setAlignmentX(CENTER_ALIGNMENT);
		add(labelForecastTemperatureRange);
		
		add(Box.createVerticalGlue());

		log.fine("WeatherPanel created");
	}
	
	@Override
	void setColorDark() {
		labelTemperature.setForeground(Color.MAGENTA.darker().darker());
		labelForecastSummary.setForeground(Color.CYAN.darker().darker());
		labelForecastTemperatureRange.setForeground(Color.CYAN.darker().darker());
	}
	
	@Override
	void setColorBright() {
		labelTemperature.setForeground(Color.MAGENTA.brighter().brighter());
		labelForecastSummary.setForeground(Color.CYAN.brighter().brighter());
		labelForecastTemperatureRange.setForeground(Color.CYAN.brighter().brighter());
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
				if(temperatureMin!=null && temperatureMax!=null) {
					labelTemperature.setText(String.format("Aktuell: %.0f C (heute %.0f bis %.0f C)", temperature,temperatureMin,temperatureMax));
				}
				else {
	        		labelTemperature.setText(String.format("Aktuell: %.0f°C", temperature));
				}
	        }
	    } );
	}
	
	/**
	 * sets the minimum temperature of the day
	 * @param temperatureMin minimum temperature
	 */
	void setTemperatureMin(double temperature) {
		temperatureMin = temperature;
	}

	/**
	 * sets the maximum temperature of the day
	 * @param temperatureMax maximum temperature
	 */
	void setTemperatureMax(double temperature) {
		temperatureMax = temperature;
	}

	void updateTemperatureChart(double[] time, double[] temperature) {
		temperatureChart.updateXYSeries(chartName, time, temperature, null);
	}
	
	/**
	 * sets the forecast for the day
	 * @param date             date of the forecast
	 * @param summary          summary text
	 * @param temperatureLow   day low temperature
	 * @param temperatureHigh  day high temperature
	 * @param icon             forecast icon
	 */
	void setForecast(Date date,String summary,double temperatureLow,double temperatureHigh,ImageIcon icon) {
		log.fine(String.format("set forecast received for date %s: %s %.0f...%.0f",date,summary,temperatureLow,temperatureHigh));

		EventQueue.invokeLater( new Runnable() {
	        public void run() {
	        	labelForecastSummary.setText("Vorhersage: "+summary);
				String text = String.format("Temperatur %.1f bis %.1f C",temperatureLow,temperatureHigh);
				log.fine("setting temperature range text: "+text);
	        	labelForecastTemperatureRange.setText(text);
	        	if(icon!=null) {
	        		labelForecastIcon.setIcon(icon);
	        	}
	        }
	    } );
	}
	

	//
	// private members
	// 
	private static final long   serialVersionUID = 8937994138265702017L;
	private static final Logger log              = Logger.getLogger( WeatherPanel.class.getName() );
	private static final String chartName		 = "Terrasse";

	private Double temperatureMin = null;         // min temperature of day
	private Double temperatureMax = null;         // max temperature of day
	
	private JLabel labelTemperature;              // label to display actual measured temperature
	private JLabel labelForecastSummary;          // label to display forecast summary
	private JLabel labelForecastIcon;             // label to display forecast icon
	private JLabel labelForecastTemperatureRange; // label to display high temperature

	final XYChart temperatureChart;               // chart to display temperature over day
}
