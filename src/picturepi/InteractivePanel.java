package picturepi;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * panel to be activated by touch event and then be used interactively by the user 
 */
public class InteractivePanel extends Panel{

    /**
     * constructor
     */
    InteractivePanel() {
        super(new InteractiveDataProvider());

        log.fine("creating InteractivePanel");

        // get the MQTT topic to publish commands
        mqttTopicCommand = Configuration.getConfiguration().getValue(InteractivePanel.class.getSimpleName(), CONFIG_KEY_MQTT_TOPIC_COMMAND, null);
        
        // create UI elements
        initUi();
    }

    void initUi() {
        log.fine("initUi called");

        setBackground(Color.BLACK);
        setLayout(new GridBagLayout());
		
		Font font   = new Font(Font.SANS_SERIF, Font.BOLD, 24);
		Color color = Color.WHITE;
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(10, 10, 10, 10);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridy = 0;
		
		// measured temperatures
        constraints.gridx = 0;
		JLabel labelHeaderTemperature1 = new JLabel("Temperatur Terrasse");
		labelHeaderTemperature1.setFont(font);
		labelHeaderTemperature1.setForeground(color);
		add(labelHeaderTemperature1,constraints);
		
        constraints.gridx = 1;
        labelTemperature1 = new JLabel("--");
		labelTemperature1.setFont(font);
		labelTemperature1.setForeground(color);
		add(labelTemperature1,constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        JLabel labelHeaderTemperature2 = new JLabel("Temperatur Hasenstall");
        labelHeaderTemperature2.setFont(font);
        labelHeaderTemperature2.setForeground(color);
        add(labelHeaderTemperature2,constraints);

        constraints.gridx = 1;
        labelTemperature2 = new JLabel("--");
        labelTemperature2.setFont(font);
        labelTemperature2.setForeground(color);
        add(labelTemperature2,constraints);
                    
        // buttons for blinds
        constraints.gridy++;
        constraints.gridx = 0;
        JLabel labelBlindsHeader = new JLabel("Rollladen");
		labelBlindsHeader.setFont(font);
		labelBlindsHeader.setForeground(color);
		add(labelBlindsHeader,constraints);
        final int ICON_SIZE = 140;

        ImageIcon icon = null;
        try {
            java.net.URL imageURL = this.getClass().getResource("otherIcons/blinds.png");
            icon = new ImageIcon(imageURL);
            icon.setImage(icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, java.awt.Image.SCALE_SMOOTH));

            constraints.gridx = 1;
            JButton buttonBlindsLeft = new JButton();
            buttonBlindsLeft.setBackground(Color.BLACK);
            buttonBlindsLeft.setIcon(icon);
            buttonBlindsLeft.addMouseListener(new java.awt.event.MouseListener() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    log.finest("mouse clicked left blinds, x="+e.getX()+", y="+e.getY());
                    if(e.getX() > ICON_SIZE/2) {
                        log.fine("touch event left blinds down");
                        if(mqttTopicCommand != null) {
                            MqttClient.getMqttClient().publish(mqttTopicCommand, "left down");
                        }
                    }
                    else {
                        log.fine("touch event left blinds up");
                        if(mqttTopicCommand != null) {
                            MqttClient.getMqttClient().publish(mqttTopicCommand, "left up");
                        }
                    }
                }
                public void mousePressed(java.awt.event.MouseEvent e) {}
                public void mouseReleased(java.awt.event.MouseEvent e) {}
                public void mouseExited(java.awt.event.MouseEvent e) {}
                public void mouseEntered(java.awt.event.MouseEvent e) {}
            });
            add(buttonBlindsLeft,constraints);

            constraints.gridx = 2;
            JButton buttonBlindsRight = new JButton();
            buttonBlindsRight.setBackground(Color.BLACK);
            buttonBlindsRight.setIcon(icon);
            buttonBlindsRight.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    log.fine("buttonRIght pressed");
                }
            });
            buttonBlindsRight.addMouseListener(new java.awt.event.MouseListener() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    log.finest("mouse clicked right blinds, x="+e.getX()+", y="+e.getY());
                    if(e.getX() > ICON_SIZE/2) {
                        log.fine("touch event right blinds down");
                        if(mqttTopicCommand != null) {
                            MqttClient.getMqttClient().publish(mqttTopicCommand, "right down");
                        }
                    }
                    else {
                        log.fine("touch event right blinds up");
                        if(mqttTopicCommand != null) {
                            MqttClient.getMqttClient().publish(mqttTopicCommand, "right up");
                        }
                    }
                }
                public void mousePressed(java.awt.event.MouseEvent e) {}
                public void mouseReleased(java.awt.event.MouseEvent e) {}
                public void mouseExited(java.awt.event.MouseEvent e) {}
                public void mouseEntered(java.awt.event.MouseEvent e) {}
            });
            add(buttonBlindsRight,constraints);
        }
        catch(Exception e) {
            log.severe("Unable to load trashbin icon: ");
        }
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
     * sets temperature 1 to be displayed
     * @param temperature temperature to be displayed in degree Celsius
     */
    void setTemperature1(double temperature) {
        log.fine("setTemperature1 called with: "+temperature);
        labelTemperature1.setText(String.format("%.1f C", temperature));
    }

    /**
     * sets temperature 2 to be displayed
     * @param temperature temperature to be displayed in degree Celsius
     */
    void setTemperature2(double temperature) {
        log.fine("setTemperature2 called with: "+temperature);
        labelTemperature2.setText(String.format("%.1f C", temperature));
    }

    //
    // private members
    // 
    private static final Logger log = Logger.getLogger( WeatherPanel.class.getName() );

    // UI elements
    private JLabel labelTemperature1 = null;  // temperature 1
    private JLabel labelTemperature2 = null;  // temperature 2

    // MQTT topic keys in config file
    private static final String CONFIG_KEY_MQTT_TOPIC_COMMAND   = "mqttTopicCommand";         // configuration key for the MQTT topic to publish commands

    private String mqttTopicCommand                             = null;                       // MQTT topic to publish commands
}
