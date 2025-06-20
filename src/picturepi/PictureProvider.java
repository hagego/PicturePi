package picturepi;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;


/*
 * Data Provider for the Picture Panel
 */
public class PictureProvider extends Provider {

	PictureProvider() {
		// fetch new picture every minute
		super(60);
		log.fine("PictureProvider created");
		
		int refreshInterval = Configuration.getConfiguration().getValue("PicturePanel", "refreshInterval", 60);
		log.config("refresh interval="+refreshInterval+" seconds");
		
		setSleepTime(refreshInterval);
	}

	@Override
	protected void fetchData() {
		log.fine("fetchData() called");
		
		if(picturePanel==null) {
			if(panel.getClass()==PicturePanel.class) {
				log.fine("assigning picturePanel object");
				picturePanel = (PicturePanel)panel;
			}
			else {
				log.severe("Panel is not of class PicturePanel. Disabling updates.");
				return;
			}
		}
		
		LocalDate today = LocalDate.now();
		if(lastDate==null || lastDate.isEqual(today)==false) {
			// new day started. Build new list of pictures to be displayed today
			log.fine("new day started: "+today);

			boolean legacyMode = Configuration.getConfiguration().getValue("PicturePanel", "legacyMode", false);
			if(legacyMode) {
				log.fine("legacy mode enabled");

				imageList = createPictureList();
			}
			else {
				log.fine("legacy mode disabled");

				// get number of pictures to display per day
				int picturesPerDay = Configuration.getConfiguration().getValue("PicturePanel", "picturesPerDay", 100);
				log.config("pictures to display per day: "+picturesPerDay);

				// collect all pictures and map them to their date
				createPictureDateList();
				imageList.clear();

				// add all pictures taken on todays day, at any year
				List<File> pictureListOfDay = getPicturesOfDay(LocalDate.now());
				int picturesOfDay = pictureListOfDay.size();
				log.config("found "+picturesOfDay+" pictures taken today");
				imageList.addAll(pictureListOfDay);

				// add all pictures taken in the month, but not on todays day
				List<File> pictureListOfMonth = getPicturesOfMonth(LocalDate.now());
				int picturesOfMonth = pictureListOfMonth.size();
				log.config("found "+picturesOfMonth+" pictures taken this month");
				imageList.addAll(pictureListOfMonth);

				if(picturesOfDay+picturesOfMonth<picturesPerDay) {
					// add pictures from other months until we have enough
					int picturesFromOtherMonths = picturesPerDay - picturesOfDay - picturesOfMonth;
					log.config("adding "+picturesFromOtherMonths+" pictures from other months");

					// get all pictures from the list
					List<File> pictureList = pictureDateList.stream().map(p -> p.file).collect(Collectors.toList());
					Collections.shuffle(pictureList);

					// add pictures from the list
					for(File file:pictureList) {
						if(imageList.size()>=picturesPerDay) {
							break;
						}
						if(pictureListOfDay.contains(file)==false && pictureListOfMonth.contains(file)==false) {
							imageList.add(file);
						}
					}
				}



				pictureDateList.forEach(f -> imageList.add(f.file));
			}
			
	    	
	    	// randomize list and reset iterator to start
	    	imageIterator = imageList.iterator();
	    	Collections.shuffle(imageList);
	    	
	    	lastDate = today;
		}
		
		// load next image in list and scale
		if(imageIterator.hasNext() == false) {
			log.finest("resetting picture iterator");
			imageIterator = imageList.iterator();
		}
		
		File  file = null;
		try {
			file = imageIterator.next();
			log.fine("preparing image "+file);
			
			Image image = ImageIO.read(file);
			Dimension dimension = picturePanel.getSize();
			if(dimension.getWidth()<=1 || dimension.getHeight()<=1) {
				// can happen at the first time when UI is not ready yet
				log.warning("panel dimension too small. skipping");
				return;
			}
			int width  = (int)dimension.getWidth();
			int height = (int)dimension.getHeight();
			log.fine("panel dimension: "+ dimension);
			
			BufferedImage scaledImage = new BufferedImage(width, height,BufferedImage.TYPE_4BYTE_ABGR);
			
			// Make sure the aspect ratio is maintained, so the image is not distorted
	        double thumbRatio = (double) width / (double) height;
	        int imageWidth = image.getWidth(null);
	        int imageHeight = image.getHeight(null);
			if(imageWidth <= 0 || imageHeight <= 0) {
				log.warning("image width or height is zero, cannot scale image : "+file);
				return;
		 	}

	        double aspectRatio = (double) imageWidth / (double) imageHeight;

	        if (thumbRatio < aspectRatio) {
	            height = (int) (width / aspectRatio);
	        } else {
	            width = (int) (height * aspectRatio);
	        }

	        // Draw the scaled image
	        Graphics2D graphics2D = scaledImage.createGraphics();
	        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	        boolean rc = graphics2D.drawImage(image, 0, 0, width, height, null);
	        log.finest("drawImage returned "+rc);
	        graphics2D.dispose();
	        
	        // get metadata (year)
	        String year = "";
	        try {
				Metadata metadata = ImageMetadataReader.readMetadata(file);
				ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

				if(directory!=null) {
					Date date = directory.getDate(ExifIFD0Directory.TAG_DATETIME);
					if(date!=null) {
						year = new SimpleDateFormat("yyyy").format(date);
					}
					log.finest("Image has year set to: "+year);
				}
				else {
					log.warning("reading metadata: directory is null");
				}
			} catch (ImageProcessingException e) {
				log.warning("Unable to access image file to read metadata: "+file);
			}
			
			picturePanel.setPicture(scaledImage, width, year);
		} catch (IOException e) {
			log.severe("Unable to read image file "+file);
			log.severe(e.getMessage());
		} catch( NoSuchElementException e) {
			log.severe("image list is empty");
		}
	}

	/**
	 * creates a list of PictureDate objects of all pictures in the specified directory
	 */
	void createPictureDateList() {
		// empty list
		pictureDateList.clear();

		log.fine("creating pictureDate list");
		
		String rootDirName = Configuration.getConfiguration().getValue("PicturePanel", "rootDir", null);
		
		if(rootDirName==null) {
			log.severe("No picture rootDir specified - no pictures to display");;
			return;
		}
		
		File rootDir = new File(rootDirName);
		log.config("picture root directory: "+rootDir.getAbsolutePath());
	    if(!rootDir.isDirectory()) {
	    	log.severe("root dir is not a directory: "+rootDir.getAbsolutePath());
	    	return;
	    }
	    
		// get array of all jpeg files in the picture diretory
    	String files[] = rootDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				 return (name.endsWith("jpg") || name.endsWith("jpeg"));
			}
    	});
    	
    	if(files==null) {
    		log.severe("Unable to retrieve files from picture directory - null returned");
			return;
    	}
		log.fine("found "+files.length+" files in picture directory "+rootDir);

		// create PictureDate objects for each file
		for(String file:files) {
			PictureDate pictureDate = new PictureDate();
			pictureDate.file = new File(rootDir,file);

			// get picture data from EXIF data
			try {
				Metadata metadata = ImageMetadataReader.readMetadata(pictureDate.file);
				ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

				if(directory!=null) {
					Date date = directory.getDate(ExifIFD0Directory.TAG_DATETIME);
					if(date!=null) {	
						log.finest(file+" has date set to: "+new SimpleDateFormat("yyyy.MM.dd").format(date));

						LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
						pictureDate.date = localDate;
						pictureDateList.add(pictureDate);
					}
					else {
						log.warning(file+": reading metadata: date is null");
					}
				}
				else {
					log.warning(file+": reading metadata: directory is null");
				}
			} catch (ImageProcessingException  e) {
				log.warning("Unable to access image file to read metadata: "+file);
			}
			catch (IOException e) {
				log.warning("Unable to access image file to read metadata: "+file);
			}
		}
	}

	/**
	 * returns a list of PictureDate objects that maps all pictures to their date
	 * @return list of PictureDate objects
	 */
	List<PictureDate> getPictureDateList() {
		return pictureDateList;
	}

	/**
	 * returns a list of pictures taken on the day and month of the specified date, but in any year
	 * @param  date date to get pictures for
	 * @return list of pictures taken on the day and month of the specified date, but in any year
	 */
	List<File> getPicturesOfDay(LocalDate date) {
		return pictureDateList.stream().filter(p -> p.date.getMonth().equals(date.getMonth()) && p.date.getDayOfMonth()==date.getDayOfMonth()).map(p -> p.file).collect(Collectors.toList());
	}

	/**
	 * returns a list of pictures taken in the month of the specified date, in any year, but at a different day
	 * @param  date date to get pictures for
	 * @return list of pictures taken in the month of the specified date, in any year, but at a different day
	 */
	List<File> getPicturesOfMonth(LocalDate date) {
		return pictureDateList.stream().filter(p -> p.date.getMonth().equals(date.getMonth()) && p.date.getDayOfMonth()!=date.getDayOfMonth()).map(p -> p.file).collect(Collectors.toList());
	}
	 
	
	/**
	 * creates the list of pictures for today
	 * @return list with image filenames to display today
	 */
	List<File> createPictureList() {
		log.fine("creating picture list");
		
		String rootDirName = Configuration.getConfiguration().getValue("PicturePanel", "rootDir", null);
		List<File> localList = new LinkedList<File>();
		
		if(rootDirName==null) {
			log.severe("No picture rootDir specified - no pictures to display");;
			return localList;
		}
		
		File rootDir = new File(rootDirName);
		log.config("root directory: "+rootDir.getAbsolutePath());
	    if(!rootDir.isDirectory()) {
	    	log.severe("root dir is not a directory: "+rootDir.getAbsolutePath());
	    	return localList;
	    }
		
		log.fine("building picture list from root dir "+rootDirName);
		File commonDir = new File(rootDirName, "common");
	    if(!commonDir.isDirectory()) {
	    	log.severe("common dir is not a directory: "+commonDir);
	    	return localList;
	    }
	    
	    // add pictures from common directory
    	String files[] = commonDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				 return (name.endsWith("jpg") || name.endsWith("jpeg"));
			}
    	});
    	
    	if(files==null) {
    		log.severe("Unable to retrieve files from common directory - null returned");
    	}
    	else {
	    	log.fine("found "+files.length+" files in common directory "+commonDir);
	    	for(String file:files) {
    			log.finest("Adding picture "+file);
    			localList.add(new File(commonDir,file));
	    	}
    	}
    	
    	// add pictures for the month
    	String month = LocalDate.now().format(DateTimeFormatter.ofPattern("MM"));
    	File monthDir = new File(rootDir,month);
    	log.fine("adding pictures of the month from "+monthDir);
    	
    	if(monthDir.isDirectory()) {
	    	files = monthDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					 return (name.endsWith("jpg") || name.endsWith("jpeg"));
				}
	    	});
	    	
	    	if(files==null) {
	    		log.severe("Unable to retrieve files from common directory - null returned");
	    	}
	    	else {
		    	log.fine("found "+files.length+" files in month directory "+monthDir);
		    	for(String file:files) {
	    			log.finest("Adding picture "+file);
	    			localList.add(new File(monthDir,file));
		    	}
	    	}
	    }
    	
    	// add pictures for the day - if any
    	String day = LocalDate.now().format(DateTimeFormatter.ofPattern("dd"));
    	File dayDir = new File(monthDir,day);
    	log.fine("adding pictures of the day from "+dayDir);
    	
    	if(dayDir.isDirectory()) {
	    	files = dayDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					 return (name.endsWith("jpg") || name.endsWith("jpeg"));
				}
	    	});
	    	
	    	if(files==null) {
	    		log.severe("Unable to retrieve files from common directory - null returned");
	    	}
	    	else {
		    	log.fine("found "+files.length+" files in day directory "+dayDir);
		    	for(String file:files) {
	    			log.finest("Adding picture "+file);
	    			localList.add(new File(dayDir,file));
		    	}
	    	}
	    }
    	
    	// add National Geographic Picture Of The Day
    	log.fine("adding National Geographic picture of the day");;
    	String pod = downloadNationalGeographicPictureOfTheDay();
    	if(pod!=null) {
    		imageList.add(new File(pod));
    	}
    	
    	log.fine("creating picture list done");
    	return localList;
	}
	
	/**
	 * gets the URL for the National Geographic Picture Of The Day image (jpg) file
	 * @return
	 */
	String getNationalGeographicPictureOfTheDayUrl() {
		
		log.finest("getting URL for National Geographic Picture Of The Day");
		try {
			Connection connection = Jsoup.connect("https://www.nationalgeographic.com/photo-of-the-day/");
			log.finest("conncetion created");
			Document document = connection.get();
			log.finest("found document");
			
			Elements all = document.getAllElements();
			log.finest("found elements");
			
			Optional<Element> elementUrl = all.stream().filter(e -> e.tag().getName().equals("meta")).filter(e -> e.attr("property").equals("og:image")).findFirst();
			if(elementUrl.isPresent()) {
				String url = elementUrl.get().attr("content");
				log.finest("retrieved URL for Picture Of The Day: "+url);
				return url;
			}
			else {
				log.warning("Unable to retrieve URL for picture of the day");
			}
			
		} catch (IOException e) {
			log.severe("IO exception whil trying to get Picture Of The Day URL: "+e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * downloads the National Geographic picture of the day
	 * @return filename or null in case of error
	 */
	String downloadNationalGeographicPictureOfTheDay() {
		String url = getNationalGeographicPictureOfTheDayUrl();
		
		try {
			HttpTools.downloadFile(url, System.getProperty("java.io.tmpdir"),"NationalGeographicPod.jpg");
			
			return "/tmp/NationalGeographicPod.jpg";
		} catch (IOException e) {
			log.severe("Unable to get National Geographic POD: "+e.getMessage());
		}
		
		return null;
	}
	
	//
	// private data
	//
	private static final Logger log = Logger.getLogger( PictureProvider.class.getName() );

	private PicturePanel     picturePanel;                          // corresponding picture panel
	private LocalDate        lastDate  = null;                      // date when last picture list was built
	private List<File>       imageList = new LinkedList<File>();    // list with filenames of images to display
	private Iterator<File>   imageIterator;                         // iterator over image list

	// local class to associate a picture with a date
	private class PictureDate {
		File      file;
		LocalDate date;
	}

	private List<PictureDate> pictureDateList = new LinkedList<PictureDate>();

}
