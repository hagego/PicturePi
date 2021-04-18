package picturepi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * helper class to deal with HTTP requests
 * @author hagen
 *
 */
public class HttpTools {

	
	private static final Logger   log     = Logger.getLogger( HttpTools.class.getName() );
	

    private static final int BUFFER_SIZE = 4096;
    
    /**
     * Downloads a file from a URL
     * @param fileURL  HTTP URL of the file to be downloaded
     * @param saveDir  path of the directory to save the file
     * @param saveFile optional, if not null, this name will be used for the file, otherwise the filename retrieved
     *                 from the HTTP server
     * @throws IOException
     */
    public static void downloadFile(String fileURL, String saveDir, String saveFile)
            throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();
 
        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();
 
            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }
            
            log.fine("downloading file from HTTP server: "+fileURL);
            log.fine("Content-Type = " + contentType+" Content-Disposition = " + disposition);
            log.fine("Content-Length = " + contentLength+" fileName = " + fileName);
 
            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath;
            if(saveFile==null) {
            	saveFilePath = saveDir + File.separator + fileName;
            }
            else {
            	saveFilePath = saveDir + File.separator + saveFile; 
            }
             
            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
 
            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
 
            outputStream.flush();
            outputStream.close();
            inputStream.close();
 
            log.fine("File downloaded to "+saveFilePath);
        } else {
            log.severe("No file to download. Server replied HTTP code: " + responseCode);
            throw new IOException("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }
}
