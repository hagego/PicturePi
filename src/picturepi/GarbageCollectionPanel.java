package picturepi;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Color;
import java.util.EnumSet;
import java.util.logging.Logger;

class GarbageCollectionPanel extends Panel {


	enum TrashBinColors {BLACK,BLUE,BROWN,YELLOW};
	
	/**
	 * Constructor
	 */
	public GarbageCollectionPanel() {
		super(new GarbageCollectionProvider());
		
		setBackground(Color.BLACK);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		// create empty trashBinColor
		trashBinColors = EnumSet.noneOf(TrashBinColors.class);
		EnumSet<TrashBinColors> newTrashBinColors = EnumSet.noneOf(TrashBinColors.class);
		setTrashBinColors(newTrashBinColors);
		
		setColorBright();
	}
	
	@Override
	boolean hasData() {
		return trashBinColors.size() > 0;
	}
	
	@Override
	void setColorDark() {
		iconPostfix = "_dark";
	}
	
	@Override
	void setColorBright() {
		iconPostfix = "_bright";
	}
	
	void setTrashBinColors(EnumSet<TrashBinColors> newTrashBinColors) {
		if(trashBinColors.equals(newTrashBinColors) == false) {
			log.fine("Trash bins to display have changed");
			
			// remove old components
			removeAll();
			
			// add new icons (if any)
			trashBinColors.clear();
			iconLabels = new JLabel[newTrashBinColors.size()];
			
			add(Box.createHorizontalGlue());
			
			int i = 0;
			for(TrashBinColors trashBinColor:newTrashBinColors) {
				// get corresponding icon
				String iconName = trashBinColor.toString().toLowerCase();
			    ImageIcon icon = null;
			    try {
				    java.net.URL imageURL = this.getClass().getResource("trashBinIcons/"+iconName+iconPostfix+".png");
				    icon = new ImageIcon(imageURL);
				    
					trashBinColors.add(trashBinColor);
					iconLabels[i] = new JLabel();
					iconLabels[i].setIcon(icon);
					add(iconLabels[i]);
					add(Box.createHorizontalGlue());
			    }
			    catch(Exception e) {
			    	log.severe("Unable to load trashbin icon: "+iconName);
			    }
			}
		}
	}
	
	
	//
	// private members
	//
	private static final long     serialVersionUID = 3917104997147741856L;
	private static final Logger   log              = Logger.getLogger( GarbageCollectionPanel.class.getName() );

	private EnumSet<TrashBinColors> trashBinColors;           // currently displayed trashBinColors
	private String                  iconPostfix = "_bright";  // postfix added to icon filenames (_dark/_bright)
	private JLabel[]                iconLabels;               // currently displayed icons
}
