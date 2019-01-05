package picturepi;

import java.awt.EventQueue;
import java.util.logging.Logger;
import javax.swing.JFrame;


/*
 * PicturePi Main Window class
 */
public class MainWindow implements Runnable {

	public MainWindow() {
		configuration = Configuration.getConfiguration();
	}
	
	/*
	 * reads configuration data from .ini file
	 */
	private void readConfig() {
		// read screen setup data from config file
		log.config("reading screen configuration");
			
		isFullscreen = configuration.getValue("screen","fullscreen",true);
		width        = configuration.getValue("screen","width",width);
		height       = configuration.getValue("screen","height",height);
			
		log.config("screen configuration: fullscreen="+isFullscreen+" width="+width+" height="+height);
	}
	
	@Override
	public void run() {
		// read data from configuration file
		readConfig();
		
		// create main frame maximized and undecorated
		mainFrame = new JFrame();
		if(isFullscreen) {
			mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			mainFrame.setUndecorated(true);
		}
		else {
			mainFrame.setSize(width,height);
		}
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mainFrame.setVisible(true);
		mainFrame.repaint();
		
		if(!isFullscreen) {
			// re-adjust size, compensating for window borders
			int w=mainFrame.getContentPane().getWidth();
			int h=mainFrame.getContentPane().getHeight();
			
			mainFrame.setSize(2*width-w,2*height-h);
		}
		
		int w=mainFrame.getWidth();
		int h=mainFrame.getHeight();
		log.config("display size: width="+w+" height="+h);
	}
	
	/**
	 * sets the active panel to display
	 * @param panel panel to display
	 */
	void setPanel(Panel panel) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				log.fine("setting panel "+panel.getClass().toString());
				mainFrame.getContentPane().removeAll();
				mainFrame.getContentPane().add(panel);
				mainFrame.setVisible(true);
				mainFrame.repaint();
			}
		});
	}

	// private members
	private static final Logger   log      = Logger.getLogger( MainWindow.class.getName() );
	
	final private        Configuration      configuration;
	private              boolean  isFullscreen = false;
	private              int      width        = 1280;
	private              int      height       = 800;
	
	private              JFrame   mainFrame;
}
