package picturepi;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.ini4j.Ini;

/*
 * Display panel for traffic information
 */
public class TrafficPanel extends Panel {

	/*
	 * Constructor
	 */
	public TrafficPanel(Ini ini) {
		super(new TrafficProvider(ini));
	}
	
	@Override
	boolean hasData() {
		return true;
	}
	
	/*
	 * builds up the screen
	 * @param routeList list of routes and driving alternatives to display
	 */
	private void setupScreen(List<TrafficProvider.Route> routeList) {
		setBorder(new LineBorder(Color.GREEN, 20, true));
		setBackground(new Color(135, 206, 250));
		
		setLayout(new GridBagLayout());
		
		
		
		// define fonts & colors
		Color colorTitle = Color.MAGENTA;
		Font  fontTitle  = new Font("Times New Roman", Font.BOLD, 40);
		
		Color colorHeader = Color.MAGENTA;
		Font  fontHeader  = new Font("Times New Roman", Font.BOLD, 28);
		
		Color colorItem = Color.CYAN;
		Font  fontItem  = new Font("Times New Roman", Font.BOLD, 24);
		
		int gridy = 0;

		// Title
		JLabel title = new JLabel("Verkehrslage von 00:00");
		title.setFont(fontTitle);
		title.setForeground(colorTitle);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setHorizontalTextPosition(SwingConstants.CENTER);

		GridBagConstraints gbcTitle = new GridBagConstraints();
		gbcTitle.anchor = GridBagConstraints.CENTER;
		gbcTitle.gridx = 0;
		gbcTitle.gridwidth = GridBagConstraints.REMAINDER;
		gbcTitle.gridy = gridy;
		gbcTitle.weightx = 1.0;
		gbcTitle.weighty = 2.0;
		add(title,gbcTitle);
		gridy++;
		
		JLabel lblCameras = new JLabel("Verkehrskameras:");
		lblCameras.setFont(fontHeader);
		lblCameras.setForeground(colorHeader);
		lblCameras.setHorizontalAlignment(SwingConstants.LEADING);
		lblCameras.setHorizontalTextPosition(SwingConstants.LEADING);

		GridBagConstraints gbcLblCamera = new GridBagConstraints();
		gbcLblCamera.anchor    = GridBagConstraints.WEST;
		gbcLblCamera.gridx     = 0;
		gbcLblCamera.gridwidth = GridBagConstraints.REMAINDER;
		gbcLblCamera.gridy     = gridy;
		gbcLblCamera.weightx   = 1.0;
		gbcLblCamera.weighty   = 1.0;
		gbcLblCamera.insets    = new Insets(0, 20, 0, 0);
		add(lblCameras,gbcLblCamera);
		gridy++;
		
		int cameras = 3;
		
		for(int camera=0 ; camera<cameras ; camera++) {
			JLabel lblName = new JLabel("description");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = camera;
			gbc.gridy = gridy;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			add(lblName,gbc);
			
			JLabel lblPicture = new JLabel("picture");
			gbc.gridy = gridy+1;
			add(lblPicture,gbc);
			
		}
		gridy += 2;
		
		// routes
		mapRouteAlternativeLabels.clear();
		for(TrafficProvider.Route route: routeList) {
			JLabel lblRoute = new JLabel("Strecke: "+route.description);
			lblRoute.setFont(fontHeader);
			lblRoute.setForeground(colorHeader);
			lblRoute.setHorizontalAlignment(SwingConstants.LEADING);
			lblRoute.setHorizontalTextPosition(SwingConstants.LEADING);

			GridBagConstraints gbcRoute = new GridBagConstraints();
			gbcRoute.anchor    = GridBagConstraints.WEST;
			gbcRoute.gridx     = 0;
			gbcRoute.gridwidth = GridBagConstraints.REMAINDER;
			gbcRoute.gridy     = gridy;
			gbcRoute.weightx   = 1.0;
			gbcRoute.weighty   = 1.0;
			gbcRoute.insets    = new Insets(0, 20, 0, 0);
			add(lblRoute,gbcRoute);
			
			gridy++;
			
			for(TrafficProvider.Route.Alternative alternative:route.alternativeList) {
				JLabel lblAlternative = new JLabel(alternative.summary+" : "+alternative.duration);
				lblAlternative.setFont(fontItem);
				lblAlternative.setForeground(colorItem);
				lblAlternative.setHorizontalAlignment(SwingConstants.LEADING);
				lblAlternative.setHorizontalTextPosition(SwingConstants.LEADING);
				
				
				GridBagConstraints gbcAlternative = new GridBagConstraints();
				gbcAlternative.anchor    = GridBagConstraints.WEST;
				gbcAlternative.gridx     = 0;
				gbcAlternative.gridwidth = GridBagConstraints.REMAINDER;
				gbcAlternative.gridy     = gridy;
				gbcAlternative.weightx   = 1.0;
				gbcAlternative.weighty   = 0.5;
				gbcAlternative.insets    = new Insets(0, 50, 0, 0);
				add(lblAlternative,gbcAlternative);
				
				mapRouteAlternativeLabels.put(route.description+":"+alternative.summary, lblAlternative);
				gridy++;
			}
		}
		
		repaint();
		
		
		
	}
	
	@Override
	void setColorDark() {
	}
	
	@Override
	void setColorBright() {
	}
	
	/*
	 * refresh display with updated data
	 */
	void refresh(List<TrafficProvider.Route> routeList) {
		log.fine("refreshing");
		
		setupScreen(routeList);
	}

	// private members
	private static final long serialVersionUID = -3498436981851393716L;
	
	private static final Logger log             = Logger.getLogger( TrafficPanel.class.getName() );
	
	private Map<String,JLabel>  mapRouteAlternativeLabels = new HashMap<String,JLabel>(); // maps route alternatives to the Jlabel objects with the duration
}
