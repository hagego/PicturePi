package picturepi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;

/**
 * Panel displaying pictures
 */
class PicturePanel extends Panel {

	/**
	 * Constructor
	 */
	public PicturePanel() {
		super(new PictureProvider());
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		setBackground(Color.BLACK);
		Font fontBig   = new Font(Font.SANS_SERIF, Font.PLAIN, 60);
		lblYear.setFont(fontBig);
		
		add(lblYear);
	}
	
	@Override
	boolean hasData() {
		return true;
	}
	
	@Override
	void setColorDark() {
	}
	
	@Override
	void setColorBright() {
	}
	
	/**
	 * sets a new picture
	 * @param image picture to set
	 * @param width width of image
	 * @param year  year picture was taken
	 */
	synchronized void setPicture(Image image,int width,String year) {
		log.fine("new image arrived");

		this.image = image;
		imageWidth = width;
		
		if(getWidth()>imageWidth+10) {
			// empty space left/right of image (portrait image)
			lblYear.setForeground(Color.BLACK);
		}
		else {
			// landscape image
			lblYear.setForeground(Color.WHITE);
		}

		if(year!=null) {
			lblYear.setText("  "+year+"  ");
		}
		else {
			lblYear.setText("");
		}
		
		repaint();
	}
	
	@Override
	synchronized protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int posX = 0;
        if(getWidth()>imageWidth) {
        	posX = (getWidth()-imageWidth)/2;
        }
        g.drawImage(image, posX, 0, null);
    }

	//
	// private members
	//
	private static final long   serialVersionUID = -8276167364875951889L;
	private static final Logger log = Logger.getLogger( PicturePanel.class.getName() );
	
	private Image     image;                            // picture to show
	private int       imageWidth;                       // width of image;
	private JLabel    lblYear = new JLabel();
}
