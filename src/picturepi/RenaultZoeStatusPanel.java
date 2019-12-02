package picturepi;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

import javax.swing.JLabel;

/**
 * Panel to display the current status of a Renault Zoe electrical car
 *
 */
public class RenaultZoeStatusPanel extends Panel {

	public RenaultZoeStatusPanel() {
		super(new RenaultZoeStatusProvider());
		
		setBackground(Color.BLACK);
		setLayout(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridwidth  = 2;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(10, 50, 50, 10);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 0.1;
		
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, 70);
		
		JLabel header = new JLabel("Status Zoe:");
		header.setFont(font);
		header.setForeground(Color.MAGENTA);
		add(header,constraints);
		
		constraints.insets = new Insets(10, 10, 10, 10);
		constraints.gridwidth = 1;
		
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.insets = new Insets(10, 50, 10, 10);
		JLabel labelChargingHeader = new JLabel("Lädt gerade:");
		labelChargingHeader.setFont(font);
		labelChargingHeader.setForeground(Color.MAGENTA);
		add(labelChargingHeader,constraints);
		
		constraints.gridx = 1;
		labelCharging = new JLabel("--");
		labelCharging.setFont(font);
		labelCharging.setForeground(Color.MAGENTA);
		add(labelCharging,constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 2;
		JLabel labelChargeLevelHeader = new JLabel("Ladezustand:");
		labelChargeLevelHeader.setFont(font);
		labelChargeLevelHeader.setForeground(Color.MAGENTA);
		add(labelChargeLevelHeader,constraints);
		
		constraints.gridx = 1;
		labelChargeLevel = new JLabel("--");
		labelChargeLevel.setFont(font);
		labelChargeLevel.setForeground(Color.MAGENTA);
		add(labelChargeLevel,constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 3;
		JLabel labelConnectedHeader = new JLabel("Eingesteckt:");
		labelConnectedHeader.setFont(font);
		labelConnectedHeader.setForeground(Color.MAGENTA);
		add(labelConnectedHeader,constraints);
		
		constraints.gridx = 1;
		labelConnected = new JLabel("--");
		labelConnected.setFont(font);
		labelConnected.setForeground(Color.MAGENTA);
		add(labelConnected,constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 4;
		JLabel labelAirConditioningHeader = new JLabel("Klimaanlage:");
		labelAirConditioningHeader.setFont(font);
		labelAirConditioningHeader.setForeground(Color.MAGENTA);
		add(labelAirConditioningHeader,constraints);
		
		constraints.gridx = 1;
		labelAirConditioning = new JLabel("--");
		labelAirConditioning.setFont(font);
		labelAirConditioning.setForeground(Color.MAGENTA);
		add(labelAirConditioning,constraints);
		
		log.fine("RenaultZoeStatusPanel created");
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
	
	/**
	 * Sets the current status
	 * @param isCharging
	 * @param chargeLevel
	 * @param isPlugged
	 * @param airConditioningEnabled
	 */
	void setStatus(boolean isCharging,int chargeLevel,boolean isPlugged,boolean airConditioningEnabled) {
		labelCharging.setText(isCharging ? "Ja" : "Nein");
		labelChargeLevel.setText(Integer.toString(chargeLevel));
		labelConnected.setText(isPlugged ? "Ja" : "Nein");
		labelAirConditioning.setText(airConditioningEnabled ? "Ja" : "Nein");
	}
	


	//
	// private members
	//
	private static final Logger   log              = Logger.getLogger( MethodHandles.lookup().lookupClass().getName() );
	private static final long     serialVersionUID = -2033700528834112613L;
	
	private              JLabel labelCharging;         // JLabel objects for charging status
	private              JLabel labelChargeLevel;      // JLabel objects for charge level
	private              JLabel labelConnected;        // JLabel objects for connection state
	private              JLabel labelAirConditioning;  // JLabel objects for air conditioning
}
